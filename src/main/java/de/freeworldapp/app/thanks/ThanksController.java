package de.freeworldapp.app.thanks;

import de.freeworldapp.app.auth.SecurityContext;
import de.freeworldapp.app.message.MessageRepository;
import de.freeworldapp.app.notification.Notification;
import de.freeworldapp.app.notification.NotificationService;
import de.freeworldapp.app.offer.Offer;
import de.freeworldapp.app.offer.OfferRepository;
import de.freeworldapp.app.user.UserRepository;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
public class ThanksController {

    private final ThanksRepository thanksRepo;
    private final OfferRepository offerRepo;
    private final UserRepository userRepo;
    private final MessageRepository messageRepo;
    private final NotificationService notificationCenter;

    public ThanksController(ThanksRepository thanksRepo, OfferRepository offerRepo,
                            UserRepository userRepo, MessageRepository messageRepo,
                            NotificationService notificationCenter) {
        this.thanksRepo = thanksRepo;
        this.offerRepo = offerRepo;
        this.userRepo = userRepo;
        this.messageRepo = messageRepo;
        this.notificationCenter = notificationCenter;
    }

    public static class Create {
        @Size(max = 280)
        public String text;
    }

    /**
     * The receiving person leaves exactly one thanks per completed gift.
     * Guards: offer must be GIVEN, not your own offer, only once, and there
     * must be an actual conversation between giver and thanker (abuse guard).
     */
    @PostMapping("/api/offers/{id}/thanks")
    public ResponseEntity<?> create(@PathVariable UUID id, @RequestBody(required = false) Create in) {
        UUID callerId = SecurityContext.authenticatedId();
        Offer offer = offerRepo.findById(id).orElse(null);
        if (offer == null) return ResponseEntity.notFound().build();

        UUID ownerId = offer.getOfferedBy().getId();
        if (ownerId.equals(callerId))
            return ResponseEntity.badRequest().body(Map.of("error", "You cannot thank yourself."));
        if (offer.getStatus() != Offer.Status.GIVEN)
            return ResponseEntity.badRequest().body(Map.of("error", "This offer has not been given away yet."));
        if (thanksRepo.existsByOfferId(id))
            return ResponseEntity.status(409).body(Map.of("error", "A thanks for this gift already exists."));
        if (messageRepo.findConversation(callerId, ownerId).isEmpty())
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "You can only thank someone you actually talked to about this gift."));

        Thanks t = new Thanks();
        t.setFromUser(userRepo.findById(callerId).orElseThrow());
        t.setToUser(offer.getOfferedBy());
        t.setOfferId(id);
        t.setOfferTitle(offer.getTitle());
        t.setText(in != null && in.text != null && !in.text.isBlank() ? in.text.strip() : null);
        Thanks saved = thanksRepo.save(t);
        notificationCenter.notify(ownerId, Notification.Type.THANKS, Map.of(
                "offerId", id.toString(),
                "offerTitle", offer.getTitle(),
                "fromUsername", saved.getFromUser().getUsername()));
        return ResponseEntity.ok(toResponse(saved));
    }

    /** Public: the thanks shown on a profile (qualitative list, no score). */
    @GetMapping("/api/users/{id}/thanks")
    public ResponseEntity<?> forUser(@PathVariable UUID id) {
        if (!userRepo.existsById(id)) return ResponseEntity.notFound().build();
        List<Map<String, Object>> out = thanksRepo.findByToUser_IdOrderByCreatedAtDesc(id)
                .stream().map(this::toResponse).toList();
        return ResponseEntity.ok(out);
    }

    private Map<String, Object> toResponse(Thanks t) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", t.getId().toString());
        map.put("fromUserId", t.getFromUser().getId().toString());
        map.put("fromUsername", t.getFromUser().getUsername());
        map.put("offerId", t.getOfferId().toString());
        map.put("offerTitle", t.getOfferTitle());
        map.put("text", t.getText());
        map.put("createdAt", DateTimeFormatter.ISO_INSTANT.format(t.getCreatedAt()));
        return map;
    }
}
