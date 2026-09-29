<?php

declare(strict_types=1);
namespace App\Mail;

final class WelcomeMail
{
    public static function send(array $customer): string
    {
        if (!filter_var(env('MAIL_ENABLED', false), FILTER_VALIDATE_BOOLEAN)) return 'disabled';
        try {
            $origin = rtrim((string) env('APP_ORIGIN', ''), '/');
            if (!filter_var($origin, FILTER_VALIDATE_URL) ||
                !in_array(parse_url($origin, PHP_URL_SCHEME), ['http', 'https'], true) ||
                !filter_var((string) env('MAIL_FROM', ''), FILTER_VALIDATE_EMAIL) ||
                trim((string) env('SMTP_HOST', '')) === '' ||
                (ENVIRONMENT === 'production' && (!str_starts_with($origin, 'https://') ||
                    !in_array((string) env('SMTP_ENCRYPTION', 'tls'), ['tls', 'ssl'], true)))) {
                throw new \RuntimeException('Invalid mail configuration');
            }
            $loginUrl = $origin . '/login';
            $mailer = new PasswordMailer(config('Email'));
            $mailer->clear(true);
            $mailer->setFrom((string) env('MAIL_FROM', ''), (string) env('MAIL_FROM_NAME', 'Webonix Tap'));
            $mailer->setTo($customer['email']);
            $mailer->setSubject('Webonix Tap’a hoş geldiniz!');
            $mailer->setMessage(view('emails/welcome', ['name' => $customer['name'], 'email' => $customer['email'], 'loginUrl' => $loginUrl]));
            $mailer->setAltMessage("Merhaba {$customer['name']},\n\nWebonix Tap hesabınız hazır.\nGiriş e-postanız: {$customer['email']}\nPanele giriş: {$loginUrl}\n\nSize atanan NFC kartlarını düzenleyebilir, okutma istatistiklerini inceleyebilir ve hesap bilgilerinizi güncelleyebilirsiniz.\n\nŞifrenizi bilmiyorsanız giriş sayfasındaki Şifremi unuttum seçeneğini kullanın.\n\nWebonix Tap · Bir dokunuşla bağlantı kur.");
            if (!$mailer->send()) throw new \RuntimeException('Mail transport failed');
            return 'sent';
        } catch (\Throwable) {
            log_message('error', 'Welcome email delivery failed.');
            return 'failed';
        }
    }
}
