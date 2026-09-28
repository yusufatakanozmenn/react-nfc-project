<?php

declare(strict_types=1);

namespace Config;

use CodeIgniter\Config\BaseConfig;

class Kint extends BaseConfig
{
    public int $maxDepth = 6;
    public bool $displayCalledFrom = false;
    public bool $expanded = false;
    public ?string $richTheme = null;
    public bool $richFolder = false;
    public array $richObjectPlugins = [];
    public array $richTabPlugins = [];
    public bool $cliColors = true;
    public bool $cliForceUTF8 = false;
    public bool $cliDetectWidth = true;
    public int $cliMinWidth = 40;
    public array $plugins = [];
}
