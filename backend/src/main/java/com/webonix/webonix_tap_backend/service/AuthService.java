package com.webonix.webonix_tap_backend.service;

import com.webonix.webonix_tap_backend.dto.AuthResponse;
import com.webonix.webonix_tap_backend.dto.RegisterRequest;
import com.webonix.webonix_tap_backend.entity.AppUser;
import com.webonix.webonix_tap_backend.repository.AppUserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.webonix.webonix_tap_backend.dto.LoginRequest;
import com.webonix.webonix_tap_backend.dto.CurrentUserResponse;
import java.util.Locale;
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
                .toLowerCase(Locale.ROOT);

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

    if (request.email() == null || request.email().isBlank()
            || request.password() == null || request.password().isBlank()) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email ve şifre gereklidir.");
    }

    String email = request.email()
            .trim()
            .toLowerCase(Locale.ROOT);

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
    public CurrentUserResponse currentUser(String email) {
        AppUser user = appUserRepository.findByEmail(email)
                .filter(candidate -> Boolean.TRUE.equals(candidate.getActive()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        return new CurrentUserResponse(user.getId(), user.getName(), user.getEmail(), user.getRole());
    }
}
