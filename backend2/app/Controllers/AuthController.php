<?php

declare(strict_types=1);
namespace App\Controllers;

use App\Exceptions\ApiException;
use App\Security\Input;
use CodeIgniter\HTTP\ResponseInterface;

class AuthController extends BaseApiController
{
    // A fixed dummy hash makes unknown-account login perform the same expensive verification.
    private const DUMMY_HASH = '$2y$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.';

    public function csrf(): ResponseInterface
    {
        $token = (string) $this->request->getCookie($this->cookieName('csrf'));
        if (preg_match('/^[a-f0-9]{64}$/D', $token) !== 1) {
            $token = bin2hex(random_bytes(32));
            $this->cookie('csrf', $token);
        }
        return $this->json(['token' => $token, 'headerName' => 'X-XSRF-TOKEN']);
    }

    public function login(): ResponseInterface
    {
        $data = $this->data();
        $email = strtolower(trim(Input::text($data, 'email')));
        $password = Input::text($data, 'password');
        if (isset($data['rememberMe']) && !is_bool($data['rememberMe'])) throw new ApiException('Geçersiz oturum seçimi.');
        $user = $this->db->query('SELECT * FROM app_users WHERE email = ? FOR UPDATE', [$email])->getRowArray();
        $valid = Input::matches($password, $user['password'] ?? self::DUMMY_HASH);
        if (!$valid || $user === null || !$user['active'] || !in_array($user['role'], ['ADMIN', 'USER'], true)) {
            throw new ApiException('E-posta veya şifre hatalı.', 401);
        }
        $this->revoke();
        $token = bin2hex(random_bytes(32));
        $remember = $data['rememberMe'] ?? false;
        $seconds = $remember ? 604800 : 3600;
        $this->db->table('auth_sessions')->insert([
            'id' => hash('sha256', $token), 'user_id' => $user['id'],
            'expires_at' => gmdate('Y-m-d H:i:s', time() + $seconds),
        ]);
        $this->cookie('session', $token, $remember ? $seconds : 0);
        return $this->json($this->publicUser($user));
    }

    public function logout(): ResponseInterface
    {
        $this->revoke();
        $this->cookie('session', '', -3600);
        return $this->response->setStatusCode(204);
    }

    public function me(): ResponseInterface
    {
        return $this->json($this->publicUser($this->currentUser()));
    }

    public function updateProfile(): ResponseInterface
    {
        $user = $this->currentUser();
        $data = $this->data();
        if (!Input::matches(Input::text($data, 'currentPassword'), $user['password'])) throw new ApiException('Mevcut şifre hatalı.');
        $profile = Input::profile($data);
        if ($this->db->table('app_users')->where('email', $profile['email'])->where('id !=', $user['id'])->countAllResults()) {
            throw new ApiException('Bu e-posta zaten kullanılıyor.', 409);
        }
        $this->db->table('app_users')->where('id', $user['id'])->update($profile);
        if ($profile['email'] !== $user['email']) $this->db->table('password_resets')->where('user_id', $user['id'])->delete();
        // Only validated fields enter the response; client-supplied role/id cannot spoof identity.
        return $this->json($this->publicUser(array_replace($user, $profile)));
    }

    public function changePassword(): ResponseInterface
    {
        $user = $this->currentUser();
        $data = $this->data();
        if (!Input::matches(Input::text($data, 'currentPassword'), $user['password'])) throw new ApiException('Mevcut şifre hatalı.');
        $password = Input::text($data, 'newPassword');
        Input::password($password);
        if ($password !== Input::text($data, 'confirmPassword')) throw new ApiException('Yeni şifreler eşleşmiyor.');
        if (Input::matches($password, $user['password'])) throw new ApiException('Yeni şifre mevcut şifrenizden farklı olmalıdır.');
        $this->replacePassword((int) $user['id'], $password);
        $this->cookie('session', '', -3600);
        return $this->response->setStatusCode(204);
    }

    public function forgotPassword(): ResponseInterface
    {
        $email = strtolower(trim(Input::text($this->data(), 'email')));
        if (!filter_var($email, FILTER_VALIDATE_EMAIL) || strlen($email) > 150) throw new ApiException('Geçerli bir e-posta adresi girin.');
        if (!filter_var(env('MAIL_ENABLED', false), FILTER_VALIDATE_BOOLEAN)) throw new ApiException('Şifre sıfırlama e-posta servisi henüz yapılandırılmadı.', 503);
        if (!filter_var((string) env('MAIL_FROM', ''), FILTER_VALIDATE_EMAIL) || trim((string) env('SMTP_HOST', '')) === '' ||
            (ENVIRONMENT === 'production' && !in_array((string) env('SMTP_ENCRYPTION', 'tls'), ['tls', 'ssl'], true))) {
            throw new ApiException('Şifre sıfırlama e-posta yapılandırması eksik.', 503);
        }
        $user = $this->db->query('SELECT * FROM app_users WHERE email = ? FOR UPDATE', [$email])->getRowArray();
        if ($user !== null && $user['active']) {
            $token = bin2hex(random_bytes(32));
            $this->db->table('password_resets')->where('user_id', $user['id'])->delete();
            $this->db->table('password_resets')->insert([
                'user_id' => $user['id'], 'token_hash' => hash('sha256', $token), 'email' => $email,
                'expires_at' => gmdate('Y-m-d H:i:s', time() + 900),
            ]);
            $link = rtrim((string) env('APP_ORIGIN', 'http://localhost:5173'), '/') . '/reset-password#token=' . $token;
            try {
                $mailer = new \App\Mail\PasswordMailer(config('Email'));
                $mailer->clear(true);
                $mailer->setFrom((string) env('MAIL_FROM', ''), (string) env('MAIL_FROM_NAME', 'Webonix Tap'));
                $mailer->setTo($email);
                $mailer->setSubject('Webonix Tap şifre yenileme');
                $mailer->setMessage('<p>Şifrenizi yenilemek için aşağıdaki bağlantıyı 15 dakika içinde açın:</p><p><a href="' . htmlspecialchars($link, ENT_QUOTES, 'UTF-8') . '">Şifremi yenile</a></p>');
                if (!$mailer->send()) throw new \RuntimeException('Mail transport failed');
            } catch (\Throwable) {
                // Roll back replacement, preserving an earlier valid link. Do not expose account existence on SMTP failure.
                log_message('error', 'Password recovery mail delivery failed.');
                throw new ApiException('Bu e-posta ile kayıtlı aktif bir hesabınız varsa sıfırlama bağlantısı gönderilecektir.', 202);
            }
        }
        return $this->json(['message' => 'Bu e-posta ile kayıtlı aktif bir hesabınız varsa sıfırlama bağlantısı gönderilecektir.'], 202);
    }

    public function resetPassword(): ResponseInterface
    {
        $data = $this->data();
        $token = Input::text($data, 'token');
        $password = Input::text($data, 'password');
        Input::password($password);
        $invalid = 'Şifre yenileme bağlantısı geçersiz veya süresi dolmuş.';
        if (preg_match('/^[a-f0-9]{64}$/Di', $token) !== 1) throw new ApiException($invalid);
        $hash = hash('sha256', $token);
        $candidate = $this->db->table('password_resets')->where('token_hash', $hash)->get()->getRowArray();
        if ($candidate === null) throw new ApiException($invalid);
        // Lock user first, consistently with login/profile/change/forgot; then read fresh reset state.
        $user = $this->db->query('SELECT * FROM app_users WHERE id = ? FOR UPDATE', [$candidate['user_id']])->getRowArray();
        $reset = $this->db->query('SELECT * FROM password_resets WHERE user_id = ? AND token_hash = ? AND expires_at > UTC_TIMESTAMP() FOR UPDATE', [$candidate['user_id'], $hash])->getRowArray();
        if ($reset === null || $user === null || !$user['active'] || $reset['email'] !== $user['email']) throw new ApiException($invalid);
        $this->replacePassword((int) $user['id'], $password);
        $this->cookie('session', '', -3600);
        return $this->json(['message' => 'Şifreniz yenilendi. Tüm oturumlar kapatıldı; yeni şifrenizle giriş yapabilirsiniz.']);
    }

    private function replacePassword(int $id, string $password): void
    {
        $this->db->table('app_users')->where('id', $id)->update(['password' => password_hash($password, PASSWORD_BCRYPT, ['cost' => 10])]);
        $this->db->table('password_resets')->where('user_id', $id)->delete();
        $this->db->table('auth_sessions')->where('user_id', $id)->delete();
    }

    private function revoke(): void
    {
        $token = (string) $this->request->getCookie($this->cookieName('session'));
        if ($token !== '') $this->db->table('auth_sessions')->where('id', hash('sha256', $token))->delete();
    }

    private function publicUser(array $user): array
    {
        return ['id' => (int) $user['id'], 'name' => $user['name'], 'email' => $user['email'], 'role' => $user['role']];
    }
}
