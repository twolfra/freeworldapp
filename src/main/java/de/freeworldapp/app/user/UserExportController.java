package de.freeworldapp.app.user;

import de.freeworldapp.app.auth.SecurityContext;
import de.freeworldapp.app.like.LikeRepository;
import de.freeworldapp.app.message.MessageRepository;
import de.freeworldapp.app.notification.NotificationRepository;
import de.freeworldapp.app.offer.OfferRepository;
import de.freeworldapp.app.request.RequestRepository;
import de.freeworldapp.app.subscription.SubscriptionRepository;
import de.freeworldapp.app.thanks.ThanksRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** DSGVO data export (AP 4.4): everything we store about the caller, as JSON. */
@RestController
public class UserExportController {

    private final UserRepository userRepo;
    private final OfferRepository offerRepo;
    private final RequestRepository requestRepo;
    private final MessageRepository messageRepo;
    private final SubscriptionRepository subscriptionRepo;
    private final LikeRepository likeRepo;
    private final ThanksRepository thanksRepo;
    private final NotificationRepository notificationRepo;

    public UserExportController(UserRepository userRepo, OfferRepository offerRepo,
                                RequestRepository requestRepo, MessageRepository messageRepo,
                                SubscriptionRepository subscriptionRepo, LikeRepository likeRepo,
                                ThanksRepository thanksRepo, NotificationRepository notificationRepo) {
        this.userRepo = userRepo;
        this.offerRepo = offerRepo;
        this.requestRepo = requestRepo;
        this.messageRepo = messageRepo;
        this.subscriptionRepo = subscriptionRepo;
        this.likeRepo = likeRepo;
        this.thanksRepo = thanksRepo;
        this.notificationRepo = notificationRepo;
    }

    @GetMapping("/api/users/me/export")
    public ResponseEntity<?> export() {
        UUID id = SecurityContext.authenticatedId();
        User u = userRepo.findById(id).orElse(null);
        if (u == null) return ResponseEntity.status(401).build();

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("exportedAt", DateTimeFormatter.ISO_INSTANT.format(java.time.Instant.now()));

        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("id", u.getId().toString());
        profile.put("username", u.getUsername());
        profile.put("email", u.getEmail());
        profile.put("createdAt", iso(u.getCreatedAt()));
        profile.put("role", u.getRole().name());
        profile.put("language", u.getLanguage());
        profile.put("notifyOnMessage", u.isNotifyOnMessage());
        profile.put("displayName", u.getDisplayName());
        profile.put("bio", u.getBio());
        profile.put("avatarUrl", u.getAvatarUrl());
        profile.put("postalCode", u.getPostalCode());
        profile.put("city", u.getCity());
        out.put("profile", profile);

        out.put("offers", offerRepo.findByOfferedBy_Id(id).stream().map(o -> Map.of(
                "id", o.getId().toString(), "title", o.getTitle(),
                "description", o.getDescription(), "category", o.getCategory(),
                "region", nullSafe(o.getRegion()), "status", o.getStatus().name(),
                "createdAt", iso(o.getCreatedAt()))).toList());

        out.put("requests", requestRepo.findByRequestedBy_Id(id).stream().map(r -> Map.of(
                "id", r.getId().toString(), "title", r.getTitle(),
                "description", r.getDescription(), "category", r.getCategory(),
                "region", nullSafe(r.getRegion()), "status", r.getStatus().name(),
                "createdAt", iso(r.getCreatedAt()))).toList());

        out.put("messages", messageRepo.findAllInvolvingUserOrdered(id).stream().map(m -> Map.of(
                "direction", m.getSender().getId().equals(id) ? "sent" : "received",
                "otherParty", m.getSender().getId().equals(id)
                        ? m.getRecipient().getUsername() : m.getSender().getUsername(),
                "content", m.getContent(),
                "createdAt", iso(m.getCreatedAt()))).toList());

        out.put("following", subscriptionRepo.findBySubscriber_Id(id).stream()
                .map(sub -> sub.getSubscribedTo().getUsername()).toList());

        out.put("likes", likeRepo.findAll().stream()
                .filter(l -> l.getUser().getId().equals(id))
                .map(l -> Map.of("targetType", l.getTargetType().name(),
                        "targetId", l.getTargetId().toString(),
                        "createdAt", iso(l.getCreatedAt()))).toList());

        out.put("thanksGiven", thanksRepo.findByFromUser_IdOrderByCreatedAtDesc(id).stream()
                .map(t -> Map.of("offerTitle", t.getOfferTitle(),
                        "text", nullSafe(t.getText()), "createdAt", iso(t.getCreatedAt()))).toList());
        out.put("thanksReceived", thanksRepo.findByToUser_IdOrderByCreatedAtDesc(id).stream()
                .map(t -> Map.of("from", t.getFromUser().getUsername(), "offerTitle", t.getOfferTitle(),
                        "text", nullSafe(t.getText()), "createdAt", iso(t.getCreatedAt()))).toList());

        out.put("notifications", notificationRepo
                .findByUser_IdOrderByCreatedAtDesc(id, PageRequest.of(0, 500)).stream()
                .map(n -> Map.of("type", n.getType().name(),
                        "payload", nullSafe(n.getPayload()),
                        "createdAt", iso(n.getCreatedAt()))).toList());

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"freeworld-export.json\"")
                .contentType(MediaType.APPLICATION_JSON)
                .body(out);
    }

    private static String iso(java.time.Instant instant) {
        return DateTimeFormatter.ISO_INSTANT.format(instant);
    }

    private static String nullSafe(String s) {
        return s == null ? "" : s;
    }
}
