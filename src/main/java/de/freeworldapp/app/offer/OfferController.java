package de.freeworldapp.app.offer;

import de.freeworldapp.app.auth.AdminGuard;
import de.freeworldapp.app.geo.PlzGeoRepository;
import de.freeworldapp.app.auth.SecurityContext;
import de.freeworldapp.app.image.StorageService;
import de.freeworldapp.app.like.Like;
import de.freeworldapp.app.like.LikeRepository;
import de.freeworldapp.app.message.ChatWebSocketHandler;
import de.freeworldapp.app.message.Message;
import de.freeworldapp.app.message.MessageNotificationService;
import de.freeworldapp.app.message.MessageRepository;
import de.freeworldapp.app.notification.Notification;
import de.freeworldapp.app.postimage.PostImage;
import de.freeworldapp.app.postimage.PostImageService;
import de.freeworldapp.app.notification.NotificationService;
import de.freeworldapp.app.subscription.SubscriptionRepository;
import de.freeworldapp.app.offer.dto.OfferDtos;
import de.freeworldapp.app.user.UserRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/offers")
public class OfferController {

    private final OfferRepository offerRepo;
    private final UserRepository userRepo;
    private final StorageService storage;
    private final LikeRepository likeRepo;
    private final AdminGuard adminGuard;
    private final PlzGeoRepository plzRepo;
    private final MessageRepository messageRepo;
    private final ChatWebSocketHandler wsHandler;
    private final MessageNotificationService notificationService;
    private final NotificationService notificationCenter;
    private final SubscriptionRepository subscriptionRepo;
    private final PostImageService postImages;

    public OfferController(OfferRepository offerRepo, UserRepository userRepo, StorageService storage,
                           LikeRepository likeRepo, AdminGuard adminGuard, MessageRepository messageRepo,
                           ChatWebSocketHandler wsHandler, MessageNotificationService notificationService,
                           NotificationService notificationCenter, SubscriptionRepository subscriptionRepo,
                           PostImageService postImages,
                           PlzGeoRepository plzRepo) {
        this.offerRepo = offerRepo;
        this.userRepo = userRepo;
        this.storage = storage;
        this.likeRepo = likeRepo;
        this.adminGuard = adminGuard;
        this.plzRepo = plzRepo;
        this.messageRepo = messageRepo;
        this.wsHandler = wsHandler;
        this.notificationService = notificationService;
        this.notificationCenter = notificationCenter;
        this.subscriptionRepo = subscriptionRepo;
        this.postImages = postImages;
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
                    String geoErr = applyGeo(o, in.postalCode);
                    if (geoErr != null)
                        return ResponseEntity.status(400).body((Object) Map.of("error", geoErr));
                    o.setImageUrl(in.imageUrl);
                    o.setOfferedBy(user);
                    String geoError = applyGeo(o, in.postalCode);
                    if (geoError != null)
                        return ResponseEntity.badRequest().body((Object) Map.of("error", geoError));
                    Offer saved = offerRepo.save(o);
                    subscriptionRepo.findBySubscribedTo_Id(user.getId()).forEach(sub ->
                            notificationCenter.notify(sub.getSubscriber().getId(),
                                    Notification.Type.NEW_POST_FROM_SUB, Map.of(
                                            "postType", "OFFER",
                                            "postId", saved.getId().toString(),
                                            "title", saved.getTitle(),
                                            "username", user.getUsername())));
                    return ResponseEntity
                            .created(URI.create("/api/offers/" + saved.getId()))
                            .body((Object) toResponse(saved));
                })
                .orElse(ResponseEntity.badRequest().body(Map.of("error", "User not found.")));
    }

    @GetMapping
    public ResponseEntity<?> list(@RequestParam(required = false) String offeredBy,
                                  @RequestParam(defaultValue = "false") boolean includeCompleted) {
        if (offeredBy != null) {
            UUID uid;
            try { uid = UUID.fromString(offeredBy); }
            catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid user id."));
            }
            // Per-user views (profile, own management) always show every status.
            return ResponseEntity.ok(offerRepo.findByOfferedBy_Id(uid).stream().map(this::toResponse).toList());
        }
        return ResponseEntity.ok(offerRepo.findAll().stream()
                .filter(o -> !o.getOfferedBy().isBlocked())
                .filter(o -> includeCompleted || o.getStatus() != Offer.Status.GIVEN)
                .map(this::toResponse).toList());
    }

    /**
     * Interest flow: instead of a raw DM, creates a structured first message
     * carrying the offer as context. Idempotent per user+offer.
     */
    @PostMapping("{id}/interest")
    public ResponseEntity<?> expressInterest(@PathVariable UUID id) {
        UUID callerId = SecurityContext.authenticatedId();
        return offerRepo.findById(id)
                .<ResponseEntity<?>>map(o -> {
                    UUID ownerId = o.getOfferedBy().getId();
                    if (ownerId.equals(callerId))
                        return ResponseEntity.badRequest().body(Map.of("error", "This is your own offer."));

                    boolean alreadyInterested = messageRepo
                            .existsBySender_IdAndContextTypeAndContextId(callerId, Message.ContextType.OFFER, id);
                    if (!alreadyInterested) {
                        var caller = userRepo.findById(callerId).orElseThrow();
                        boolean de = "de".equalsIgnoreCase(o.getOfferedBy().getLanguage());
                        Message m = new Message();
                        m.setSender(caller);
                        m.setRecipient(o.getOfferedBy());
                        m.setContent((de ? "Ich interessiere mich für: " : "I'm interested in: ") + o.getTitle());
                        m.setContextType(Message.ContextType.OFFER);
                        m.setContextId(id);
                        Message saved = messageRepo.save(m);
                        Map<String, Object> payload = wsHandler.toMessagePayload(saved);
                        wsHandler.push(ownerId, payload);
                        wsHandler.push(callerId, payload);
                        notificationService.notifyNewMessage(ownerId, callerId, saved.getContent());
                        notificationCenter.notify(ownerId, Notification.Type.INTEREST, Map.of(
                                "offerId", id.toString(),
                                "offerTitle", o.getTitle(),
                                "fromUsername", caller.getUsername()));
                    }
                    return ResponseEntity.ok(Map.of(
                            "conversationWith", ownerId.toString(),
                            "created", !alreadyInterested));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /** Owner/admin only: how many distinct users expressed interest. */
    @GetMapping("{id}/interested")
    public ResponseEntity<?> interestedCount(@PathVariable UUID id) {
        UUID callerId = SecurityContext.authenticatedId();
        return offerRepo.findById(id)
                .<ResponseEntity<?>>map(o -> {
                    if (!o.getOfferedBy().getId().equals(callerId) && !adminGuard.isAdmin())
                        return ResponseEntity.status(403).body(Map.of("error", "Not your offer."));
                    long count = messageRepo.countInterestedUsers(Message.ContextType.OFFER, id,
                            o.getOfferedBy().getId());
                    return ResponseEntity.ok(Map.of("count", count));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /** Lifecycle: owner (or admin) moves an offer between ACTIVE / RESERVED / GIVEN. */
    @PostMapping("{id}/status")
    public ResponseEntity<?> changeStatus(@PathVariable UUID id, @Valid @RequestBody OfferDtos.StatusUpdate in) {
        UUID callerId = SecurityContext.authenticatedId();
        return offerRepo.findById(id)
                .<ResponseEntity<?>>map(o -> {
                    if (!o.getOfferedBy().getId().equals(callerId) && !adminGuard.isAdmin())
                        return ResponseEntity.status(403).body(Map.of("error", "Not your offer."));

                    Offer.Status newStatus;
                    try { newStatus = Offer.Status.valueOf(in.status.toUpperCase()); }
                    catch (IllegalArgumentException e) {
                        return ResponseEntity.badRequest().body(Map.of("error", "Invalid status."));
                    }

                    if (newStatus == Offer.Status.RESERVED && in.reservedForId != null && !in.reservedForId.isBlank()) {
                        try {
                            var reservedFor = userRepo.findById(UUID.fromString(in.reservedForId));
                            if (reservedFor.isEmpty())
                                return ResponseEntity.badRequest().body(Map.of("error", "Unknown user."));
                            o.setReservedFor(reservedFor.get());
                        } catch (IllegalArgumentException e) {
                            return ResponseEntity.badRequest().body(Map.of("error", "Invalid user id."));
                        }
                    }
                    if (newStatus != Offer.Status.RESERVED) o.setReservedFor(null);

                    o.setStatus(newStatus);
                    return ResponseEntity.ok(toResponse(offerRepo.save(o)));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("{id}")
    public ResponseEntity<OfferDtos.Response> get(@PathVariable UUID id) {
        return offerRepo.findById(id)
                .map(o -> {
                    OfferDtos.Response resp = toResponse(o);
                    resp.images = postImages.list(PostImage.TargetType.OFFER, id);
                    return ResponseEntity.ok(resp);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /** Replaces the gallery (ordered, max 5); first image becomes the cover. */
    @PutMapping("{id}/images")
    @Transactional
    public ResponseEntity<?> setImages(@PathVariable UUID id,
                                       @RequestBody Map<String, List<Map<String, String>>> body) {
        UUID callerId = SecurityContext.authenticatedId();
        return offerRepo.findById(id)
                .<ResponseEntity<?>>map(o -> {
                    if (!o.getOfferedBy().getId().equals(callerId) && !adminGuard.isAdmin())
                        return ResponseEntity.status(403).body(Map.of("error", "Not your offer."));
                    List<Map<String, String>> images = body.get("images");
                    if (images != null && images.size() > PostImageService.MAX_IMAGES)
                        return ResponseEntity.badRequest().body(Map.of("error",
                                "A post can have at most " + PostImageService.MAX_IMAGES + " images."));
                    String cover = postImages.replace(PostImage.TargetType.OFFER, id, images);
                    o.setImageUrl(cover);
                    offerRepo.save(o);
                    return ResponseEntity.ok(Map.of(
                            "images", postImages.list(PostImage.TargetType.OFFER, id)));
                })
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
    @Transactional
    public ResponseEntity<?> delete(@PathVariable UUID id) {
        UUID callerId = SecurityContext.authenticatedId();
        return offerRepo.findById(id)
                .map(o -> {
                    if (!o.getOfferedBy().getId().equals(callerId))
                        return ResponseEntity.status(403).<Void>build();
                    String imageUrl = o.getImageUrl();
                    likeRepo.deleteAllByTargetTypeAndTargetId(Like.TargetType.OFFER, id);
                    postImages.deleteAll(PostImage.TargetType.OFFER, id);
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
        out.lat = o.getLat();
        out.lon = o.getLon();
        out.postalCode = o.getPostalCode();
        out.city = o.getCity();
        out.status = o.getStatus().name();
        if (o.getReservedFor() != null) {
            out.reservedForId = o.getReservedFor().getId().toString();
            out.reservedForUsername = o.getReservedFor().getUsername();
        }
        return out;
    }

    /**
     * Resolves an optional postal code against the local plz_geo table.
     * Returns an error message, or null on success. Coordinates are the
     * PLZ centroid — deliberately never an exact address.
     */
    private String applyGeo(Offer post, String postalCode) {
        if (postalCode == null || postalCode.isBlank()) return null;
        var geo = plzRepo.findByPlz(postalCode.strip());
        if (geo.isEmpty()) return "Unknown postal code.";
        var g = geo.get();
        post.setPostalCode(g.getPlz());
        post.setCity(g.getCity());
        post.setLat(g.getLat());
        post.setLon(g.getLon());
        post.setRegion(g.getPlz() + " " + g.getCity());
        return null;
    }
}
