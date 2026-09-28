<?php

declare(strict_types=1);

namespace App\Controllers;

use CodeIgniter\HTTP\ResponseInterface;

class StatisticsController extends BaseApiController
{
    public function index(): ResponseInterface
    {
        $user = $this->currentUser();
        $builder = $this->db->table('nfc_cards')->select('id, name, type, code, destination_url, scans, active, owner_id');
        if ($user['role'] !== 'ADMIN') {
            $builder->where('owner_id', $user['id']);
        }
        $rows = $builder->get()->getResultArray();
        $active = count(array_filter($rows, static fn (array $row): bool => (bool) $row['active']));
        $cards = array_map(static fn (array $row): array => [
            'id' => (int) $row['id'],
            'name' => $row['name'],
            'type' => $row['type'],
            'code' => $row['code'],
            'destinationUrl' => $row['destination_url'],
            'scans' => (int) $row['scans'],
            'active' => (bool) $row['active'],
            'ownerId' => $row['owner_id'] === null ? null : (int) $row['owner_id'],
        ], $rows);
        return $this->json(['totalCards' => count($cards), 'activeCards' => $active, 'inactiveCards' => count($cards) - $active, 'totalScans' => array_sum(array_column($cards, 'scans')), 'cards' => $cards]);
    }
}
