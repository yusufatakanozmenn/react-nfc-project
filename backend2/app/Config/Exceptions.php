<?php

declare(strict_types=1);
namespace Config;

use App\Exceptions\ApiExceptionHandler;
use CodeIgniter\Config\BaseConfig;
use CodeIgniter\Debug\ExceptionHandlerInterface;

class Exceptions extends BaseConfig
{
    // Our handler logs only the exception class; framework traces can contain secrets/SQL.
    public bool $log = false;
    public array $ignoreCodes = [404];
    public string $errorViewPath = APPPATH . 'Views/errors';
    public array $sensitiveDataInTrace = [];
    public bool $logDeprecations = true;
    public string $deprecationLogLevel = 'warning';

    public function handler(int $statusCode, \Throwable $exception): ExceptionHandlerInterface
    {
        return new ApiExceptionHandler();
    }
}
