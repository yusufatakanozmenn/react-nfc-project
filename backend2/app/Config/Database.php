<?php

declare(strict_types=1);

namespace Config;

use CodeIgniter\Database\Config;

class Database extends Config
{
    public string $defaultGroup = 'default';
    public array $default;

    public function __construct()
    {
        $this->default = [
            'DSN'      => '',
            'hostname' => env('database.default.hostname', '127.0.0.1'),
            'username' => env('database.default.username', 'root'),
            'password' => env('database.default.password', ''),
            'database' => env('database.default.database', 'webonix_tap'),
            'DBDriver' => env('database.default.DBDriver', 'MySQLi'),
            'DBPrefix' => '',
            'pConnect' => false,
            'DBDebug'  => true,
            'charset'  => 'utf8mb4',
            'DBCollat' => 'utf8mb4_general_ci',
            'swapPre'  => '',
            'encrypt'  => false,
            'compress' => false,
            'strictOn' => true,
            'failover' => [],
            'port'     => (int) env('database.default.port', 3306),
        ];
    }
}
