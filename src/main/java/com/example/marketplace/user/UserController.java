package com.example.marketplace.user;

import com.example.marketplace.user.dto.UserDtos;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.format.DateTimeFormatter;
import java.util.List;
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
    // create new User
    @PostMapping
    public ResponseEntity<UserDtos.Response> create(@Valid @RequestBody UserDtos.Create in) {
        if (userRepo.existsByUsername(in.username)) return ResponseEntity.badRequest().build();
        if (userRepo.existsByEmail(in.email)) return ResponseEntity.badRequest().build();

        User u = new User();
        u.setUsername(in.username);
        u.setEmail(in.email);
        u.setPasswordHash(encoder.encode(in.password));
        u = userRepo.save(u);

        return ResponseEntity.created(URI.create("/api/users/" + u.getId()))
                .body(toResponse(u));
    }

    @GetMapping
    public List<UserDtos.Response> list() {
        return userRepo.findAll().stream().map(this::toResponse).toList();
    }

    @GetMapping("{id}")
    public ResponseEntity<UserDtos.Response> get(@PathVariable UUID id) {
        return userRepo.findById(id).map(u -> ResponseEntity.ok(toResponse(u)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("{id}")
    public ResponseEntity<UserDtos.Response> update(@PathVariable UUID id, @Valid @RequestBody UserDtos.Update in) {
        return userRepo.findById(id).map(u -> {
            u.setUsername(in.username);
            u.setEmail(in.email);
            return ResponseEntity.ok(toResponse(userRepo.save(u)));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        if (!userRepo.existsById(id)) return ResponseEntity.notFound().build();
        userRepo.deleteById(id);
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
