package de.freeworldapp.app.user;

import de.freeworldapp.app.auth.LoginAttemptService;
import de.freeworldapp.app.auth.PasswordResetToken;
import de.freeworldapp.app.auth.PasswordResetTokenRepository;
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
    private final PasswordResetTokenRepository resetRepo;
    private final LoginAttemptService loginAttempts;

    public AuthController(UserRepository userRepo, PasswordEncoder encoder,
                          SessionRepository sessionRepo, EmailService emailService,
                          PasswordResetTokenRepository resetRepo, LoginAttemptService loginAttempts) {
        this.userRepo = userRepo;
        this.encoder = encoder;
        this.sessionRepo = sessionRepo;
        this.emailService = emailService;
        this.resetRepo = resetRepo;
        this.loginAttempts = loginAttempts;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody UserDtos.Login in) {
        if (loginAttempts.isLocked(in.username)) {
            return ResponseEntity.status(429).body(Map.of("error",
                    "Too many failed sign-in attempts. Please try again in a few minutes."));
        }
        var userOpt = userRepo.findByUsername(in.username)
                .filter(u -> encoder.matches(in.password, u.getPasswordHash()));
        if (userOpt.isEmpty()) {
            loginAttempts.onFailure(in.username);
            return ResponseEntity.status(401).build();
        }
        loginAttempts.onSuccess(in.username);
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


    /** Always 200 — never reveals whether the email is registered. */
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        if (email != null && !email.isBlank()) {
            userRepo.findByEmailIgnoreCase(email.trim()).ifPresent(u -> {
                String rawToken = Tokens.generate();
                PasswordResetToken t = new PasswordResetToken();
                t.setUser(u);
                t.setTokenHash(Tokens.sha256(rawToken));
                t.setExpiresAt(Instant.now().plus(1, ChronoUnit.HOURS));
                resetRepo.save(t);
                emailService.sendPasswordResetEmail(u.getEmail(), u.getUsername(), rawToken, u.getLanguage());
            });
        }
        return ResponseEntity.ok(Map.of("message",
                "If that email is registered, a password reset link has been sent."));
    }

    @PostMapping("/reset-password")
    @Transactional
    public ResponseEntity<?> resetPassword(@Valid @RequestBody UserDtos.ResetPassword in) {
        var tokenOpt = resetRepo.findByRawToken(in.token);
        if (tokenOpt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("error", "Invalid reset token."));
        }
        PasswordResetToken t = tokenOpt.get();
        if (t.getUsedAt() != null || t.getExpiresAt().isBefore(Instant.now())) {
            return ResponseEntity.status(410)
                    .body(Map.of("error", "This reset link has expired or was already used."));
        }

        User u = t.getUser();
        u.setPasswordHash(encoder.encode(in.newPassword));
        userRepo.save(u);
        t.setUsedAt(Instant.now());
        resetRepo.save(t);
        // Whoever holds a session is signed out — the password may have leaked.
        sessionRepo.deleteByUser_Id(u.getId());
        emailService.sendPasswordChangedEmail(u.getEmail(), u.getUsername(), u.getLanguage());

        return ResponseEntity.ok(Map.of("message", "Password reset. You can now sign in."));
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
        out.displayName = u.getDisplayName();
        out.bio = u.getBio();
        out.avatarUrl = u.getAvatarUrl();
        out.postalCode = u.getPostalCode();
        out.city = u.getCity();
        return out;
    }
}
