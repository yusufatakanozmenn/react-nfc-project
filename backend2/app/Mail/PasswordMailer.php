<?php

declare(strict_types=1);
namespace App\Mail;

/** CI's default failed-send logger includes the full message (and reset token). */
final class PasswordMailer extends \CodeIgniter\Email\Email
{
    protected function spoolEmail()
    {
        $this->unwrapSpecials();
        try {
            return $this->sendWithSmtp();
        } catch (\Throwable) {
            return false;
        } finally {
            // This dedicated sender never keeps a SMTP connection or logs a transcript.
            if (is_resource($this->SMTPConnect)) fclose($this->SMTPConnect);
            $this->SMTPConnect = false;
        }
    }
}
