package com.webonix.webonix_tap_backend.service;

import com.webonix.webonix_tap_backend.dto.AuthResponse;
import com.webonix.webonix_tap_backend.dto.RegisterRequest;
import com.webonix.webonix_tap_backend.entity.AppUser;
import com.webonix.webonix_tap_backend.repository.AppUserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.webonix.webonix_tap_backend.dto.LoginRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            AppUserRepository appUserRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService
    ) {
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public AuthResponse register(RegisterRequest request) {

        String email = request.email()
                .trim()
                .toLowerCase();

        if (appUserRepository.existsByEmail(email)) {
            throw new RuntimeException("Bu email adresi zaten kayıtlı.");
        }

        String encodedPassword =
                passwordEncoder.encode(request.password());

        AppUser user = new AppUser(
                request.name(),
                email,
                encodedPassword,
                "USER",
                true
        );

        AppUser savedUser =
                appUserRepository.save(user);

        return new AuthResponse(
        savedUser.getId(),
        savedUser.getName(),
        savedUser.getEmail(),
        savedUser.getRole(),
        null,
        "Kullanıcı başarıyla oluşturuldu."
        );
    }

    public AuthResponse login(LoginRequest request) {

    String email = request.email()
            .trim()
            .toLowerCase();

    AppUser user = appUserRepository
            .findByEmail(email)
            .orElseThrow(() ->
                    new ResponseStatusException(
                            HttpStatus.UNAUTHORIZED,
                            "Email veya şifre hatalı."
                    )
            );

    if (!Boolean.TRUE.equals(user.getActive())) {
        throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Kullanıcı hesabı pasif."
        );
    }

    boolean passwordMatches =
            passwordEncoder.matches(
                    request.password(),
                    user.getPassword()
            );

    if (!passwordMatches) {
        throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Email veya şifre hatalı."
        );
    }
    String token = jwtService.generateToken(
        user.getEmail(),
        user.getRole()
    );
   return new AuthResponse(
        user.getId(),
        user.getName(),
        user.getEmail(),
        user.getRole(),
        token,
        "Giriş başarılı."
    );
    }
}