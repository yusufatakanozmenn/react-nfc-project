<?php

declare(strict_types=1);

use CodeIgniter\Router\RouteCollection;

/** @var RouteCollection $routes */
$routes->options('(:any)', 'CorsController::options');
$routes->get('api/auth/csrf', 'AuthController::csrf');
$routes->get('api/auth/me', 'AuthController::me');
$routes->put('api/auth/me', 'AuthController::updateProfile');
$routes->put('api/auth/me/password', 'AuthController::changePassword');
$routes->post('api/auth/login', 'AuthController::login');
$routes->post('api/auth/logout', 'AuthController::logout');
$routes->post('api/auth/forgot-password', 'AuthController::forgotPassword');
$routes->post('api/auth/reset-password', 'AuthController::resetPassword');

$routes->get('api/cards', 'CardController::index');
$routes->post('api/cards', 'CardController::create');
$routes->get('api/cards/(:num)', 'CardController::showCard/$1');
$routes->put('api/cards/(:num)', 'CardController::updateCard/$1');
$routes->patch('api/cards/(:num)/status', 'CardController::toggleStatus/$1');
$routes->put('api/cards/(:num)/owner', 'CardController::updateOwner/$1');
$routes->delete('api/cards/(:num)', 'CardController::deleteCard/$1');

$routes->get('api/admin/users', 'AdminUserController::users');
$routes->get('api/admin/customers', 'AdminUserController::customers');
$routes->post('api/admin/customers', 'AdminUserController::createCustomer');
$routes->get('api/statistics', 'StatisticsController::index');

// Public URL written to the NFC tag; inactive/unknown cards do not redirect.
$routes->get('r/(:segment)', 'RedirectController::scan/$1');
