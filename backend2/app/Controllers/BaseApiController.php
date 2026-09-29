<?php

declare(strict_types=1);
namespace App\Controllers;

use App\Exceptions\ApiException;
use CodeIgniter\HTTP\ResponseInterface;
use CodeIgniter\RESTful\ResourceController;
use CodeIgniter\HTTP\RequestInterface;
use Psr\Log\LoggerInterface;

abstract class BaseApiController extends ResourceController
{
    protected $db;
    protected bool $writing = false;
    private ?array $identity = null;
    private ?array $body = null;

    public function initController(RequestInterface $request, ResponseInterface $response, LoggerInterface $logger)
    {
        parent::initController($request, $response, $logger);
        $this->db = db_connect();
        $this->db->transException(true);
        $this->response->setHeader('Cache-Control', 'no-store')
            ->setHeader('X-Content-Type-Options', 'nosniff')->setHeader('X-Frame-Options', 'DENY')
            ->setHeader('Referrer-Policy', 'no-referrer')->setHeader('Content-Security-Policy', "default-src 'none'; frame-ancestors 'none'")
            ->setHeader('Vary', 'Origin');
        $origin = (string) env('APP_ORIGIN', 'http://localhost:5173');
        if ($this->request->getHeaderLine('Origin') === $origin) {
            $this->response->setHeader('Access-Control-Allow-Origin', $origin)
                ->setHeader('Access-Control-Allow-Credentials', 'true')
                ->setHeader('Access-Control-Allow-Headers', 'Content-Type, X-XSRF-TOKEN')
                ->setHeader('Access-Control-Allow-Methods', 'GET, POST, PUT, PATCH, DELETE, OPTIONS');
        }
    }

    // Explicit routes only; central enforcement also covers profile updates and future mutations.
    public function _remap(string $method, ...$params): ResponseInterface
    {
        $transaction = false;
        try {
            if (ENVIRONMENT === 'production' && (!$this->secureCookies() ||
                !str_starts_with((string) env('APP_ORIGIN', ''), 'https://') || !$this->request->isSecure())) {
                throw new ApiException('Sunucu güvenlik yapılandırması eksik.', 503);
            }
            $origin = $this->request->getHeaderLine('Origin');
            if ($origin !== '' && $origin !== (string) env('APP_ORIGIN', 'http://localhost:5173')) {
                throw new ApiException('İstek kaynağına izin verilmiyor.', 403);
            }
            $this->writing = !in_array(strtoupper($this->request->getMethod()), ['GET', 'HEAD', 'OPTIONS'], true);
            if ($this->writing) {
                if (!$this->requireCsrf()) throw new ApiException('Güvenlik doğrulaması geçersiz.', 403);
                $this->data();
                $this->limitRequest($method);
                if (!$this->db->transBegin()) throw new ApiException('İşlem başlatılamadı.', 503);
                $transaction = true;
            }
            if ($this instanceof CardController) foreach ($params as &$param) {
                if (!is_string($param) || !ctype_digit($param) || strlen($param) > 18 || (int) $param < 1) throw new ApiException('Kayıt bulunamadı.', 404);
                $param = (int) $param;
            }
            unset($param);
            $result = $this->{$method}(...$params);
            if ($transaction) {
                if (!$this->db->transStatus()) throw new ApiException('İşlem kaydedilemedi.', 503);
                if ($result->getStatusCode() >= 400) $this->db->transRollback();
                elseif (!$this->db->transCommit()) throw new ApiException('İşlem kaydedilemedi.', 503);
                $transaction = false;
                if ($result->getStatusCode() < 400) $result = $this->afterCommit($result);
            }
            return $result;
        } catch (ApiException $error) {
            if ($transaction) $this->db->transRollback();
            return $this->json(['message' => $error->getMessage()], $error->status);
        } catch (\Throwable $error) {
            if ($transaction) $this->db->transRollback();
            // Never log SQL, input, credentials, tokens or SMTP transcripts.
            log_message('error', 'API operation failed: {kind} at {fault_file}:{fault_line}', ['kind' => get_class($error), 'fault_file' => basename($error->getFile()), 'fault_line' => $error->getLine()]);
            if ((int) $error->getCode() === 1062) return $this->json(['message' => 'Bu kayıt zaten kullanılıyor.'], 409);
            return $this->json(['message' => 'İşlem gerçekleştirilemedi. Lütfen tekrar deneyin.'], 500);
        }
    }

    protected function afterCommit(ResponseInterface $result): ResponseInterface
    {
        return $result;
    }

    protected function data(): array
    {
        if ($this->body !== null) return $this->body;
        $raw = (string) $this->request->getBody();
        if (strlen($raw) > 16384) throw new ApiException('İstek çok büyük.', 413);
        if ($raw === '') return $this->body = [];
        try { $object = json_decode($raw, false, 32, JSON_THROW_ON_ERROR); }
        catch (\JsonException) { throw new ApiException('Geçersiz JSON.'); }
        if (!$object instanceof \stdClass) throw new ApiException('Geçersiz JSON nesnesi.');
        return $this->body = (array) $object;
    }

    private function limitRequest(string $method): void
    {
        if (!$this instanceof AuthController) return;
        $policy = match ($method) {
            'login' => [30, 10], 'forgotPassword' => [20, 3], 'resetPassword' => [10, 0],
            'changePassword', 'updateProfile' => [30, 10], default => null,
        };
        if ($policy === null) return;
        $keys = [[$method . ':ip', $this->request->getIPAddress(), $policy[0]]];
        if ($policy[1]) {
            // Resolve identity without a lock before entering the account transaction.
            $email = $this->data()['email'] ?? '';
            $account = in_array($method, ['login', 'forgotPassword'], true)
                ? (is_string($email) ? strtolower(trim($email)) : '')
                : (string) $this->lookupUser()['id'];
            $keys[] = [$method . ':account', $account, $policy[1]];
        }
        foreach ($keys as [$scope, $key, $max]) {
            $retry = $this->rateLimit($scope, $key, $max);
            if ($retry !== null) {
                $this->response->setHeader('Retry-After', (string) $retry);
                throw new ApiException('Çok fazla deneme. Lütfen daha sonra tekrar deneyin.', 429);
            }
        }
    }

    private function rateLimit(string $scope, string $identity, int $max): ?int
    {
        $key = hash('sha256', $scope . '|' . $identity);
        $this->db->transBegin();
        try {
            // Upsert acquires a lock even on the first concurrent attempt (no missing-row race).
            $this->db->query('INSERT INTO auth_rate_limits (id, attempts, window_started_at) VALUES (?, 0, UTC_TIMESTAMP()) ON DUPLICATE KEY UPDATE id = VALUES(id)', [$key]);
            $row = $this->db->query('SELECT attempts, TIMESTAMPDIFF(SECOND, window_started_at, UTC_TIMESTAMP()) AS elapsed FROM auth_rate_limits WHERE id = ? FOR UPDATE', [$key])->getRowArray();
            $elapsed = max(0, (int) $row['elapsed']);
            $retry = $elapsed < 900 && (int) $row['attempts'] >= $max ? max(1, 900 - $elapsed) : null;
            if ($retry === null) {
                if ($elapsed >= 900) $this->db->query('UPDATE auth_rate_limits SET attempts = 1, window_started_at = UTC_TIMESTAMP() WHERE id = ?', [$key]);
                else $this->db->query('UPDATE auth_rate_limits SET attempts = attempts + 1 WHERE id = ?', [$key]);
            }
            if (!$this->db->transStatus() || !$this->db->transCommit()) throw new ApiException('Deneme sınırı doğrulanamadı.', 503);
        } catch (\Throwable $error) { $this->db->transRollback(); throw $error; }
        $this->db->query('DELETE FROM auth_rate_limits WHERE window_started_at < UTC_TIMESTAMP() - INTERVAL 1 DAY LIMIT 100');
        return $retry;
    }

    protected function json(mixed $data, int $status = 200): ResponseInterface
    {
        return $this->response->setStatusCode($status)->setJSON($data);
    }

    protected function requireCsrf(): bool
    {
        $cookie = (string) $this->request->getCookie($this->cookieName('csrf'));
        $header = $this->request->getHeaderLine('X-XSRF-TOKEN');
        return preg_match('/^[a-f0-9]{64}$/D', $cookie) === 1 && hash_equals($cookie, $header);
    }

    private function lookupUser(bool $lock = false): array
    {
        $token = (string) $this->request->getCookie($this->cookieName('session'));
        if (preg_match('/^[a-f0-9]{64}$/D', $token) !== 1) throw new ApiException('Oturum gerekli.', 401);
        $user = $this->db->query('SELECT u.* FROM app_users u JOIN auth_sessions s ON u.id = s.user_id WHERE s.id = ? AND s.expires_at > UTC_TIMESTAMP() AND u.active = 1' . ($lock ? ' FOR UPDATE' : ''), [hash('sha256', $token)])->getRowArray();
        if ($user === null || !in_array($user['role'], ['ADMIN', 'USER'], true)) throw new ApiException('Oturum geçersiz.', 401);
        return $user;
    }

    protected function currentUser(): array
    {
        if ($this->identity !== null) return $this->identity;
        $user = $this->lookupUser();
        if ($this->writing) {
            $this->db->query('SELECT id FROM app_users WHERE id = ? FOR UPDATE', [$user['id']]);
            // Recheck both password/session revocation and current role after waiting for the lock.
            $user = $this->lookupUser(true);
        }
        return $this->identity = $user;
    }

    protected function admin(): array
    {
        $user = $this->currentUser();
        if ($user['role'] !== 'ADMIN') throw new ApiException('Bu işlem için yönetici yetkisi gerekir.', 403);
        return $user;
    }

    protected function secureCookies(): bool
    {
        return filter_var(env('COOKIE_SECURE', ENVIRONMENT === 'production'), FILTER_VALIDATE_BOOLEAN);
    }

    protected function cookieName(string $kind): string
    {
        return ($this->secureCookies() ? '__Host-' : '') . 'webonix_' . $kind;
    }

    protected function cookie(string $kind, string $value, int $seconds = 0): void
    {
        $cookie = new \CodeIgniter\Cookie\Cookie($this->cookieName($kind), $value, [
            'expires' => $seconds === 0 ? 0 : time() + $seconds, 'path' => '/', 'domain' => '',
            'secure' => $this->secureCookies(), 'httponly' => true, 'samesite' => 'Strict',
        ]);
        $this->response->setCookie($cookie);
    }
}
