package africa.prodesign.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    // @Async is essential here, not cosmetic: without it, a slow or misconfigured
    // SMTP server (e.g. the placeholder Gmail credentials in application.yml)
    // blocks the entire register()/forgot-password() HTTP request until the
    // send attempt times out. The 3s timeouts in application.yml bound how long
    // that can take even on this background thread, but the point of @Async is
    // that the person doesn't wait on it at all.
    @Async
    public void sendVerificationEmail(String to, String token) {
        send(to, "Verify your Prodesign Africa account",
                "Welcome to Prodesign Africa. Verify your account using this token: " + token);
    }

    @Async
    public void sendPasswordResetEmail(String to, String token) {
        send(to, "Reset your Prodesign Africa password",
                "Use this token to reset your password: " + token + " (expires in 1 hour).");
    }

    private void send(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
        } catch (Exception e) {
            // In dev, SMTP is frequently unconfigured; log instead of failing the request flow.
            log.warn("Could not send email to {}: {}", to, e.getMessage());
        }
    }
}
