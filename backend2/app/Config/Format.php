<?php

declare(strict_types=1);

namespace Config;

use CodeIgniter\Config\BaseConfig;
use CodeIgniter\Format\JSONFormatter;
use CodeIgniter\Format\XMLFormatter;

class Format extends BaseConfig
{
    public int $jsonEncodeDepth = 512;
    public array $formatterOptions = [];

    public array $formatters = [
        'application/json' => JSONFormatter::class,
        'application/xml' => XMLFormatter::class,
        'text/xml' => XMLFormatter::class,
    ];
}
