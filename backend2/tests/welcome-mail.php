<?php
// Isolated mail-composition tests: no network, credentials or customer database.
declare(strict_types=1);
namespace App\Mail {
    final class PasswordMailer {
        public static array $message = [];
        public static bool $fail = false;
        public function __construct(mixed $config) {}
        public function clear(bool $attachments): void { self::$message = []; }
        public function setFrom(string $email, string $name): void { self::$message['from'] = $email; }
        public function setTo(string $email): void { self::$message['to'] = $email; }
        public function setSubject(string $subject): void { self::$message['subject'] = $subject; }
        public function setMessage(string $html): void { self::$message['html'] = $html; }
        public function setAltMessage(string $text): void { self::$message['text'] = $text; }
        public function send(): bool { return !self::$fail; }
    }
}
namespace {
    define('ENVIRONMENT', 'production');
    $settings = ['MAIL_ENABLED' => true, 'MAIL_FROM' => 'hello@example.test', 'SMTP_HOST' => 'smtp.example.test', 'SMTP_ENCRYPTION' => 'tls', 'APP_ORIGIN' => 'https://panel.example.test'];
    function env(string $key, mixed $fallback = null): mixed { global $settings; return $settings[$key] ?? $fallback; }
    function config(string $name): array { return []; }
    function log_message(string $level, string $message): void { if (str_contains($message, '@')) throw new \RuntimeException('PII logged'); }
    function view(string $path, array $data): string { extract($data); ob_start(); require dirname(__DIR__) . '/app/Views/' . $path . '.php'; return ob_get_clean(); }
    function check(bool $value, string $label): void { if (!$value) throw new \RuntimeException($label); }
    require dirname(__DIR__) . '/app/Mail/WelcomeMail.php';
    $customer = ['name' => '<img src=x onerror=alert(1)>', 'email' => 'customer@example.test', 'password' => 'NEVER-EMAIL-THIS'];
    check(\App\Mail\WelcomeMail::send($customer) === 'sent', 'Send succeeds');
    $message = \App\Mail\PasswordMailer::$message;
    check(str_contains($message['html'], '&lt;img'), 'Name escaped');
    check(!str_contains($message['html'], '<img src=x'), 'No injected markup');
    check(str_contains($message['html'], 'https://panel.example.test/login'), 'Configured panel URL');
    check(!str_contains(json_encode($message), $customer['password']), 'No password in message');
    check($message['to'] === $customer['email'] && str_contains($message['text'], '/login'), 'Recipient and plain-text fallback');
    \App\Mail\PasswordMailer::$fail = true;
    check(\App\Mail\WelcomeMail::send($customer) === 'failed', 'SMTP rejection reported');
    $settings['MAIL_ENABLED'] = false;
    check(\App\Mail\WelcomeMail::send($customer) === 'disabled', 'Disabled SMTP reported');
    $settings['MAIL_ENABLED'] = true; $settings['APP_ORIGIN'] = 'http://panel.example.test';
    check(\App\Mail\WelcomeMail::send($customer) === 'failed', 'HTTPS required in production');
    echo "PASS: welcome email escaping, recipient, links, password exclusion, fallback, SMTP failure and configuration checks.\n";
}
