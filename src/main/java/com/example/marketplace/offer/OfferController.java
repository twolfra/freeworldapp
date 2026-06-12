package com.example.marketplace.offer;

import com.example.marketplace.offer.dto.OfferDtos;
import com.example.marketplace.user.UserRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/offers")
public class OfferController {

    private final OfferRepository offerRepo;
    private final UserRepository userRepo;

    public OfferController(OfferRepository offerRepo, UserRepository userRepo) {
        this.offerRepo = offerRepo;
        this.userRepo = userRepo;
    }

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody OfferDtos.Create in) {
        UUID userId;
        try {
            userId = UUID.fromString(in.offeredById);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", "Invalid user id."));
        }

        return userRepo.findById(userId)
                .map(user -> {
                    Offer o = new Offer();
                    o.setTitle(in.title);
                    o.setDescription(in.description);
                    o.setRegion(in.region);
                    o.setCategory(in.category);
                    o.setQuantity(in.quantity);
                    o.setOfferedBy(user);
                    Offer saved = offerRepo.save(o);
                    return ResponseEntity
                            .created(URI.create("/api/offers/" + saved.getId()))
                            .body((Object) toResponse(saved));
                })
                .orElse(ResponseEntity.badRequest().body(java.util.Map.of("error", "User not found.")));
    }

    @GetMapping
    public List<OfferDtos.Response> list() {
        return offerRepo.findAll().stream().map(this::toResponse).toList();
    }

    @GetMapping("{id}")
    public ResponseEntity<OfferDtos.Response> get(@PathVariable UUID id) {
        return offerRepo.findById(id)
                .map(o -> ResponseEntity.ok(toResponse(o)))
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        if (!offerRepo.existsById(id)) return ResponseEntity.notFound().build();
        offerRepo.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private OfferDtos.Response toResponse(Offer o) {
        var out = new OfferDtos.Response();
        out.id = o.getId().toString();
        out.title = o.getTitle();
        out.description = o.getDescription();
        out.region = o.getRegion();
        out.category = o.getCategory();
        out.quantity = o.getQuantity();
        out.offeredById = o.getOfferedBy().getId().toString();
        out.createdAt = DateTimeFormatter.ISO_INSTANT.format(o.getCreatedAt());
        return out;
    }
}
