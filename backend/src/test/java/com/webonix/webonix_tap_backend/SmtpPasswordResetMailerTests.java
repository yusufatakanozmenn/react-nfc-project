package com.webonix.webonix_tap_backend;
import com.webonix.webonix_tap_backend.service.SmtpPasswordResetMailer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.javamail.*;
import jakarta.mail.internet.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SmtpPasswordResetMailerTests {
    @SuppressWarnings("unchecked")
    @Test void mailUsesUtf8SenderAndTrustedLinkWithoutPassword() throws Exception {
        var sender=mock(JavaMailSender.class);
        ObjectProvider<JavaMailSender> provider=mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(sender); when(provider.getObject()).thenReturn(sender);
        var message=new JavaMailSenderImpl().createMimeMessage(); when(sender.createMimeMessage()).thenReturn(message);
        var mailer=new SmtpPasswordResetMailer(provider,true,"sender@example.test","Webonix Yazılım ve Ajans Hizmetleri");
        assertTrue(mailer.available());
        mailer.sendLink("customer@example.test","https://panel.example.test/reset-password#token=test-only");
        verify(sender).send(message);
        assertEquals("sender@example.test",((InternetAddress)message.getFrom()[0]).getAddress());
        assertEquals("Webonix Yazılım ve Ajans Hizmetleri",((InternetAddress)message.getFrom()[0]).getPersonal());
        assertEquals("customer@example.test",((InternetAddress)message.getAllRecipients()[0]).getAddress());
        assertEquals("Webonix Tap — Şifre sıfırlama",message.getSubject());
        assertTrue(message.getContent().toString().contains("15 dakika"));
        assertTrue(message.getContent().toString().contains("https://panel.example.test/reset-password#token=test-only"));
    }
    @SuppressWarnings("unchecked")
    @Test void disabledMailNeverSendsAndEnabledMailRequiresSender() {
        ObjectProvider<JavaMailSender> provider=mock(ObjectProvider.class);
        var disabled=new SmtpPasswordResetMailer(provider,false,"","Webonix");
        assertFalse(disabled.available()); assertThrows(IllegalStateException.class,()->disabled.sendLink("test@example.test","unused"));
        verifyNoInteractions(provider);
        assertThrows(IllegalStateException.class,()->new SmtpPasswordResetMailer(provider,true,"sender@example.test","Webonix"));
    }
}
