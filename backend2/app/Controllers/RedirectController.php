<?php

declare(strict_types=1);
namespace App\Controllers;

use App\Exceptions\ApiException;
use CodeIgniter\HTTP\ResponseInterface;

final class RedirectController extends BaseApiController
{
    public function scan(string $code): ResponseInterface
    {
        if (preg_match('/^[a-zA-Z0-9_-]{1,64}$/D', $code) !== 1) throw new ApiException('Kart bulunamadı.', 404);
        $this->db->transBegin();
        try {
            $card = $this->db->query('SELECT id, active, destination_url FROM nfc_cards WHERE code = ? FOR UPDATE', [$code])->getRowArray();
            $url = $card['destination_url'] ?? '';
            if ($card === null || !$card['active'] || !filter_var($url, FILTER_VALIDATE_URL) ||
                !in_array(strtolower((string) parse_url($url, PHP_URL_SCHEME)), ['http', 'https'], true) ||
                parse_url($url, PHP_URL_USER) !== null || parse_url($url, PHP_URL_PASS) !== null) {
                throw new ApiException('Kart bulunamadı veya kullanıma kapalı.', 404);
            }
            $this->db->query('UPDATE nfc_cards SET scans = COALESCE(scans, 0) + 1, version = version + 1 WHERE id = ?', [$card['id']]);
            if (!$this->db->transStatus() || !$this->db->transCommit()) throw new ApiException('İşlem kaydedilemedi.', 503);
            return $this->response->setStatusCode(302)->setHeader('Location', $url);
        } catch (\Throwable $error) { $this->db->transRollback(); throw $error; }
    }
}
