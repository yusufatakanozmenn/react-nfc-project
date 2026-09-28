<?php

declare(strict_types=1);

namespace Config;

use CodeIgniter\Config\Filters as BaseFilters;

class Filters extends BaseFilters
{
    public array $aliases = [];
    public array $required = [
        'before' => [],
        'after' => [],
    ];
    public array $globals = [
        'before' => [],
        'after' => [],
    ];
    public array $methods = [];
    public array $filters = [];
}
