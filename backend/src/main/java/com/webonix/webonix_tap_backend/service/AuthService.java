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

    @org.springframework.transaction.annotation.Transactional
    public AuthResponse register(RegisterRequest request) {
        AppUser user = createUser(request);
        return new AuthResponse(user.getId(), user.getName(), user.getEmail(), user.getRole(),
                null, "Kullanıcı başarıyla oluşturuldu.");
    }

    @org.springframework.transaction.annotation.Transactional
    public com.webonix.webonix_tap_backend.dto.UserSummaryResponse createCustomer(RegisterRequest request) {
        return toSummary(createUser(request));
    }

    public java.util.List<com.webonix.webonix_tap_backend.dto.UserSummaryResponse> listCustomers() {
        return appUserRepository.findByRoleOrderByNameAsc("USER").stream().map(this::toSummary).toList();
    }

    private AppUser createUser(RegisterRequest request) {
        String name = request.name() == null ? "" : request.name().trim();
        String email = request.email() == null ? "" : request.email().trim().toLowerCase(Locale.ROOT);
        String password = request.password();
        if (name.isEmpty() || name.length() > 100 || email.length() > 150
                || !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Geçerli bir müşteri adı ve e-posta girin.");
        }
        if (password == null || password.isBlank() || password.length() < 8
                || password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Şifre en az 8 karakter ve en fazla 72 bayt olmalıdır.");
        }
        if (appUserRepository.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Bu e-posta zaten kullanılıyor.");
        }
        AppUser user = new AppUser(name, email, passwordEncoder.encode(password), "USER", true);
        try {
            return appUserRepository.saveAndFlush(user);
        } catch (org.springframework.dao.DataIntegrityViolationException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Bu e-posta zaten kullanılıyor.");
        }
    }

    private com.webonix.webonix_tap_backend.dto.UserSummaryResponse toSummary(AppUser user) {
        return new com.webonix.webonix_tap_backend.dto.UserSummaryResponse(
                user.getId(), user.getName(), user.getEmail(), user.getActive());
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
        user.getId(),
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
    public AppUser requireActiveUser(String userId) {
        return appUserRepository.findById(Long.valueOf(userId))
                .filter(candidate -> Boolean.TRUE.equals(candidate.getActive()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }

    public CurrentUserResponse currentUser(String userId) {
        return toCurrentUser(requireActiveUser(userId));
    }

    @org.springframework.transaction.annotation.Transactional
    public CurrentUserResponse updateProfile(String userId, com.webonix.webonix_tap_backend.dto.UpdateProfileRequest request) {
        AppUser user = requireActiveUser(userId);
        String name = request.name() == null ? "" : request.name().trim();
        String updatedEmail = request.email() == null ? "" : request.email().trim().toLowerCase(Locale.ROOT);
        if (name.isEmpty() || name.length() > 100 || updatedEmail.length() > 150
                || !updatedEmail.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Geçerli bir ad ve e-posta girin.");
        }
        if (request.currentPassword() == null || !passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mevcut şifre hatalı.");
        }
        appUserRepository.findByEmail(updatedEmail).ifPresent(existing -> {
            if (!existing.getId().equals(user.getId())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Bu e-posta zaten kullanılıyor.");
            }
        });
        user.setName(name);
        user.setEmail(updatedEmail);
        try {
            return toCurrentUser(appUserRepository.saveAndFlush(user));
        } catch (org.springframework.dao.DataIntegrityViolationException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Bu e-posta zaten kullanılıyor.");
        }
    }

    public java.util.List<com.webonix.webonix_tap_backend.dto.UserSummaryResponse> listUsers() {
        return appUserRepository.findAll().stream().map(user ->
                new com.webonix.webonix_tap_backend.dto.UserSummaryResponse(
                        user.getId(), user.getName(), user.getEmail(), user.getActive())).toList();
    }

    private CurrentUserResponse toCurrentUser(AppUser user) {
        return new CurrentUserResponse(user.getId(), user.getName(), user.getEmail(), user.getRole());
    }
}
