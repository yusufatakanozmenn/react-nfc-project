<?php

declare(strict_types=1);

namespace Config;

use CodeIgniter\Config\AutoloadConfig;

class Autoload extends AutoloadConfig
{
    public $helpers = [];

    public $psr4 = [
        'App' => APPPATH,
    ];
}
