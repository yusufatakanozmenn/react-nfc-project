<?php
// Real CodeIgniter renderer regression test; no database, SMTP or project .env.
define('ENVIRONMENT', 'production');
$root = dirname(__DIR__);
define('FCPATH', $root . '/public/');
require $root . '/vendor/autoload.php';
require $root . '/app/Config/Paths.php';
$paths = new class extends \Config\Paths { public string $envDirectory = __DIR__ . '/fixtures/no-env'; };
require $paths->systemDirectory . '/Boot.php';
\CodeIgniter\Boot::bootConsole($paths);
try {
    $html = view('emails/welcome', ['name'=>'<script>test</script>', 'email'=>'test@example.test', 'loginUrl'=>'https://example.test/login']);
    if (!str_contains($html, 'https://example.test/login')) throw new RuntimeException('Link missing');
    if (!str_contains($html, '&lt;script&gt;test&lt;/script&gt;') || str_contains($html, '<script>test</script>')) throw new RuntimeException('Unsafe name rendering');
    if (config(\Config\View::class)->saveData !== false) throw new RuntimeException('View data must not persist');
    echo "PASS: real CI welcome view rendered\n";
} catch (Throwable $e) { echo get_class($e) . ': ' . $e->getMessage() . "\n"; exit(1); }
