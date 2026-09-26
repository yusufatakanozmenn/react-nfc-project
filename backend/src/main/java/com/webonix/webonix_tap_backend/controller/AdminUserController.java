package com.webonix.webonix_tap_backend.controller;

import com.webonix.webonix_tap_backend.dto.RegisterRequest;
import com.webonix.webonix_tap_backend.dto.UserSummaryResponse;
import com.webonix.webonix_tap_backend.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminUserController {
    private final AuthService auth;
    public AdminUserController(AuthService auth) { this.auth = auth; }

    @GetMapping("/users")
    public List<UserSummaryResponse> listUsers() { return auth.listUsers(); }

    @GetMapping("/customers")
    public List<UserSummaryResponse> listCustomers() { return auth.listCustomers(); }

    @PostMapping("/customers")
    @ResponseStatus(HttpStatus.CREATED)
    public UserSummaryResponse createCustomer(@RequestBody RegisterRequest request) {
        return auth.createCustomer(request);
    }
}
