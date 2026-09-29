<?php

declare(strict_types=1);

namespace Config;

class View extends \CodeIgniter\Config\View
{
    // Do not carry one customer's template data into a later rendering.
    public $saveData = false;
}
