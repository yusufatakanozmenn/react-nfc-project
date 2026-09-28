<?php

declare(strict_types=1);

namespace App\Controllers;

use CodeIgniter\HTTP\ResponseInterface;

class CorsController extends BaseApiController
{
    public function options(): ResponseInterface
    {
        return $this->response->setStatusCode(204);
    }
}
