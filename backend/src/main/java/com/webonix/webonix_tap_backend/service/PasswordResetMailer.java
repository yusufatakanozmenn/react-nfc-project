package com.webonix.webonix_tap_backend.service;
public interface PasswordResetMailer {
    boolean available();
    void sendLink(String email, String link);
}
