package com.example.marketplace.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    @Value("${app.mail.from:noreply@freeworldapp.local}")
    private String from;

    @Value("${app.base-url:http://localhost:5173}")
    private String baseUrl;

    @Value("${BREVO_API_KEY:}")
    private String brevoApiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    public void sendContactEmail(String senderName, String senderEmail, String message) {
        String adminEmail = "info@freeworldapp.de";

        if (brevoApiKey == null || brevoApiKey.isEmpty()) {
            log.info("Contact form (no Brevo) — from: {} <{}>, message: {}", senderName, senderEmail, message);
            return;
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("api-key", brevoApiKey);

            Map<String, Object> body = Map.of(
                "sender",  Map.of("name", "FreeWorld Contact", "email", from),
                "to",      List.of(Map.of("email", adminEmail)),
                "replyTo", Map.of("name", senderName, "email", senderEmail),
                "subject", "FreeWorld Kontaktanfrage von " + senderName,
                "textContent",
                    "Name:  " + senderName + "\n" +
                    "Email: " + senderEmail + "\n\n" +
                    message
            );

            restTemplate.postForEntity(
                "https://api.brevo.com/v3/smtp/email",
                new HttpEntity<>(body, headers),
                String.class
            );
            log.info("Contact email forwarded from {} <{}>", senderName, senderEmail);
        } catch (Exception e) {
            log.error("Failed to send contact email: {}", e.getMessage(), e);
        }
    }

    public void sendVerificationEmail(String toEmail, String token) {
        String verifyUrl = baseUrl + "/verify-email?token=" + token;

        if (brevoApiKey == null || brevoApiKey.isEmpty()) {
            log.warn("BREVO_API_KEY not set — open this link to verify: {}", verifyUrl);
            return;
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("api-key", brevoApiKey);

            Map<String, Object> body = Map.of(
                "sender", Map.of("name", "FreeWorld", "email", from),
                "to", List.of(Map.of("email", toEmail)),
                "subject", "Verify your FreeWorld account",
                "textContent",
                    "Welcome to FreeWorld!\n\n" +
                    "Please verify your email address by visiting:\n\n" +
                    verifyUrl + "\n\n" +
                    "This link expires in 24 hours.\n\n" +
                    "If you did not create this account, you can ignore this email."
            );

            restTemplate.postForEntity(
                "https://api.brevo.com/v3/smtp/email",
                new HttpEntity<>(body, headers),
                String.class
            );
            log.info("Verification email sent to {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send verification email to {}: {}", toEmail, e.getMessage(), e);
            log.warn("Fallback — open this link to verify: {}", verifyUrl);
        }
    }
}
