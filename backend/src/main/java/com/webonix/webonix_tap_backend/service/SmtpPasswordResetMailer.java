package com.webonix.webonix_tap_backend.service;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
@Service
public class SmtpPasswordResetMailer implements PasswordResetMailer {
    private final ObjectProvider<JavaMailSender> sender;
    private final boolean enabled;
    private final String from;
    public SmtpPasswordResetMailer(ObjectProvider<JavaMailSender> sender,
            @Value("${app.mail.enabled:false}") boolean enabled, @Value("${app.mail.from:}") String from) {
        this.sender = sender; this.enabled = enabled; this.from = from;
        if (enabled && (sender.getIfAvailable() == null || !from.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")))
            throw new IllegalStateException("Password reset mail requires SMTP configuration and a valid sender.");
    }
    public boolean available() { return enabled; }
    public void sendLink(String email, String link) {
        if (!enabled) throw new IllegalStateException("Password reset mail is disabled");
        var message = new SimpleMailMessage();
        message.setFrom(from); message.setTo(email); message.setSubject("Webonix Tap — Şifre sıfırlama");
        message.setText("Şifrenizi yenilemek için aşağıdaki bağlantıyı açın. Bağlantı 15 dakika geçerlidir ve yalnızca bir kez kullanılabilir.\n\n"
                + link + "\n\nBu isteği siz yapmadıysanız bu e-postayı yok sayabilirsiniz. Şifreniz değişmedi.");
        sender.getObject().send(message);
    }
}
