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

    public void sendNewReportEmail(String toEmail, String reporterUsername,
                                   String targetType, String targetTitle, String reason) {
        String panelUrl = baseUrl + "/admin";

        if (brevoApiKey == null || brevoApiKey.isEmpty()) {
            log.info("New report (no Brevo) — {} reported {} '{}' for {}", reporterUsername, targetType, targetTitle, reason);
            return;
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("api-key", brevoApiKey);

            Map<String, Object> body = Map.of(
                "sender", Map.of("name", "FreeWorld", "email", from),
                "to", List.of(Map.of("email", toEmail)),
                "subject", "New report: " + targetType.toLowerCase() + " flagged as " + reason.toLowerCase(),
                "textContent",
                    "A new report has been filed.\n\n" +
                    "Type:     " + targetType + "\n" +
                    "Item:     " + targetTitle + "\n" +
                    "Reason:   " + reason + "\n" +
                    "Reporter: " + reporterUsername + "\n\n" +
                    "Review it in the admin panel:\n" + panelUrl
            );

            restTemplate.postForEntity(
                "https://api.brevo.com/v3/smtp/email",
                new HttpEntity<>(body, headers),
                String.class
            );
            log.info("Report notification sent to {} for {} '{}'", toEmail, targetType, targetTitle);
        } catch (Exception e) {
            log.error("Failed to send report notification to {}: {}", toEmail, e.getMessage(), e);
        }
    }

    public void sendPostRemovedEmail(String toEmail, String username, String postTitle, String postType) {
        if (brevoApiKey == null || brevoApiKey.isEmpty()) {
            log.info("Post removed (no Brevo) — {} <{}> post '{}' ({})", username, toEmail, postTitle, postType);
            return;
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("api-key", brevoApiKey);

            Map<String, Object> body = Map.of(
                "sender", Map.of("name", "FreeWorld", "email", from),
                "to", List.of(Map.of("email", toEmail)),
                "subject", "Your FreeWorld post was removed",
                "textContent",
                    "Hi " + username + ",\n\n" +
                    "Your " + postType + " post \"" + postTitle + "\" has been removed by a moderator " +
                    "because it was reported and found to violate our community guidelines.\n\n" +
                    "If you believe this was a mistake, please reply to this email or contact us at info@freeworldapp.de.\n\n" +
                    "The FreeWorld Team"
            );

            restTemplate.postForEntity(
                "https://api.brevo.com/v3/smtp/email",
                new HttpEntity<>(body, headers),
                String.class
            );
            log.info("Post-removed email sent to {} for post '{}'", toEmail, postTitle);
        } catch (Exception e) {
            log.error("Failed to send post-removed email to {}: {}", toEmail, e.getMessage(), e);
        }
    }

    /**
     * Notify a recipient by email that they received a direct message.
     * Includes a truncated preview, a deep link to the conversation, and a
     * login-free unsubscribe link (mirrored in the List-Unsubscribe header).
     */
    public void sendNewMessageEmail(String toEmail, String recipientUsername, String senderUsername,
                                    String content, String senderId, String unsubscribeToken, String language) {
        boolean de = "de".equalsIgnoreCase(language);

        String preview = content == null ? "" : content.strip();
        if (preview.length() > 150) preview = preview.substring(0, 150).strip() + "…";

        String conversationUrl = baseUrl + "/messages/" + senderId;
        String unsubscribeUrl  = baseUrl + "/api/notifications/unsubscribe?token=" + unsubscribeToken;

        String subject = de
                ? "Neue Nachricht von " + senderUsername + " auf FreeWorld"
                : "New message from " + senderUsername + " on FreeWorld";

        String text = de
                ? "Hallo " + recipientUsername + ",\n\n" +
                  senderUsername + " hat dir eine Nachricht auf FreeWorld geschickt:\n\n" +
                  "\"" + preview + "\"\n\n" +
                  "Lesen und antworten:\n" + conversationUrl + "\n\n" +
                  "—\nDu möchtest keine Benachrichtigungen über Nachrichten mehr erhalten?\n" +
                  "Hier deaktivieren:\n" + unsubscribeUrl
                : "Hi " + recipientUsername + ",\n\n" +
                  senderUsername + " sent you a message on FreeWorld:\n\n" +
                  "\"" + preview + "\"\n\n" +
                  "Read and reply:\n" + conversationUrl + "\n\n" +
                  "—\nDon't want notifications about new messages?\n" +
                  "Turn them off here:\n" + unsubscribeUrl;

        if (brevoApiKey == null || brevoApiKey.isEmpty()) {
            log.info("New message email (no Brevo) — to {} <{}> from {}: {}", recipientUsername, toEmail, senderUsername, preview);
            return;
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("api-key", brevoApiKey);

            Map<String, Object> body = Map.of(
                "sender", Map.of("name", "FreeWorld", "email", from),
                "to", List.of(Map.of("email", toEmail)),
                "subject", subject,
                "textContent", text,
                // RFC 8058 / 2369 — lets Gmail, Apple Mail etc. show a native unsubscribe button.
                "headers", Map.of(
                    "List-Unsubscribe", "<" + unsubscribeUrl + ">",
                    "List-Unsubscribe-Post", "List-Unsubscribe=One-Click"
                )
            );

            restTemplate.postForEntity(
                "https://api.brevo.com/v3/smtp/email",
                new HttpEntity<>(body, headers),
                String.class
            );
            log.info("New-message email sent to {} (from {})", toEmail, senderUsername);
        } catch (Exception e) {
            log.error("Failed to send new-message email to {}: {}", toEmail, e.getMessage(), e);
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
