package com.example.marketplace.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    @Value("${app.mail.from:noreply@freeworldapp.local}")
    private String from;

    @Value("${app.base-url:http://localhost:5173}")
    private String baseUrl;

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendVerificationEmail(String toEmail, String token) {
        String verifyUrl = baseUrl + "/verify-email?token=" + token;
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(from);
            msg.setTo(toEmail);
            msg.setSubject("Verify your FreeWorld account");
            msg.setText(
                "Welcome to FreeWorld!\n\n" +
                "Please verify your email address by visiting:\n\n" +
                verifyUrl + "\n\n" +
                "This link expires in 24 hours.\n\n" +
                "If you did not create this account, you can ignore this email."
            );
            mailSender.send(msg);
        } catch (Exception e) {
            log.warn("================================================");
            log.warn("EMAIL VERIFICATION — SMTP not available in dev");
            log.warn("To verify {}, open:", toEmail);
            log.warn("{}", verifyUrl);
            log.warn("================================================");
        }
    }
}
