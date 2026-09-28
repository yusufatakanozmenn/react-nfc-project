<?php

declare(strict_types=1);
namespace App\Controllers;

use App\Exceptions\ApiException;
use App\Security\Input;
use CodeIgniter\HTTP\ResponseInterface;

class CardController extends BaseApiController
{
    private const TYPES = ['google', 'instagram', 'whatsapp', 'website'];

    public function index(): ResponseInterface
    {
        $user = $this->currentUser();
        $builder = $this->cardsQuery();
        if ($user['role'] !== 'ADMIN') $builder->where('c.owner_id', $user['id']);
        return $this->json(array_map([$this, 'mapCard'], $builder->orderBy('c.id', 'DESC')->get()->getResultArray()));
    }

    public function showCard(int $id): ResponseInterface
    {
        return $this->json($this->findVisible($id));
    }

    public function create(): ResponseInterface
    {
        $this->admin();
        $data = $this->validatedCard($this->data());
        $owner = $this->validatedOwner($this->data()['ownerId'] ?? null);
        // 64-bit random codes fit the existing 16-character column; UNIQUE also enforces collision rejection.
        $this->db->table('nfc_cards')->insert($data + ['code' => strtoupper(bin2hex(random_bytes(8))), 'owner_id' => $owner, 'scans' => 0, 'active' => 1, 'version' => 0]);
        return $this->json($this->findVisible((int) $this->db->insertID()), 201);
    }

    public function updateCard(int $id): ResponseInterface
    {
        $this->findVisible($id);
        $data = $this->validatedCard($this->data());
        $this->db->table('nfc_cards')->where('id', $id)->set('version', 'version + 1', false)->update($data);
        return $this->json($this->findVisible($id));
    }

    public function toggleStatus(int $id): ResponseInterface
    {
        $this->findVisible($id);
        $this->db->query('UPDATE nfc_cards SET active = NOT active, version = version + 1 WHERE id = ?', [$id]);
        return $this->json($this->findVisible($id));
    }

    public function updateOwner(int $id): ResponseInterface
    {
        $this->admin();
        $data = $this->data();
        if (!array_key_exists('ownerId', $data)) throw new ApiException('Kart sahibi seçin veya null gönderin.');
        // Owner validation happens before the card lock, consistent with the user -> card lock order.
        $owner = $this->validatedOwner($data['ownerId']);
        $this->findVisible($id);
        $this->db->query('UPDATE nfc_cards SET owner_id = ?, version = version + 1 WHERE id = ?', [$owner, $id]);
        return $this->json($this->findVisible($id));
    }

    public function deleteCard(int $id): ResponseInterface
    {
        $this->admin();
        $this->findVisible($id);
        $this->db->table('nfc_cards')->where('id', $id)->delete();
        return $this->response->setStatusCode(204);
    }

    private function cardsQuery()
    {
        return $this->db->table('nfc_cards c')->select('c.*, u.name AS owner_name')->join('app_users u', 'u.id = c.owner_id', 'left');
    }

    private function findVisible(int $id): array
    {
        $user = $this->currentUser();
        if ($this->writing) {
            // Ownership is read under a lock held until commit, including assignment and deletion.
            $card = $this->db->query('SELECT * FROM nfc_cards WHERE id = ? FOR UPDATE', [$id])->getRowArray();
            if ($card !== null) {
                $owner = $card['owner_id'] === null ? null : $this->db->table('app_users')->select('name')->where('id', $card['owner_id'])->get()->getRowArray();
                $card['owner_name'] = $owner['name'] ?? null;
            }
        } else {
            $card = $this->cardsQuery()->where('c.id', $id)->get()->getRowArray();
        }
        if ($card === null || ($user['role'] !== 'ADMIN' && (int) $card['owner_id'] !== (int) $user['id'])) throw new ApiException('Kart bulunamadı.', 404);
        return $this->mapCard($card);
    }

    private function validatedOwner(mixed $owner): ?int
    {
        if ($owner === null) return null;
        if (!is_int($owner) || $owner < 1) throw new ApiException('Geçerli kullanıcı seçin.');
        $user = $this->db->query('SELECT id, active FROM app_users WHERE id = ? FOR UPDATE', [$owner])->getRowArray();
        if ($user === null || !$user['active']) throw new ApiException('Aktif bir kullanıcı seçin.');
        return $owner;
    }

    private function mapCard(array $card): array
    {
        return ['id' => (int) $card['id'], 'name' => $card['name'], 'type' => $card['type'], 'code' => $card['code'], 'destinationUrl' => $card['destination_url'], 'scans' => (int) $card['scans'], 'active' => (bool) $card['active'], 'ownerId' => $card['owner_id'] === null ? null : (int) $card['owner_id'], 'ownerName' => $card['owner_name']];
    }

    private function validatedCard(array $data): array
    {
        $name = trim(Input::text($data, 'name'));
        $url = trim(Input::text($data, 'destinationUrl'));
        $type = Input::text($data, 'type');
        if ($name === '' || mb_strlen($name) > 255 || strlen($url) > 255 || !in_array($type, self::TYPES, true) ||
            !filter_var($url, FILTER_VALIDATE_URL) || !in_array(strtolower((string) parse_url($url, PHP_URL_SCHEME)), ['http', 'https'], true) ||
            parse_url($url, PHP_URL_USER) !== null || parse_url($url, PHP_URL_PASS) !== null) {
            throw new ApiException('Geçerli kart bilgileri girin (ad ve adres en fazla 255 karakter).');
        }
        return ['name' => $name, 'type' => $type, 'destination_url' => $url];
    }
}
