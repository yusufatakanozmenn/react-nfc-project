<?php

declare(strict_types=1);

namespace Config;

use CodeIgniter\Cache\Handlers\DummyHandler;
use CodeIgniter\Cache\Handlers\FileHandler;
use CodeIgniter\Config\BaseConfig;

class Cache extends BaseConfig
{
    public string $handler = 'file';
    public string $backupHandler = 'dummy';
    public string $prefix = '';
    public int $ttl = 60;
    public string $storePath = WRITEPATH . 'cache/';
    public string $reservedCharacters = '{}()/\\@:';
    public bool $cacheQueryString = false;
    public array $file = [
        'storePath' => WRITEPATH . 'cache',
        'mode' => 0640,
    ];
    public array $validHandlers = [
        'dummy' => DummyHandler::class,
        'file' => FileHandler::class,
    ];
}
