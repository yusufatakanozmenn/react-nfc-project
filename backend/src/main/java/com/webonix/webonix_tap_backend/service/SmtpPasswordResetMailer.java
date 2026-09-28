package com.webonix.webonix_tap_backend.service;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.mail.javamail.JavaMailSender;
@Service
public class SmtpPasswordResetMailer implements PasswordResetMailer {
    private final ObjectProvider<JavaMailSender> sender;
    private final boolean enabled;
    private final String from;
    private final String senderName;
    public SmtpPasswordResetMailer(ObjectProvider<JavaMailSender> sender,
            @Value("${app.mail.enabled:false}") boolean enabled, @Value("${app.mail.from:}") String from,
            @Value("${app.mail.sender-name:Webonix Tap}") String senderName) {
        this.sender = sender; this.enabled = enabled; this.from = from; this.senderName = senderName;
        if (enabled && (sender.getIfAvailable() == null || !from.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")))
            throw new IllegalStateException("Password reset mail requires SMTP configuration and a valid sender.");
    }
    public boolean available() { return enabled; }
    public void sendLink(String email, String link) {
        if (!enabled) throw new IllegalStateException("Password reset mail is disabled");
        try {
            var message = sender.getObject().createMimeMessage();
            var helper = new MimeMessageHelper(message, "UTF-8");
            helper.setFrom(from, senderName); helper.setTo(email); helper.setReplyTo(from);
            helper.setSubject("Webonix Tap — Şifre sıfırlama");
            helper.setText("Şifrenizi yenilemek için aşağıdaki bağlantıyı açın. Bağlantı 15 dakika geçerlidir ve yalnızca bir kez kullanılabilir.\n\n"
                    + link + "\n\nBu isteği siz yapmadıysanız bu e-postayı yok sayabilirsiniz. Şifreniz değişmedi.");
            sender.getObject().send(message);
        } catch (jakarta.mail.MessagingException | java.io.UnsupportedEncodingException exception) {
            throw new IllegalStateException("Password reset message could not be constructed");
        }
    }
}
