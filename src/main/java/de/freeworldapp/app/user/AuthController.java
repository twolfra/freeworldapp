package de.freeworldapp.app.user;

import de.freeworldapp.app.auth.SecurityContext;
import de.freeworldapp.app.auth.Session;
import de.freeworldapp.app.auth.SessionRepository;
import de.freeworldapp.app.auth.Tokens;
import de.freeworldapp.app.email.EmailService;
import de.freeworldapp.app.user.dto.UserDtos;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepo;
    private final PasswordEncoder encoder;
    private final SessionRepository sessionRepo;
    private final EmailService emailService;

    public AuthController(UserRepository userRepo, PasswordEncoder encoder,
                          SessionRepository sessionRepo, EmailService emailService) {
        this.userRepo = userRepo;
        this.encoder = encoder;
        this.sessionRepo = sessionRepo;
        this.emailService = emailService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody UserDtos.Login in) {
        var userOpt = userRepo.findByUsername(in.username)
                .filter(u -> encoder.matches(in.password, u.getPasswordHash()));
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(401).build();
        }
        User u = userOpt.get();
        if (!u.isEmailVerified()) {
            return ResponseEntity.status(403)
                    .body(Map.of("error", "Email not verified. Please check your inbox."));
        }
        if (u.isBlocked()) {
            return ResponseEntity.status(403)
                    .body(Map.of("error", "This account has been blocked. Contact support if you believe this is a mistake."));
        }
        String rawToken = Tokens.generate();
        Session s = new Session();
        s.setTokenHash(Tokens.sha256(rawToken));
        s.setUser(u);
        s.setExpiresAt(Instant.now().plus(30, ChronoUnit.DAYS));
        sessionRepo.save(s);
        UserDtos.Response resp = toResponse(u);
        resp.token = rawToken;
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/verify")
    public ResponseEntity<?> verify(@RequestParam String token) {
        var userOpt = userRepo.findByVerificationToken(token);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("error", "Invalid verification token."));
        }
        User u = userOpt.get();
        if (u.getVerificationTokenExpiresAt() == null
                || u.getVerificationTokenExpiresAt().isBefore(Instant.now())) {
            return ResponseEntity.status(410)
                    .body(Map.of("error", "Verification link has expired. Please request a new one."));
        }
        u.setEmailVerified(true);
        u.setVerificationToken(null);
        u.setVerificationTokenExpiresAt(null);
        userRepo.save(u);
        return ResponseEntity.ok(Map.of("message", "Email verified! You can now log in."));
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<?> resendVerification(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email is required."));
        }
        userRepo.findByEmail(email).ifPresent(u -> {
            if (!u.isEmailVerified()) {
                String token = UUID.randomUUID().toString();
                u.setVerificationToken(token);
                u.setVerificationTokenExpiresAt(Instant.now().plus(24, ChronoUnit.HOURS));
                userRepo.save(u);
                emailService.sendVerificationEmail(u.getEmail(), token);
            }
        });
        return ResponseEntity.ok(Map.of("message",
                "If that email is registered and unverified, a new link has been sent."));
    }

    /** Requires the current password; all OTHER sessions of the user are invalidated. */
    @PostMapping("/change-password")
    @Transactional
    public ResponseEntity<?> changePassword(@Valid @RequestBody UserDtos.ChangePassword in,
                                            HttpServletRequest request) {
        UUID callerId = SecurityContext.authenticatedId();
        User u = userRepo.findById(callerId).orElse(null);
        if (u == null) return ResponseEntity.status(401).build();

        if (!encoder.matches(in.oldPassword, u.getPasswordHash())) {
            return ResponseEntity.status(403)
                    .body(Map.of("error", "Current password is incorrect."));
        }

        u.setPasswordHash(encoder.encode(in.newPassword));
        userRepo.save(u);

        String currentToken = request.getHeader("X-Session-Token");
        sessionRepo.deleteByUser_IdAndTokenHashNot(callerId, Tokens.sha256(currentToken));

        return ResponseEntity.ok(Map.of("message", "Password changed."));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        String token = request.getHeader("X-Session-Token");
        if (token != null) {
            sessionRepo.findByRawToken(token).ifPresent(sessionRepo::delete);
        }
        return ResponseEntity.noContent().build();
    }

    private UserDtos.Response toResponse(User u) {
        var out = new UserDtos.Response();
        out.id = u.getId().toString();
        out.username = u.getUsername();
        out.email = u.getEmail();
        out.createdAt = DateTimeFormatter.ISO_INSTANT.format(u.getCreatedAt());
        out.role = u.getRole().name();
        out.notifyOnMessage = u.isNotifyOnMessage();
        out.language = u.getLanguage();
        return out;
    }
}
