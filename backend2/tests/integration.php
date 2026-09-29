<?php
/** Full HTTP + MySQL tests. Uses only an explicitly supplied disposable database ending in _test. */
declare(strict_types=1);

mysqli_report(MYSQLI_REPORT_ERROR | MYSQLI_REPORT_STRICT);
$database = getenv('TEST_DB_NAME') ?: '';
if (!preg_match('/^[a-z0-9_]+_test$/D', $database) || !getenv('TEST_DB_PASSWORD')) {
    fwrite(STDERR, "Set TEST_DB_NAME (ending in _test), TEST_DB_PASSWORD, and optionally TEST_DB_PORT/TEST_DB_USER. Never use a live database.\n"); exit(2);
}
$root = dirname(__DIR__);
$db = new mysqli('127.0.0.1', getenv('TEST_DB_USER') ?: 'root', getenv('TEST_DB_PASSWORD'), $database, (int)(getenv('TEST_DB_PORT') ?: 13306));
$db->set_charset('utf8mb4');
$schema = getenv('TEST_LEGACY_SCHEMA') ? '/tests/fixtures/legacy-schema.sql' : '/database/schema.sql';
$db->multi_query(file_get_contents($root . $schema));
do { if ($result = $db->store_result()) $result->free(); } while ($db->more_results() && $db->next_result());
$port = (int)(getenv('TEST_HTTP_PORT') ?: 18081);
$env = getenv();
$env = array_merge($env, ['CI_ENVIRONMENT'=>'development', 'COOKIE_SECURE'=>'false', 'MAIL_ENABLED'=>'true', 'APP_ORIGIN'=>'http://localhost:5173',
    'app.baseURL'=>"http://127.0.0.1:$port/", 'database.default.hostname'=>'127.0.0.1', 'database.default.port'=>(string)(getenv('TEST_DB_PORT') ?: 13306),
    'database.default.username'=>getenv('TEST_DB_USER') ?: 'root', 'database.default.password'=>getenv('TEST_DB_PASSWORD'),
    'database.default.database'=>$database, 'database.default.DBDriver'=>'MySQLi', 'PHP_CLI_SERVER_WORKERS'=>'4']);
$log = tempnam(sys_get_temp_dir(), 'webonix-api-test-');
$outbox = $log . '-mail'; mkdir($outbox, 0700);
$smtpPort = (int)(getenv('TEST_SMTP_PORT') ?: 18082);
$env = array_merge($env, ['SMTP_HOST'=>'127.0.0.1', 'SMTP_PORT'=>(string)$smtpPort, 'SMTP_USERNAME'=>'', 'SMTP_PASSWORD'=>'',
    'SMTP_ENCRYPTION'=>'none', 'MAIL_FROM'=>'sender@example.test', 'MAIL_FROM_NAME'=>'Webonix Test']);
$smtp = proc_open(['python3', $root . '/tests/smtp_fixture.py', (string)$smtpPort, $outbox],
    [0=>['pipe','r'],1=>['file',$log,'a'],2=>['file',$log,'a']], $smtpPipes, $root);
fclose($smtpPipes[0]);
$process = proc_open([PHP_BINARY, '-S', "127.0.0.1:$port", '-t', $root . '/public', $root . '/tests/router.php'],
    [0=>['pipe','r'], 1=>['file',$log,'a'], 2=>['file',$log,'a']], $pipes, $root, $env);
if (!is_resource($process)) throw new RuntimeException('Cannot start test HTTP server');
fclose($pipes[0]);
$sessionCookie = 'webonix_session'; $csrfCookie = 'webonix_csrf';
$checks = 0; $cases = 0;
function check(bool $condition, string $message): void { global $checks; $checks++; if (!$condition) throw new RuntimeException($message); }
function query(string $sql, array $params = []): mysqli_result|bool {
    global $db; $s = $db->prepare($sql); if ($params) $s->bind_param(implode('', array_map(fn($value)=>is_int($value)?'i':'s', $params)), ...$params); $s->execute(); return $s->get_result();
}
function scalar(string $sql, array $params = []): mixed { return query($sql, $params)->fetch_row()[0]; }
function handle(string $method, string $path, array $cookies = [], mixed $body = null, array $headers = []): CurlHandle {
    global $port;
    $c = curl_init("http://127.0.0.1:$port$path");
    $base = ['Content-Type: application/json', 'Origin: http://localhost:5173'];
    foreach ($headers as $name=>$value) { $base = array_values(array_filter($base, fn($h)=>!str_starts_with(strtolower($h),strtolower($name).':'))); $base[]="$name: $value"; }
    curl_setopt_array($c, [CURLOPT_RETURNTRANSFER=>true, CURLOPT_HEADER=>true, CURLOPT_CUSTOMREQUEST=>$method, CURLOPT_TIMEOUT=>15, CURLOPT_HTTPHEADER=>$base,
        CURLOPT_COOKIE=>implode('; ',array_map(fn($key)=>$key.'='.$cookies[$key],array_keys($cookies)))]);
    if ($body !== null) curl_setopt($c, CURLOPT_POSTFIELDS, is_string($body) ? $body : json_encode($body));
    return $c;
}
function decode(CurlHandle $c, string $raw): array {
    $size=curl_getinfo($c,CURLINFO_HEADER_SIZE); $headers=substr($raw,0,$size); $body=substr($raw,$size); $cookies=[];
    preg_match_all('/^Set-Cookie:\s*([^=]+)=([^;\r\n]*)/mi',$headers,$matches,PREG_SET_ORDER);
    foreach($matches as $match) $cookies[$match[1]]=urldecode($match[2]);
    return ['status'=>curl_getinfo($c,CURLINFO_RESPONSE_CODE),'headers'=>$headers,'body'=>json_decode($body,true),'cookies'=>$cookies];
}
function request(string $method,string $path,array $cookies=[],mixed $body=null,array $headers=[]): array {
    $c=handle($method,$path,$cookies,$body,$headers); $raw=curl_exec($c);
    if($raw===false) throw new RuntimeException('HTTP transport failure'); return decode($c,$raw) + ['route'=>$method.' '.$path];
}
function mutate(string $method,string $path,array $cookies=[],mixed $body=null,array $headers=[]): array {
    $csrf=request('GET','/api/auth/csrf',$cookies); check($csrf['status']===200,'CSRF endpoint unavailable: HTTP '.$csrf['status']);
    return request($method,$path,array_merge($cookies,$csrf['cookies']),$body,array_merge(['X-XSRF-TOKEN'=>$csrf['body']['token']],$headers));
}
function login(string $email='user@example.test',string $password='old-test-password',bool $remember=false): array {
    return mutate('POST','/api/auth/login',[],compact('email','password')+['rememberMe'=>$remember]);
}
function session(string $email='user@example.test'): array { $r=login($email); check($r['status']===200,'Fixture login failed'); return $r['cookies']; }
function resetLink(int $user=2, int $seconds=900, string $email='user@example.test'): string {
    $token=bin2hex(random_bytes(32)); query('DELETE FROM password_resets WHERE user_id=?',[$user]);
    query('INSERT INTO password_resets(user_id,token_hash,email,expires_at) VALUES(?,?,?,?)',[$user,hash('sha256',$token),$email,gmdate('Y-m-d H:i:s',time()+$seconds)]); return $token;
}
function seed(): void {
    global $db;
    foreach(['auth_sessions','password_resets','nfc_cards','app_users','auth_rate_limits'] as $table) $db->query("DELETE FROM $table");
    $hash=password_hash('old-test-password',PASSWORD_BCRYPT,['cost'=>10]);
    foreach([[1,'Admin','admin@example.test','ADMIN',1],[2,'User','user@example.test','USER',1],[3,'Other','other@example.test','USER',1],[4,'Disabled','disabled@example.test','USER',0]] as [$id,$name,$email,$role,$active])
        query('INSERT INTO app_users(id,name,email,password,role,active) VALUES(?,?,?,?,?,?)',[$id,$name,$email,$hash,$role,$active]);
    foreach([[1,2,5],[2,3,50],[3,null,0]] as [$id,$owner,$scans]) query('INSERT INTO nfc_cards(id,name,type,code,destination_url,owner_id,scans,active,version) VALUES(?,?,?,?,?,?,?,1,0)',[$id,'Card '.$id,'website','CODE'.$id,'https://example.test',$owner,$scans]);
}
function scenario(string $name, callable $test): void { global $cases; seed(); $test(); $cases++; echo "PASS $name\n"; }
function status(array $r,int $expected): void { check($r['status']===$expected,"Expected HTTP $expected, got ".$r['status']." on ".($r['route']??'request')); }
$card=['name'=>'Updated','type'=>'website','destinationUrl'=>'https://example.test/new'];
$password=['currentPassword'=>'old-test-password','newPassword'=>'new123','confirmPassword'=>'new123'];
try {
    for($i=0;$i<50;$i++) { try { $r=request('GET','/api/auth/csrf'); if($r['status']===200) break; } catch(Throwable) {} usleep(100000); }
    scenario('cookie session lifetimes and server-side hashes',function(){
        $r=login();status($r,200);check(!preg_match('/^Set-Cookie: webonix_session=.*(?:Max-Age|Expires)=/mi',$r['headers']),'Normal login must use a session cookie');
        check(str_contains(strtolower($r['headers']),'httponly')&&str_contains(strtolower($r['headers']),'samesite=strict'),'Cookie flags');
        check((int)scalar('SELECT TIMESTAMPDIFF(SECOND,UTC_TIMESTAMP(),expires_at) FROM auth_sessions LIMIT 1')<=3600,'Normal session duration');
        check(!isset($r['body']['password'])&&!isset($r['body']['token']),'No credentials in response');
        $r=login('user@example.test','old-test-password',true);status($r,200);preg_match('/^Set-Cookie: webonix_session=.*Max-Age=(\d+)/mi',$r['headers'],$age);check(isset($age[1])&&(int)$age[1]>=604798&&(int)$age[1]<=604800,'Remember duration');
        check(scalar('SELECT id FROM auth_sessions ORDER BY expires_at DESC LIMIT 1')===hash('sha256',$r['cookies']['webonix_session']),'Stored session is hashed');
    });
    scenario('customer deletion enforces admin, CSRF, ownership and session revocation',function(){
        $admin=session('admin@example.test'); $user=session();
        status(mutate('DELETE','/api/admin/customers/4'),401);
        status(mutate('DELETE','/api/admin/customers/4',$user),403);
        status(request('DELETE','/api/admin/customers/4',$admin),403);
        status(mutate('DELETE','/api/admin/customers/1',$admin),403);
        status(mutate('DELETE','/api/admin/customers/999',$admin),404);
        status(mutate('DELETE','/api/admin/customers/0',$admin),404);
        status(mutate('DELETE','/api/admin/customers/99999999999999999999999',$admin),404);
        resetLink();
        status(mutate('DELETE','/api/admin/customers/2',$admin),409);
        check((int)scalar('SELECT COUNT(*) FROM app_users WHERE id=2')===1,'Owned customer remains');
        check((int)scalar('SELECT COUNT(*) FROM password_resets WHERE user_id=2')===1,'Rejected deletion preserves reset');
        status(request('GET','/api/auth/me',$user),200);
        status(mutate('PUT','/api/cards/1/owner',$admin,['ownerId'=>null]),200);
        status(mutate('DELETE','/api/admin/customers/2',$admin),204);
        check((int)scalar('SELECT COUNT(*) FROM app_users WHERE id=2')===0,'Customer removed');
        check((int)scalar('SELECT COUNT(*) FROM auth_sessions WHERE user_id=2')===0,'Sessions removed');
        check((int)scalar('SELECT COUNT(*) FROM password_resets WHERE user_id=2')===0,'Recovery tokens removed');
        check((int)scalar('SELECT COUNT(*) FROM nfc_cards')===3,'Cards preserved');
        status(request('GET','/api/auth/me',$user),401);
        status(mutate('DELETE','/api/admin/customers/2',$admin),404);
        status(mutate('DELETE','/api/admin/customers/4',$admin),204);
    });
    scenario('wrong password, inactive and unknown accounts',function(){foreach(['user@example.test','disabled@example.test','missing@example.test'] as $email)status(login($email,'wrong'),401);status(login('disabled@example.test'),401);});
    scenario('authentication, roles and hidden ownership',function(){
        status(request('GET','/api/cards'),401);$u=session();status(request('GET','/api/admin/customers',$u),403);status(request('GET','/api/admin/users',$u),403);
        status(request('GET','/api/cards/2',$u),404);status(request('GET','/api/cards/999',$u),404);status(request('GET','/api/cards/3',$u),404);
        status(mutate('POST','/api/cards',$u,['name'=>'x']),403);status(mutate('DELETE','/api/cards/1',$u),403);status(mutate('PUT','/api/cards/1/owner',$u,['ownerId'=>3]),403);
        $r=request('GET','/api/cards',$u);status($r,200);check(array_column($r['body'],'id')===[1],'Only own cards');
        $r=request('GET','/api/statistics',$u);check($r['body']['totalCards']===1&&$r['body']['totalScans']===5,'Only own statistics');
        check(request('GET','/api/statistics',session('admin@example.test'))['body']['totalScans']===55,'Admin sees all statistics');
    });
    scenario('CSRF covers profile and all mutations; CORS rejects hostile origin',function(){
        $u=session();foreach([['PUT','/api/auth/me'],['PUT','/api/auth/me/password'],['POST','/api/auth/logout'],['PATCH','/api/cards/1/status'],['POST','/api/auth/forgot-password'],['POST','/api/auth/reset-password']] as [$m,$p])status(request($m,$p,$u,[]),403);
        status(request('GET','/api/auth/me',$u,null,['Origin'=>'https://evil.example']),403);
        status(mutate('PUT','/api/auth/me',$u,[],['Origin'=>'https://evil.example']),403);
        status(request('OPTIONS','/api/cards',[],null,['Access-Control-Request-Method'=>'PUT']),204);
    });
    scenario('profile validation, duplicate email and role spoof rejection',function(){
        $u=session();$body=['name'=>' Changed ','email'=>' NEW@example.test ','currentPassword'=>'old-test-password','id'=>1,'role'=>'ADMIN','active'=>false];
        $r=mutate('PUT','/api/auth/me',$u,$body);status($r,200);check($r['body']['id']===2&&$r['body']['role']==='USER'&&$r['body']['name']==='Changed'&&$r['body']['email']==='new@example.test','Identity and normalized profile');
        $body['email']='admin@example.test';status(mutate('PUT','/api/auth/me',$u,$body),409);
        $body['email']='invalid';status(mutate('PUT','/api/auth/me',$u,$body),400);
        $body['email']='valid@example.test';$body['name']=str_repeat('a',101);status(mutate('PUT','/api/auth/me',$u,$body),400);
        $body['name']='Valid';$body['currentPassword']='wrong';status(mutate('PUT','/api/auth/me',$u,$body),400);
    });
    scenario('password change revokes ALL sessions and recovery links',function()use($password){
        $first=session();$second=session();$other=session('other@example.test');resetLink();
        status(mutate('PUT','/api/auth/me/password',$first,$password+['id'=>3]),204);
        status(request('GET','/api/auth/me',$first),401);status(request('GET','/api/auth/me',$second),401);status(request('GET','/api/auth/me',$other),200);
        check((int)scalar('SELECT COUNT(*) FROM password_resets WHERE user_id=2')===0,'Old reset deleted');status(login(),401);status(login('user@example.test','new123'),200);
    });
    scenario('invalid password updates have no side effects',function()use($password){
        $u=session();resetLink();$hash=scalar('SELECT password FROM app_users WHERE id=2');
        foreach([['newPassword'=>'short','confirmPassword'=>'short'],['newPassword'=>str_repeat('ş',37),'confirmPassword'=>str_repeat('ş',37)],['newPassword'=>'      ','confirmPassword'=>'      '],['currentPassword'=>'wrong'],['confirmPassword'=>'different'],['newPassword'=>'old-test-password','confirmPassword'=>'old-test-password']] as $override)status(mutate('PUT','/api/auth/me/password',$u,array_replace($password,$override)),400);
        check(scalar('SELECT password FROM app_users WHERE id=2')===$hash,'Hash unchanged');status(request('GET','/api/auth/me',$u),200);check((int)scalar('SELECT COUNT(*) FROM password_resets')===1,'Reset preserved');
    });
    scenario('card update/status and owner transfer',function()use($card){
        $u=session();$admin=session('admin@example.test');status(mutate('PUT','/api/cards/1',$u,$card+['ownerId'=>3,'scans'=>999]),200);
        check((int)scalar('SELECT owner_id FROM nfc_cards WHERE id=1')===2&&(int)scalar('SELECT scans FROM nfc_cards WHERE id=1')===5,'Protected fields');
        $r=mutate('PATCH','/api/cards/1/status',$u);status($r,200);check($r['body']['active']===false,'Toggle status');
        status(mutate('PUT','/api/cards/1/owner',$admin,['ownerId'=>3]),200);status(mutate('PUT','/api/cards/1',$u,$card),404);status(mutate('PATCH','/api/cards/1/status',$u),404);
        status(mutate('PUT','/api/cards/1/owner',$admin,['ownerId'=>null]),200);status(mutate('PUT','/api/cards/1/owner',$admin,[]),400);
    });
    scenario('card/owner validation and admin create/delete',function()use($card){
        $admin=session('admin@example.test');foreach([['destinationUrl'=>'javascript:alert(1)'],['destinationUrl'=>'https://u:p@example.test'],['name'=>str_repeat('x',256)],['type'=>'unknown'],['ownerId'=>999],['ownerId'=>4],['ownerId'=>'2']] as $invalid)status(mutate('POST','/api/cards',$admin,array_replace($card,$invalid)),400);
        $r=mutate('POST','/api/cards',$admin,$card+['ownerId'=>2]);status($r,201);check(strlen($r['body']['code'])===16,'Random card code');$id=$r['body']['id'];status(mutate('DELETE',"/api/cards/$id",$admin),204);status(mutate('DELETE',"/api/cards/$id",$admin),404);
    });
    scenario('customer creation validates lengths and fixes role to USER',function(){
        $admin=session('admin@example.test');$body=['name'=>'Customer','email'=>'new@example.test','password'=>'new123','role'=>'ADMIN'];
        $r=mutate('POST','/api/admin/customers',$admin,$body);status($r,201);check($r['body']['role']==='USER'&&!isset($r['body']['password']),'Customer role and safe response');
        status(mutate('POST','/api/admin/customers',$admin,$body),409);status(mutate('POST','/api/admin/customers',$admin,array_replace($body,['password'=>str_repeat('x',73),'email'=>'long@example.test'])),400);
        $r=request('GET','/api/admin/customers',$admin);check(!in_array('ADMIN',array_column($r['body'],'role'),true),'Customers exclude admins');
    });
    scenario('password reset validates account/email/expiry and is single use',function(){
        $u=session();$token=resetLink();status(mutate('POST','/api/auth/reset-password',[],['token'=>$token,'password'=>'short']),400);
        status(mutate('POST','/api/auth/reset-password',[],['token'=>$token,'password'=>'new123']),200);status(request('GET','/api/auth/me',$u),401);
        status(mutate('POST','/api/auth/reset-password',[],['token'=>$token,'password'=>'other123']),400);
        foreach([resetLink(2,-1),resetLink(2,900,'old@example.test'),resetLink(4,900,'disabled@example.test')] as $invalid)status(mutate('POST','/api/auth/reset-password',[],['token'=>$invalid,'password'=>'new123']),400);
    });
    scenario('password reset token cannot win twice concurrently',function(){
        $token=resetLink();$csrf=request('GET','/api/auth/csrf');$multi=curl_multi_init();$handles=[];
        foreach(['new123','new456'] as $password){$h=handle('POST','/api/auth/reset-password',$csrf['cookies'],compact('token','password'),['X-XSRF-TOKEN'=>$csrf['body']['token']]);curl_multi_add_handle($multi,$h);$handles[]=$h;}
        do{curl_multi_exec($multi,$running);if($running)curl_multi_select($multi,0.1);}while($running);
        $codes=[];foreach($handles as $h){$codes[]=decode($h,curl_multi_getcontent($h))['status'];curl_multi_remove_handle($multi,$h);}sort($codes);check($codes===[200,400],'Exactly one reset succeeds');
    });
    scenario('rate limits cannot be bypassed by rotating email or source header',function(){
        for($i=0;$i<30;$i++)status(mutate('POST','/api/auth/login',[],['email'=>"missing$i@example.test",'password'=>'wrong'],['X-Forwarded-For'=>"192.0.2.$i"]),401);
        $r=login('admin@example.test');status($r,429);check(str_contains(strtolower($r['headers']),'retry-after:'),'Retry-After header');
    });
    scenario('password change attempts are bounded',function()use($password){
        $u=session();for($i=0;$i<10;$i++)status(mutate('PUT','/api/auth/me/password',$u,array_replace($password,['currentPassword'=>'wrong'])),400);
        status(mutate('PUT','/api/auth/me/password',$u,$password),429);status(request('GET','/api/auth/me',$u),200);
    });
    scenario('logout revokes copied session; disabled user loses access',function(){
        $u=session();$r=mutate('POST','/api/auth/logout',$u);status($r,204);check(str_contains(strtolower($r['headers']),'max-age=0'),'Cookie deleted');status(request('GET','/api/auth/me',$u),401);
        $u=session();query('UPDATE app_users SET active=0 WHERE id=2');status(request('GET','/api/auth/me',$u),401);
    });
    scenario('malformed payloads and disabled SMTP return controlled JSON',function(){
        $u=session();foreach(['{','[]','null',json_encode(['name'=>['nested']])] as $body)status(mutate('PUT','/api/auth/me',$u,$body),400);
        status(mutate('POST','/api/auth/forgot-password',[],['email'=>'user@example.test'],['X-Fixture-Mail'=>'disabled']),503);
        status(request('GET','/api/auth/register'),404);status(mutate('POST','/api/auth/register',[],[]),404);
    });
    scenario('NFC redirect increments scans only for valid active cards',function(){
        $r=request('GET','/r/CODE1');status($r,302);check(str_contains($r['headers'],'Location: https://example.test'),'Stored destination');
        check((int)scalar('SELECT scans FROM nfc_cards WHERE id=1')===6,'Atomic scan increment');
        query('UPDATE nfc_cards SET active=0 WHERE id=1');status(request('GET','/r/CODE1'),404);
        check((int)scalar('SELECT scans FROM nfc_cards WHERE id=1')===6,'Inactive card not counted');
        status(request('GET','/r/unknown'),404);query('UPDATE nfc_cards SET destination_url=? WHERE id=2',['javascript:alert(1)']);status(request('GET','/r/CODE2'),404);
    });
    scenario('SMTP recovery delivers token privately and replacement is rolled back on failure',function()use($outbox){
        foreach(glob($outbox.'/*.json') as $file)unlink($file);
        $known=mutate('POST','/api/auth/forgot-password',[],['email'=>'user@example.test']);status($known,202);
        $unknown=mutate('POST','/api/auth/forgot-password',[],['email'=>'missing@example.test']);status($unknown,202);check($known['body']===$unknown['body'],'Generic response');
        $files=glob($outbox.'/*.json');check(count($files)===1,'Only active known user gets mail');
        $mail=json_decode(file_get_contents($files[0]),true);check($mail['to']==='user@example.test','Only synthetic recipient');
        check(preg_match('/reset-password#token=([a-f0-9]{64})/',$mail['body'],$match)===1,'Reset link in captured mail');
        $first=$match[1];check(scalar('SELECT token_hash FROM password_resets WHERE user_id=2')===hash('sha256',$first),'Only hash persisted');
        status(mutate('POST','/api/auth/forgot-password',[],['email'=>'user@example.test']),202);
        status(mutate('POST','/api/auth/reset-password',[],['token'=>$first,'password'=>'new123']),400);
        $logPath=dirname(__DIR__).'/writable/logs/log-'.gmdate('Y-m-d').'.log';
        $before=is_file($logPath)?filesize($logPath):0;
        query('UPDATE app_users SET email=? WHERE id=2',['fail@example.test']);$old=resetLink(2,900,'fail@example.test');
        status(mutate('POST','/api/auth/forgot-password',[],['email'=>'fail@example.test']),202);
        check(scalar('SELECT token_hash FROM password_resets WHERE user_id=2')===hash('sha256',$old),'SMTP failure preserves old link');
        $written=is_file($logPath)?substr(file_get_contents($logPath),$before):'';
        check(!str_contains($written,'reset-password#token')&&!str_contains($written,$old),'SMTP errors must not log reset messages/tokens');
    });
    scenario('ownership transfer while an old owner edit waits prevents the stale write',function()use($card,$db){
        $u=session();$csrf=request('GET','/api/auth/csrf',$u);
        $db->begin_transaction();query('SELECT id FROM nfc_cards WHERE id=1 FOR UPDATE');
        $h=handle('PUT','/api/cards/1',array_merge($u,$csrf['cookies']),$card,['X-XSRF-TOKEN'=>$csrf['body']['token']]);
        $multi=curl_multi_init();curl_multi_add_handle($multi,$h);
        $until=microtime(true)+0.3;do{curl_multi_exec($multi,$running);curl_multi_select($multi,0.05);}while(microtime(true)<$until);
        query('UPDATE nfc_cards SET owner_id=3,version=version+1 WHERE id=1');$db->commit();
        do{curl_multi_exec($multi,$running);if($running)curl_multi_select($multi,0.1);}while($running);
        status(decode($h,curl_multi_getcontent($h)),404);curl_multi_remove_handle($multi,$h);
        check(scalar('SELECT name FROM nfc_cards WHERE id=1')==='Card 1','Stale owner cannot write after transfer');
    });
    scenario('production requires HTTPS and sends host-only secure cookies',function(){
        status(request('GET','/api/auth/csrf',[],null,['X-Fixture-Production'=>'insecure','Origin'=>'https://panel.example.test']),503);
        $r=request('GET','/api/auth/csrf',[],null,['X-Fixture-Production'=>'secure','Origin'=>'https://panel.example.test']);status($r,200);
        check(isset($r['cookies']['__Host-webonix_csrf']),'Host-prefixed production cookie');
        check(preg_match('/^Set-Cookie: __Host-webonix_csrf=.*; secure; HttpOnly; SameSite=Strict/mi',$r['headers'])===1,'Secure production attributes');
        check(!preg_match('/^Set-Cookie:.*; domain=/mi',$r['headers']),'No Domain attribute');
    });
    scenario('existing Java BCrypt hashes still authenticate',function(){
        $hash=scalar('SELECT password FROM app_users WHERE id=2');query('UPDATE app_users SET password=? WHERE id=2',[str_replace('$2y$','$2a$',$hash)]);status(login(),200);
    });
    echo "SUCCESS: $cases scenarios, $checks assertions. No production data or external email used.\n";
} catch(Throwable $e) { fwrite(STDERR,'FAIL: '.$e->getMessage()."\n"); $failed=true; }
finally {
    // PHP workers inherit the listener: terminate the process group when available.
    $status=proc_get_status($process);
    if(function_exists('posix_kill')) {
        // Only children of this exact test server, never unrelated PHP processes.
        exec('pgrep -P '.(int)$status['pid'], $children);
        foreach($children as $child)posix_kill((int)$child,SIGTERM);
    }
    proc_terminate($process);proc_close($process);
    proc_terminate($smtp);proc_close($smtp);foreach(glob($outbox.'/*.json') as $file)unlink($file);rmdir($outbox);
    if (isset($failed)) echo 'Test-server diagnostic log: '.$log.PHP_EOL; else unlink($log);
}
exit(isset($failed)?1:0);
