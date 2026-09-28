package com.webonix.webonix_tap_backend.dto;

public record ChangePasswordRequest(String currentPassword, String newPassword, String confirmPassword) {}
