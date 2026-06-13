package com.example.marketplace.user;

import com.example.marketplace.auth.SecurityContext;
import com.example.marketplace.user.dto.UserDtos;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository userRepo;
    private final PasswordEncoder encoder;

    public UserController(UserRepository userRepo, PasswordEncoder encoder) {
        this.userRepo = userRepo;
        this.encoder = encoder;
    }

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody UserDtos.Create in) {
        if (userRepo.existsByUsername(in.username))
            return ResponseEntity.badRequest().body(Map.of("error", "Username already taken."));
        if (userRepo.existsByEmail(in.email))
            return ResponseEntity.badRequest().body(Map.of("error", "Email already registered."));

        User u = new User();
        u.setUsername(in.username);
        u.setEmail(in.email);
        u.setPasswordHash(encoder.encode(in.password));
        u = userRepo.save(u);

        return ResponseEntity.created(URI.create("/api/users/" + u.getId()))
                .body(toPublicResponse(u));
    }

    @GetMapping
    public List<UserDtos.PublicResponse> list() {
        return userRepo.findAll().stream().map(this::toPublicResponse).toList();
    }

    @GetMapping("{id}")
    public ResponseEntity<UserDtos.PublicResponse> get(@PathVariable UUID id) {
        return userRepo.findById(id).map(u -> ResponseEntity.ok(toPublicResponse(u)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("{id}")
    public ResponseEntity<?> update(@PathVariable UUID id, @Valid @RequestBody UserDtos.Update in) {
        UUID callerId = SecurityContext.authenticatedId();
        if (!callerId.equals(id))
            return ResponseEntity.status(403).body(Map.of("error", "You can only update your own account."));

        return userRepo.findById(id).map(u -> {
            u.setUsername(in.username);
            u.setEmail(in.email);
            return ResponseEntity.ok(toPublicResponse(userRepo.save(u)));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("{id}")
    public ResponseEntity<?> delete(@PathVariable UUID id) {
        UUID callerId = SecurityContext.authenticatedId();
        if (!callerId.equals(id))
            return ResponseEntity.status(403).body(Map.of("error", "You can only delete your own account."));

        if (!userRepo.existsById(id)) return ResponseEntity.notFound().build();
        userRepo.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private UserDtos.PublicResponse toPublicResponse(User u) {
        var out = new UserDtos.PublicResponse();
        out.id = u.getId().toString();
        out.username = u.getUsername();
        out.createdAt = DateTimeFormatter.ISO_INSTANT.format(u.getCreatedAt());
        return out;
    }
}
