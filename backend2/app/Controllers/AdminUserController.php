<?php

declare(strict_types=1);
namespace App\Controllers;

use App\Exceptions\ApiException;
use App\Security\Input;
use CodeIgniter\HTTP\ResponseInterface;

class AdminUserController extends BaseApiController
{
    private ?array $createdCustomer = null;

    protected function afterCommit(ResponseInterface $result): ResponseInterface
    {
        if ($this->createdCustomer === null) return $result;
        $customer = $this->createdCustomer;
        $this->createdCustomer = null;
        // SMTP failure must not turn a committed registration into a failed request.
        $status = \App\Mail\WelcomeMail::send($customer);
        return $this->json($customer + ['welcomeEmailStatus' => $status], 201);
    }

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
        $this->createdCustomer = ['id' => (int) $this->db->insertID()] + $profile + ['active' => true, 'role' => 'USER'];
        return $this->json($this->createdCustomer, 201);
    }

    public function deleteCustomer(string $id): ResponseInterface
    {
        $this->admin();
        // Admin routes receive string parameters through BaseApiController::_remap.
        if (!ctype_digit($id) || strlen($id) > 18 || (int) $id < 1) {
            throw new ApiException('Müşteri bulunamadı.', 404);
        }

        // The shared write transaction keeps this lock through commit. Card creation
        // and assignment lock the same owner row before assigning a card.
        $customer = $this->db->query('SELECT id, role FROM app_users WHERE id = ? FOR UPDATE', [(int) $id])->getRowArray();
        if ($customer === null) throw new ApiException('Müşteri bulunamadı.', 404);
        if ($customer['role'] !== 'USER') throw new ApiException('Yönetici hesabı bu işlemle silinemez.', 403);

        $card = $this->db->query('SELECT id FROM nfc_cards WHERE owner_id = ? LIMIT 1 FOR UPDATE', [(int) $id])->getRowArray();
        if ($card !== null) {
            throw new ApiException('Bu müşteriye bağlı kartlar var. Önce kartları başka müşteriye aktarın veya kart sahibini kaldırın.', 409);
        }

        // Explicit cleanup also supports the legacy schema without cascading FKs.
        $this->db->table('auth_sessions')->where('user_id', (int) $id)->delete();
        $this->db->table('password_resets')->where('user_id', (int) $id)->delete();
        $this->db->table('app_users')->where('id', (int) $id)->where('role', 'USER')->delete();
        return $this->response->setStatusCode(204)->setBody('');
    }

    private function listUsers(bool $customers): array
    {
        $builder = $this->db->table('app_users')->select('id, name, email, active, role')->orderBy('name');
        if ($customers) $builder->where('role', 'USER');
        return array_map(static fn (array $user): array => ['id' => (int) $user['id'], 'name' => $user['name'], 'email' => $user['email'], 'active' => (bool) $user['active'], 'role' => $user['role']], $builder->get()->getResultArray());
    }
}
