package com.example.marketplace.user;

import com.example.marketplace.auth.Session;
import com.example.marketplace.auth.SessionRepository;
import com.example.marketplace.user.dto.UserDtos;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepo;
    private final PasswordEncoder encoder;
    private final SessionRepository sessionRepo;

    public AuthController(UserRepository userRepo, PasswordEncoder encoder, SessionRepository sessionRepo) {
        this.userRepo = userRepo;
        this.encoder = encoder;
        this.sessionRepo = sessionRepo;
    }

    @PostMapping("/login")
    public ResponseEntity<UserDtos.Response> login(@Valid @RequestBody UserDtos.Login in) {
        return userRepo.findByUsername(in.username)
                .filter(u -> encoder.matches(in.password, u.getPasswordHash()))
                .map(u -> {
                    Session s = new Session();
                    s.setToken(UUID.randomUUID().toString());
                    s.setUser(u);
                    s.setExpiresAt(Instant.now().plus(30, ChronoUnit.DAYS));
                    sessionRepo.save(s);
                    UserDtos.Response resp = toResponse(u);
                    resp.token = s.getToken();
                    return ResponseEntity.ok(resp);
                })
                .orElse(ResponseEntity.status(401).build());
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        String token = request.getHeader("X-Session-Token");
        if (token != null) {
            sessionRepo.findByToken(token).ifPresent(sessionRepo::delete);
        }
        return ResponseEntity.noContent().build();
    }

    private UserDtos.Response toResponse(User u) {
        var out = new UserDtos.Response();
        out.id = u.getId().toString();
        out.username = u.getUsername();
        out.email = u.getEmail();
        out.createdAt = DateTimeFormatter.ISO_INSTANT.format(u.getCreatedAt());
        return out;
    }
}
