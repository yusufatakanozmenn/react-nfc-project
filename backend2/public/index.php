<?php

declare(strict_types=1);

use CodeIgniter\Boot;
use Config\Paths;

defined('FCPATH') || define('FCPATH', __DIR__ . DIRECTORY_SEPARATOR);

require FCPATH . '../vendor/autoload.php';
require FCPATH . '../app/Config/Paths.php';

$paths = new Paths();

require rtrim($paths->systemDirectory, '\\/ ') . DIRECTORY_SEPARATOR . 'Boot.php';

exit(Boot::bootWeb($paths));