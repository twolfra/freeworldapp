package com.example.marketplace.user;

import com.example.marketplace.user.dto.UserDtos;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepo;
    private final PasswordEncoder encoder;

    public AuthController(UserRepository userRepo, PasswordEncoder encoder) {
        this.userRepo = userRepo;
        this.encoder = encoder;
    }

    @PostMapping("/login")
    public ResponseEntity<UserDtos.Response> login(@Valid @RequestBody UserDtos.Login in) {
        return userRepo.findByUsername(in.username)
                .filter(u -> encoder.matches(in.password, u.getPasswordHash()))
                .map(u -> ResponseEntity.ok(toResponse(u)))
                .orElse(ResponseEntity.status(401).build());
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
