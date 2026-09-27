package com.webonix.webonix_tap_backend.controller;
import com.webonix.webonix_tap_backend.service.*;
import com.webonix.webonix_tap_backend.security.LoginRateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.annotation.PreDestroy;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import java.time.Clock;
import java.util.*;
import java.util.concurrent.*;
@RestController
@RequestMapping("/api/auth")
public class PasswordResetController {
    private final PasswordResetService service;
    private final PasswordResetMailer mailer;
    private final LoginRateLimiter requests = new LoginRateLimiter(Clock.systemUTC(),3,20);
    private final LoginRateLimiter completions = new LoginRateLimiter();
    private final ExecutorService executor = new ThreadPoolExecutor(1,2,60,TimeUnit.SECONDS,new ArrayBlockingQueue<>(50),
            task -> { Thread thread=new Thread(task,"password-reset-mail"); thread.setDaemon(true); return thread; });
    public PasswordResetController(PasswordResetService service, PasswordResetMailer mailer) { this.service=service; this.mailer=mailer; }
    public record ForgotRequest(String email) {}
    public record ResetRequest(String token, String password) {}
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgot(@RequestBody ForgotRequest body, HttpServletRequest request) {
        String email=body.email()==null ? "" : body.email().trim().toLowerCase(Locale.ROOT);
        if(email.length()>150 || !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"))
            return ResponseEntity.badRequest().body(Map.of("message","Geçerli bir e-posta adresi girin."));
        long wait=requests.retryAfter(request.getRemoteAddr(),email);
        if(wait>0) return limited(wait);
        if(!mailer.available()) return ResponseEntity.status(503).body(Map.of("message","Şifre sıfırlama e-postası henüz kullanılamıyor. Lütfen yöneticinizle iletişime geçin."));
        try {
            executor.execute(() -> {
                try { service.request(email); }
                catch(Exception failure) { org.slf4j.LoggerFactory.getLogger(getClass()).warn("Password reset delivery failed; check SMTP/database configuration. No credentials logged."); }
            });
        } catch(RejectedExecutionException busy) { return limited(60); }
        return ResponseEntity.accepted().body(Map.of("message","Bu e-posta ile kayıtlı aktif bir hesabınız varsa sıfırlama bağlantısı gönderilecektir. Gelen kutunuzu ve spam klasörünü kontrol edin."));
    }
    @PostMapping("/reset-password")
    public ResponseEntity<?> reset(@RequestBody ResetRequest body, HttpServletRequest request) {
        long wait=completions.retryAfter(request.getRemoteAddr(),request.getRemoteAddr());
        if(wait>0) return limited(wait);
        service.reset(body.token(),body.password());
        return ResponseEntity.ok(Map.of("message","Şifreniz yenilendi. Tüm oturumlar kapatıldı; yeni şifrenizle giriş yapabilirsiniz."));
    }
    private ResponseEntity<?> limited(long seconds) {
        return ResponseEntity.status(429).header("Retry-After",Long.toString(seconds)).body(Map.of("message","Çok fazla deneme. Lütfen daha sonra tekrar deneyin."));
    }
    @PreDestroy void stop() { executor.shutdownNow(); }
}
