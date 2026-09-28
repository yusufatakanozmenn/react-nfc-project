<?php
// Dedicated HTTP entry point: never load the developer/production .env in integration tests.
declare(strict_types=1);
$database = getenv('database.default.database');
if (!is_string($database) || !preg_match('/^[a-z0-9_]+_test$/D', $database)) {
    http_response_code(503); exit('Disposable test database required');
}
foreach (getenv() as $key => $value) { $_ENV[$key] = $value; $_SERVER[$key] = $value; }
if (($_SERVER['HTTP_X_FIXTURE_MAIL'] ?? '') === 'disabled') $_ENV['MAIL_ENABLED'] = 'false';
$production = $_SERVER['HTTP_X_FIXTURE_PRODUCTION'] ?? '';
if ($production !== '') {
    $_ENV['CI_ENVIRONMENT'] = $_SERVER['CI_ENVIRONMENT'] = 'production';
    $_ENV['APP_ORIGIN'] = 'https://panel.example.test';
    $_ENV['COOKIE_SECURE'] = $production === 'secure' ? 'true' : 'false';
    $_SERVER['HTTPS'] = $production === 'secure' ? 'on' : 'off';
}
$_SERVER['SCRIPT_NAME'] = '/index.php';
define('FCPATH', dirname(__DIR__) . '/public/');
require FCPATH . '../vendor/autoload.php';
require FCPATH . '../app/Config/Paths.php';
$paths = new class extends \Config\Paths { public string $envDirectory = __DIR__; };
require $paths->systemDirectory . '/Boot.php';
exit(\CodeIgniter\Boot::bootWeb($paths));
