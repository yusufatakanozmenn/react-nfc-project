<?php

declare(strict_types=1);
namespace App\Security;

use App\Exceptions\ApiException;

final class Input
{
    public static function text(array $data, string $key): string
    {
        if (!isset($data[$key]) || !is_string($data[$key])) {
            throw new ApiException('Geçerli alan bilgileri girin.');
        }
        return $data[$key];
    }

    public static function profile(array $data): array
    {
        $name = trim(self::text($data, 'name'));
        $email = strtolower(trim(self::text($data, 'email')));
        if ($name === '' || mb_strlen($name) > 100 || strlen($email) > 150 || !filter_var($email, FILTER_VALIDATE_EMAIL)) {
            throw new ApiException('Geçerli ad ve e-posta girin.');
        }
        return ['name' => $name, 'email' => $email];
    }

    public static function password(string $password): void
    {
        if (trim($password) === '' || mb_strlen($password) < 6 || strlen($password) > 72 || str_contains($password, "\0")) {
            throw new ApiException('Şifre en az 6 karakter ve en fazla 72 bayt olmalıdır.');
        }
    }

    public static function matches(string $password, string $hash): bool
    {
        return $password !== '' && strlen($password) <= 72 && !str_contains($password, "\0") && password_verify($password, $hash);
    }
}
