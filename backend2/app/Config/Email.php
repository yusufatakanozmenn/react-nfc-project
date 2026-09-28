<?php

declare(strict_types=1);

namespace Config;

use CodeIgniter\Config\BaseConfig;

class Email extends BaseConfig
{
    public string $fromEmail = '';
    public string $fromName = 'Webonix Tap';
    public string $protocol = 'smtp';
    public string $SMTPHost = '';
    public string $SMTPAuthMethod = 'login';
    public string $SMTPUser = '';
    public string $SMTPPass = '';
    public int $SMTPPort = 587;
    public int $SMTPTimeout = 5;
    public string $SMTPCrypto = 'tls';
    public string $mailType = 'html';
    public string $charset = 'UTF-8';
    public bool $validate = true;
    public int $priority = 3;
    public string $CRLF = "\r\n";
    public string $newline = "\r\n";

    public function __construct()
    {
        parent::__construct();
        $this->fromEmail = (string) env('MAIL_FROM', '');
        $this->fromName = (string) env('MAIL_FROM_NAME', 'Webonix Tap');
        $this->SMTPHost = (string) env('SMTP_HOST', '');
        $this->SMTPUser = (string) env('SMTP_USERNAME', '');
        $this->SMTPPass = (string) env('SMTP_PASSWORD', '');
        $this->SMTPPort = (int) env('SMTP_PORT', 587);
        $this->SMTPCrypto = (string) env('SMTP_ENCRYPTION', 'tls');
    }
}
