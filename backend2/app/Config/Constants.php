<?php

declare(strict_types=1);

/**
 * --------------------------------------------------------------------
 * Constants
 * --------------------------------------------------------------------
 */

defined('FCPATH') || define('FCPATH', ROOTPATH . 'public' . DIRECTORY_SEPARATOR);
defined('APPPATH') || define('APPPATH', ROOTPATH . 'app' . DIRECTORY_SEPARATOR);
defined('SYSTEMPATH') || define('SYSTEMPATH', ROOTPATH . 'vendor/codeigniter4/framework/system' . DIRECTORY_SEPARATOR);
defined('WRITEPATH') || define('WRITEPATH', ROOTPATH . 'writable' . DIRECTORY_SEPARATOR);
defined('TESTPATH') || define('TESTPATH', ROOTPATH . 'tests' . DIRECTORY_SEPARATOR);
defined('COMPOSER_PATH') || define('COMPOSER_PATH', ROOTPATH . 'vendor/autoload.php');
defined('APP_NAMESPACE') || define('APP_NAMESPACE', 'App');
defined('EXIT_SUCCESS') || define('EXIT_SUCCESS', 0);
defined('EXIT_ERROR') || define('EXIT_ERROR', 1);
defined('EXIT_CONFIG') || define('EXIT_CONFIG', 3);
defined('EXIT_UNKNOWN_FILE') || define('EXIT_UNKNOWN_FILE', 4);
defined('EXIT_UNKNOWN_CLASS') || define('EXIT_UNKNOWN_CLASS', 5);
defined('EXIT_UNKNOWN_METHOD') || define('EXIT_UNKNOWN_METHOD', 6);
defined('EXIT_USER_INPUT') || define('EXIT_USER_INPUT', 7);
defined('EXIT_DATABASE') || define('EXIT_DATABASE', 8);
$ciEnvironment = $_ENV['CI_ENVIRONMENT'] ?? $_SERVER['CI_ENVIRONMENT'] ?? getenv('CI_ENVIRONMENT') ?: 'production';
defined('CI_DEBUG') || define('CI_DEBUG', $ciEnvironment !== 'production');
