<?php

declare(strict_types=1);

namespace Config;

use CodeIgniter\Config\BaseConfig;

class Routing extends BaseConfig
{
    public string $defaultNamespace = 'App\\Controllers';
    public string $defaultController = 'Home';
    public string $defaultMethod = 'index';
    public bool $translateURIDashes = false;
    public bool $override404 = false;
    public bool $autoRoute = false;
    public array $routeFiles = [];
    public bool $prioritize = false;
    public bool $useControllerAttributes = false;
    public bool $multipleSegmentsOneParam = false;
}
