package de.freeworldapp.app.user;

import de.freeworldapp.app.auth.SecurityContext;
import de.freeworldapp.app.auth.PasswordResetTokenRepository;
import de.freeworldapp.app.auth.SessionRepository;
import de.freeworldapp.app.email.EmailService;
import de.freeworldapp.app.image.StorageService;
import de.freeworldapp.app.like.LikeRepository;
import de.freeworldapp.app.message.MessageRepository;
import de.freeworldapp.app.notification.NotificationRepository;
import de.freeworldapp.app.offer.OfferRepository;
import de.freeworldapp.app.report.Report;
import de.freeworldapp.app.report.ReportRepository;
import de.freeworldapp.app.request.RequestRepository;
import de.freeworldapp.app.subscription.SubscriptionRepository;
import de.freeworldapp.app.thanks.ThanksRepository;
import de.freeworldapp.app.user.dto.UserDtos;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository userRepo;
    private final PasswordEncoder encoder;
    private final EmailService emailService;
    private final SessionRepository sessionRepo;
    private final PasswordResetTokenRepository resetRepo;
    private final MessageRepository messageRepo;
    private final SubscriptionRepository subscriptionRepo;
    private final OfferRepository offerRepo;
    private final RequestRepository requestRepo;
    private final StorageService storageService;
    private final LikeRepository likeRepo;
    private final ReportRepository reportRepo;
    private final ThanksRepository thanksRepo;
    private final NotificationRepository notificationRepo;

    public UserController(UserRepository userRepo, PasswordEncoder encoder, EmailService emailService,
                          SessionRepository sessionRepo, PasswordResetTokenRepository resetRepo,
                          MessageRepository messageRepo,
                          SubscriptionRepository subscriptionRepo, OfferRepository offerRepo,
                          RequestRepository requestRepo, StorageService storageService, LikeRepository likeRepo,
                          ReportRepository reportRepo, ThanksRepository thanksRepo,
                          NotificationRepository notificationRepo) {
        this.userRepo = userRepo;
        this.encoder = encoder;
        this.emailService = emailService;
        this.sessionRepo = sessionRepo;
        this.resetRepo = resetRepo;
        this.messageRepo = messageRepo;
        this.subscriptionRepo = subscriptionRepo;
        this.offerRepo = offerRepo;
        this.requestRepo = requestRepo;
        this.storageService = storageService;
        this.likeRepo = likeRepo;
        this.reportRepo = reportRepo;
        this.thanksRepo = thanksRepo;
        this.notificationRepo = notificationRepo;
    }

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody UserDtos.Create in) {
        if (userRepo.existsByUsername(in.username))
            return ResponseEntity.badRequest().body(Map.of("error", "Username already taken."));
        if (userRepo.existsByEmail(in.email))
            return ResponseEntity.badRequest().body(Map.of("error", "Email already registered."));

        String token = UUID.randomUUID().toString();

        User u = new User();
        u.setUsername(in.username);
        u.setEmail(in.email);
        u.setPasswordHash(encoder.encode(in.password));
        u.setEmailVerified(false);
        u.setVerificationToken(token);
        u.setVerificationTokenExpiresAt(Instant.now().plus(24, ChronoUnit.HOURS));
        if ("de".equalsIgnoreCase(in.language)) u.setLanguage("de");
        u = userRepo.save(u);

        emailService.sendVerificationEmail(u.getEmail(), token);

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

    /** Owner-only partial profile update: null keeps, "" clears a field. */
    @PutMapping("{id}/profile")
    public ResponseEntity<?> updateProfile(@PathVariable UUID id, @Valid @RequestBody UserDtos.ProfileUpdate in) {
        UUID callerId = SecurityContext.authenticatedId();
        if (!callerId.equals(id))
            return ResponseEntity.status(403).body(Map.of("error", "You can only update your own profile."));

        return userRepo.findById(id).map(u -> {
            if (in.displayName != null) u.setDisplayName(blankToNull(in.displayName));
            if (in.bio != null) u.setBio(blankToNull(in.bio));
            if (in.avatarUrl != null) {
                String old = u.getAvatarUrl();
                String next = blankToNull(in.avatarUrl);
                if (old != null && !old.equals(next)) storageService.delete(old);
                u.setAvatarUrl(next);
            }
            if (in.postalCode != null) u.setPostalCode(blankToNull(in.postalCode));
            if (in.city != null) u.setCity(blankToNull(in.city));
            return ResponseEntity.ok(toOwnResponse(userRepo.save(u)));
        }).orElse(ResponseEntity.notFound().build());
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.strip();
    }

    /** Full own-account view (used after profile updates so the client can sync). */
    private UserDtos.Response toOwnResponse(User u) {
        var out = new UserDtos.Response();
        out.id = u.getId().toString();
        out.username = u.getUsername();
        out.email = u.getEmail();
        out.createdAt = DateTimeFormatter.ISO_INSTANT.format(u.getCreatedAt());
        out.role = u.getRole().name();
        out.notifyOnMessage = u.isNotifyOnMessage();
        out.language = u.getLanguage();
        out.displayName = u.getDisplayName();
        out.bio = u.getBio();
        out.avatarUrl = u.getAvatarUrl();
        out.postalCode = u.getPostalCode();
        out.city = u.getCity();
        return out;
    }

    @DeleteMapping("{id}")
    @Transactional
    public ResponseEntity<?> delete(@PathVariable UUID id) {
        UUID callerId = SecurityContext.authenticatedId();
        if (!callerId.equals(id))
            return ResponseEntity.status(403).body(Map.of("error", "You can only delete your own account."));

        if (!userRepo.existsById(id)) return ResponseEntity.notFound().build();

        // Delete images from storage before removing the records
        offerRepo.findByOfferedBy_Id(id).forEach(o -> storageService.delete(o.getImageUrl()));
        requestRepo.findByRequestedBy_Id(id).forEach(r -> storageService.delete(r.getImageUrl()));

        // Reports filed by this user, reports targeting this user, and reports
        // targeting any of this user's posts.
        reportRepo.deleteAllByReporterId(id);
        reportRepo.deleteAllByTargetTypeAndTargetId(Report.TargetType.USER, id);
        offerRepo.findByOfferedBy_Id(id).forEach(o ->
                reportRepo.deleteAllByTargetTypeAndTargetId(Report.TargetType.OFFER, o.getId()));
        requestRepo.findByRequestedBy_Id(id).forEach(r ->
                reportRepo.deleteAllByTargetTypeAndTargetId(Report.TargetType.REQUEST, r.getId()));

        sessionRepo.deleteByUser_Id(id);
        resetRepo.deleteByUser_Id(id);
        thanksRepo.deleteByFromUser_IdOrToUser_Id(id, id);
        notificationRepo.deleteByUser_Id(id);
        subscriptionRepo.deleteAllInvolvingUser(id);
        messageRepo.deleteAllInvolvingUser(id);
        likeRepo.deleteAllByUserId(id);
        offerRepo.deleteAll(offerRepo.findByOfferedBy_Id(id));
        requestRepo.deleteAll(requestRepo.findByRequestedBy_Id(id));
        userRepo.deleteById(id);

        return ResponseEntity.noContent().build();
    }

    private UserDtos.PublicResponse toPublicResponse(User u) {
        var out = new UserDtos.PublicResponse();
        out.id = u.getId().toString();
        out.username = u.getUsername();
        out.createdAt = DateTimeFormatter.ISO_INSTANT.format(u.getCreatedAt());
        out.displayName = u.getDisplayName();
        out.bio = u.getBio();
        out.avatarUrl = u.getAvatarUrl();
        out.city = u.getCity();
        return out;
    }
}
