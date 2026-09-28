<?php

declare(strict_types=1);
namespace App\Exceptions;

use CodeIgniter\Debug\ExceptionHandlerInterface;
use CodeIgniter\HTTP\RequestInterface;
use CodeIgniter\HTTP\ResponseInterface;

final class ApiExceptionHandler implements ExceptionHandlerInterface
{
    public function handle(\Throwable $exception, RequestInterface $request, ResponseInterface $response, int $statusCode, int $exitCode): void
    {
        if ($statusCode < 400 || $statusCode > 599) $statusCode = 500;
        if ($statusCode !== 404) log_message('error', 'Unhandled API error: {kind}', ['kind' => get_class($exception)]);
        $message = $statusCode === 404 ? 'Kaynak bulunamadı.' : 'İşlem gerçekleştirilemedi. Lütfen tekrar deneyin.';
        if (is_cli()) { fwrite(STDERR, $message . PHP_EOL); return; }
        $origin = $request->getHeaderLine('Origin');
        if ($origin === (string) env('APP_ORIGIN', 'http://localhost:5173')) {
            $response->setHeader('Access-Control-Allow-Origin', $origin)->setHeader('Access-Control-Allow-Credentials', 'true');
        }
        $response->setStatusCode($statusCode)->setHeader('Cache-Control', 'no-store')->setHeader('Vary', 'Origin')
            ->setHeader('X-Content-Type-Options', 'nosniff')->setJSON(['message' => $message])->send();
    }
}
