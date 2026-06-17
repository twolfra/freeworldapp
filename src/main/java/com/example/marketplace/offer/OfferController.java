package com.example.marketplace.offer;

import com.example.marketplace.auth.SecurityContext;
import com.example.marketplace.image.StorageService;
import com.example.marketplace.like.Like;
import com.example.marketplace.like.LikeRepository;
import com.example.marketplace.offer.dto.OfferDtos;
import com.example.marketplace.user.UserRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/offers")
public class OfferController {

    private final OfferRepository offerRepo;
    private final UserRepository userRepo;
    private final StorageService storage;
    private final LikeRepository likeRepo;

    public OfferController(OfferRepository offerRepo, UserRepository userRepo, StorageService storage, LikeRepository likeRepo) {
        this.offerRepo = offerRepo;
        this.userRepo = userRepo;
        this.storage = storage;
        this.likeRepo = likeRepo;
    }

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody OfferDtos.Create in) {
        UUID userId = SecurityContext.authenticatedId();
        return userRepo.findById(userId)
                .map(user -> {
                    Offer o = new Offer();
                    o.setTitle(in.title);
                    o.setDescription(in.description);
                    o.setRegion(in.region);
                    o.setCategory(in.category);
                    o.setQuantity(in.quantity);
                    o.setImageUrl(in.imageUrl);
                    o.setOfferedBy(user);
                    Offer saved = offerRepo.save(o);
                    return ResponseEntity
                            .created(URI.create("/api/offers/" + saved.getId()))
                            .body((Object) toResponse(saved));
                })
                .orElse(ResponseEntity.badRequest().body(Map.of("error", "User not found.")));
    }

    @GetMapping
    public ResponseEntity<?> list(@RequestParam(required = false) String offeredBy) {
        if (offeredBy != null) {
            UUID uid;
            try { uid = UUID.fromString(offeredBy); }
            catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid user id."));
            }
            return ResponseEntity.ok(offerRepo.findByOfferedBy_Id(uid).stream().map(this::toResponse).toList());
        }
        return ResponseEntity.ok(offerRepo.findAll().stream()
                .filter(o -> !o.getOfferedBy().isBlocked())
                .map(this::toResponse).toList());
    }

    @GetMapping("{id}")
    public ResponseEntity<OfferDtos.Response> get(@PathVariable UUID id) {
        return offerRepo.findById(id)
                .map(o -> ResponseEntity.ok(toResponse(o)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("{id}")
    public ResponseEntity<?> update(@PathVariable UUID id, @Valid @RequestBody OfferDtos.Update in) {
        UUID callerId = SecurityContext.authenticatedId();
        return offerRepo.findById(id)
                .map(o -> {
                    if (!o.getOfferedBy().getId().equals(callerId))
                        return ResponseEntity.status(403).body((Object) Map.of("error", "Not your offer."));
                    String oldImage = o.getImageUrl();
                    o.setTitle(in.title);
                    o.setDescription(in.description);
                    o.setRegion(in.region);
                    o.setCategory(in.category);
                    o.setQuantity(in.quantity);
                    o.setImageUrl(in.imageUrl);
                    Offer saved = offerRepo.save(o);
                    // Delete the old image only if it was replaced or removed
                    if (oldImage != null && !oldImage.equals(in.imageUrl)) storage.delete(oldImage);
                    return ResponseEntity.ok((Object) toResponse(saved));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("{id}")
    public ResponseEntity<?> delete(@PathVariable UUID id) {
        UUID callerId = SecurityContext.authenticatedId();
        return offerRepo.findById(id)
                .map(o -> {
                    if (!o.getOfferedBy().getId().equals(callerId))
                        return ResponseEntity.status(403).<Void>build();
                    String imageUrl = o.getImageUrl();
                    likeRepo.deleteAllByTargetTypeAndTargetId(Like.TargetType.OFFER, id);
                    offerRepo.delete(o);
                    storage.delete(imageUrl);
                    return ResponseEntity.noContent().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
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
        out.offeredByUsername = o.getOfferedBy().getUsername();
        out.imageUrl = o.getImageUrl();
        out.createdAt = DateTimeFormatter.ISO_INSTANT.format(o.getCreatedAt());
        return out;
    }
}
