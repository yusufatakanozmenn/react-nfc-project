<?php

declare(strict_types=1);
namespace App\Controllers;

use App\Exceptions\ApiException;
use App\Security\Input;
use CodeIgniter\HTTP\ResponseInterface;

class AdminUserController extends BaseApiController
{
    public function users(): ResponseInterface
    {
        $this->admin();
        return $this->json($this->listUsers(false));
    }

    public function customers(): ResponseInterface
    {
        $this->admin();
        return $this->json($this->listUsers(true));
    }

    public function createCustomer(): ResponseInterface
    {
        $this->admin();
        $data = $this->data();
        $profile = Input::profile($data);
        $password = Input::text($data, 'password');
        Input::password($password);
        if ($this->db->table('app_users')->where('email', $profile['email'])->countAllResults()) throw new ApiException('Bu e-posta zaten kullanılıyor.', 409);
        $this->db->table('app_users')->insert($profile + ['password' => password_hash($password, PASSWORD_BCRYPT, ['cost' => 10]), 'role' => 'USER', 'active' => 1]);
        return $this->json(['id' => (int) $this->db->insertID()] + $profile + ['active' => true, 'role' => 'USER'], 201);
    }

    private function listUsers(bool $customers): array
    {
        $builder = $this->db->table('app_users')->select('id, name, email, active, role')->orderBy('name');
        if ($customers) $builder->where('role', 'USER');
        return array_map(static fn (array $user): array => ['id' => (int) $user['id'], 'name' => $user['name'], 'email' => $user['email'], 'active' => (bool) $user['active'], 'role' => $user['role']], $builder->get()->getResultArray());
    }
}
