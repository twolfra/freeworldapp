package com.example.marketplace.request;

import com.example.marketplace.request.dto.RequestDtos;
import com.example.marketplace.user.UserRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/requests")
public class RequestController {

    private final RequestRepository requestRepo;
    private final UserRepository userRepo;

    public RequestController(RequestRepository requestRepo, UserRepository userRepo) {
        this.requestRepo = requestRepo;
        this.userRepo = userRepo;
    }

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody RequestDtos.Create in) {
        UUID userId;
        try {
            userId = UUID.fromString(in.requestedById);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid user id."));
        }

        return userRepo.findById(userId)
                .map(user -> {
                    Request r = new Request();
                    r.setTitle(in.title);
                    r.setDescription(in.description);
                    r.setRegion(in.region);
                    r.setCategory(in.category);
                    r.setQuantity(in.quantity);
                    r.setRequestedBy(user);
                    Request saved = requestRepo.save(r);
                    return ResponseEntity
                            .created(URI.create("/api/requests/" + saved.getId()))
                            .body((Object) toResponse(saved));
                })
                .orElse(ResponseEntity.badRequest().body(Map.of("error", "User not found.")));
    }

    @GetMapping
    public ResponseEntity<?> list(@RequestParam(required = false) String requestedBy) {
        if (requestedBy != null) {
            UUID uid;
            try { uid = UUID.fromString(requestedBy); }
            catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid user id."));
            }
            return ResponseEntity.ok(requestRepo.findByRequestedBy_Id(uid).stream().map(this::toResponse).toList());
        }
        return ResponseEntity.ok(requestRepo.findAll().stream().map(this::toResponse).toList());
    }

    @GetMapping("{id}")
    public ResponseEntity<RequestDtos.Response> get(@PathVariable UUID id) {
        return requestRepo.findById(id)
                .map(r -> ResponseEntity.ok(toResponse(r)))
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        if (!requestRepo.existsById(id)) return ResponseEntity.notFound().build();
        requestRepo.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private RequestDtos.Response toResponse(Request r) {
        var out = new RequestDtos.Response();
        out.id = r.getId().toString();
        out.title = r.getTitle();
        out.description = r.getDescription();
        out.region = r.getRegion();
        out.category = r.getCategory();
        out.quantity = r.getQuantity();
        out.requestedById = r.getRequestedBy().getId().toString();
        out.requestedByUsername = r.getRequestedBy().getUsername();
        out.createdAt = DateTimeFormatter.ISO_INSTANT.format(r.getCreatedAt());
        return out;
    }
}
