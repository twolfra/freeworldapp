package de.freeworldapp.app.user;

import de.freeworldapp.app.auth.SecurityContext;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * Message-notification preferences.
 *
 * The unsubscribe endpoint is intentionally login-free (token-based) so it works
 * straight from an email link and from mail-client one-click unsubscribe
 * (List-Unsubscribe-Post). The preferences endpoint requires a session.
 */
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final UserRepository userRepo;

    public NotificationController(UserRepository userRepo) {
        this.userRepo = userRepo;
    }

    /** Browser click from the email link — returns a small confirmation page. */
    @GetMapping(value = "/unsubscribe", produces = MediaType.TEXT_HTML_VALUE)
    @Transactional
    public ResponseEntity<String> unsubscribePage(@RequestParam String token) {
        User u = disableNotifications(token);
        boolean de = u != null && "de".equalsIgnoreCase(u.getLanguage());
        String msg = u == null
                ? (de ? "Ungültiger Link." : "Invalid link.")
                : (de ? "Du erhältst keine E-Mail-Benachrichtigungen über neue Nachrichten mehr. "
                      + "Du kannst sie jederzeit in deinem Profil wieder aktivieren."
                      : "You will no longer receive email notifications about new messages. "
                      + "You can turn them back on anytime in your profile.");
        String title = de ? "Benachrichtigungen deaktiviert" : "Notifications turned off";
        String html = "<!doctype html><html><head><meta charset=\"utf-8\">"
                + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">"
                + "<title>FreeWorld</title></head>"
                + "<body style=\"font-family:system-ui,sans-serif;max-width:520px;margin:60px auto;padding:0 20px;color:#1a1a1a\">"
                + "<h1 style=\"font-size:1.4rem\">" + title + "</h1>"
                + "<p style=\"line-height:1.5\">" + msg + "</p>"
                + "<p><a href=\"/\" style=\"color:#0a7d4f\">← FreeWorld</a></p>"
                + "</body></html>";
        return ResponseEntity.ok(html);
    }

    /** Mail-client one-click unsubscribe (RFC 8058). */
    @PostMapping("/unsubscribe")
    @Transactional
    public ResponseEntity<Void> unsubscribeOneClick(@RequestParam String token) {
        disableNotifications(token);
        return ResponseEntity.ok().build();
    }

    /** Authenticated toggle from the in-app settings UI. */
    @PutMapping("/preferences")
    @Transactional
    public ResponseEntity<?> updatePreferences(@RequestBody Map<String, Object> body) {
        UUID callerId = SecurityContext.authenticatedId();
        return userRepo.findById(callerId).map(u -> {
            Object notify = body.get("notifyOnMessage");
            if (notify instanceof Boolean b) u.setNotifyOnMessage(b);
            Object lang = body.get("language");
            if (lang instanceof String s && !s.isBlank()) u.setLanguage(s);
            userRepo.save(u);
            return ResponseEntity.ok(Map.of(
                    "notifyOnMessage", u.isNotifyOnMessage(),
                    "language", u.getLanguage()));
        }).orElse(ResponseEntity.notFound().build());
    }

    private User disableNotifications(String token) {
        if (token == null || token.isBlank()) return null;
        User u = userRepo.findByUnsubscribeToken(token).orElse(null);
        if (u != null && u.isNotifyOnMessage()) {
            u.setNotifyOnMessage(false);
            userRepo.save(u);
        }
        return u;
    }
}
