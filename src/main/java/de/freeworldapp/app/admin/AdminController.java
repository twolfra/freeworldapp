package de.freeworldapp.app.admin;

import de.freeworldapp.app.auth.AdminGuard;
import de.freeworldapp.app.auth.SecurityContext;
import de.freeworldapp.app.auth.PasswordResetTokenRepository;
import de.freeworldapp.app.auth.SessionRepository;
import de.freeworldapp.app.email.EmailService;
import de.freeworldapp.app.image.StorageService;
import de.freeworldapp.app.like.Like;
import de.freeworldapp.app.like.LikeRepository;
import de.freeworldapp.app.offer.Offer;
import de.freeworldapp.app.offer.OfferRepository;
import de.freeworldapp.app.message.MessageRepository;
import de.freeworldapp.app.report.Report;
import de.freeworldapp.app.report.ReportRepository;
import de.freeworldapp.app.report.dto.ReportDtos;
import de.freeworldapp.app.request.Request;
import de.freeworldapp.app.request.RequestRepository;
import de.freeworldapp.app.subscription.SubscriptionRepository;
import de.freeworldapp.app.user.User;
import de.freeworldapp.app.user.UserRepository;
import de.freeworldapp.app.user.dto.UserDtos;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminGuard adminGuard;
    private final UserRepository userRepo;
    private final OfferRepository offerRepo;
    private final RequestRepository requestRepo;
    private final LikeRepository likeRepo;
    private final ReportRepository reportRepo;
    private final SessionRepository sessionRepo;
    private final PasswordResetTokenRepository resetRepo;
    private final SubscriptionRepository subscriptionRepo;
    private final MessageRepository messageRepo;
    private final StorageService storage;
    private final EmailService emailService;
    private final AdminAuditRepository auditRepo;

    public AdminController(AdminGuard adminGuard, UserRepository userRepo, OfferRepository offerRepo,
                           RequestRepository requestRepo, LikeRepository likeRepo, ReportRepository reportRepo,
                           SessionRepository sessionRepo, PasswordResetTokenRepository resetRepo,
                           SubscriptionRepository subscriptionRepo,
                           MessageRepository messageRepo, StorageService storage, EmailService emailService,
                           AdminAuditRepository auditRepo) {
        this.adminGuard = adminGuard;
        this.userRepo = userRepo;
        this.offerRepo = offerRepo;
        this.requestRepo = requestRepo;
        this.likeRepo = likeRepo;
        this.reportRepo = reportRepo;
        this.sessionRepo = sessionRepo;
        this.resetRepo = resetRepo;
        this.subscriptionRepo = subscriptionRepo;
        this.messageRepo = messageRepo;
        this.storage = storage;
        this.emailService = emailService;
        this.auditRepo = auditRepo;
    }

    private static final ResponseEntity<Object> FORBIDDEN =
            ResponseEntity.status(403).body(Map.of("error", "Admin access required."));

    // ---- Users ----------------------------------------------------------

    @GetMapping("/users")
    public ResponseEntity<?> listUsers() {
        if (!adminGuard.isAdmin()) return FORBIDDEN;
        List<UserDtos.AdminResponse> out = userRepo.findAll().stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .map(this::toAdminResponse)
                .toList();
        return ResponseEntity.ok(out);
    }

    @PostMapping("/users/{id}/block")
    @Transactional
    public ResponseEntity<?> block(@PathVariable UUID id) {
        if (!adminGuard.isAdmin()) return FORBIDDEN;
        if (id.equals(SecurityContext.authenticatedId()))
            return ResponseEntity.badRequest().body(Map.of("error", "You cannot block your own account."));
        return userRepo.findById(id).map(u -> {
            u.setBlocked(true);
            u.setBlockedAt(Instant.now());
            userRepo.save(u);
            sessionRepo.deleteByUser_Id(id); // kill any live sessions immediately
            audit(AdminAuditEntry.Action.BLOCK_USER, AdminAuditEntry.TargetType.USER, id);
            return ResponseEntity.ok((Object) toAdminResponse(u));
        }).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/users/{id}/unblock")
    public ResponseEntity<?> unblock(@PathVariable UUID id) {
        if (!adminGuard.isAdmin()) return FORBIDDEN;
        return userRepo.findById(id).map(u -> {
            u.setBlocked(false);
            u.setBlockedAt(null);
            userRepo.save(u);
            audit(AdminAuditEntry.Action.UNBLOCK_USER, AdminAuditEntry.TargetType.USER, id);
            return ResponseEntity.ok((Object) toAdminResponse(u));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/users/{id}")
    @Transactional
    public ResponseEntity<?> deleteUser(@PathVariable UUID id) {
        if (!adminGuard.isAdmin()) return FORBIDDEN;
        if (id.equals(SecurityContext.authenticatedId()))
            return ResponseEntity.badRequest().body(Map.of("error", "You cannot delete your own account."));
        if (!userRepo.existsById(id)) return ResponseEntity.notFound().build();

        offerRepo.findByOfferedBy_Id(id).forEach(o -> storage.delete(o.getImageUrl()));
        requestRepo.findByRequestedBy_Id(id).forEach(r -> storage.delete(r.getImageUrl()));

        reportRepo.deleteAllByReporterId(id);
        reportRepo.deleteAllByTargetTypeAndTargetId(Report.TargetType.USER, id);
        offerRepo.findByOfferedBy_Id(id).forEach(o ->
                reportRepo.deleteAllByTargetTypeAndTargetId(Report.TargetType.OFFER, o.getId()));
        requestRepo.findByRequestedBy_Id(id).forEach(r ->
                reportRepo.deleteAllByTargetTypeAndTargetId(Report.TargetType.REQUEST, r.getId()));

        sessionRepo.deleteByUser_Id(id);
        resetRepo.deleteByUser_Id(id);
        subscriptionRepo.deleteAllInvolvingUser(id);
        messageRepo.deleteAllInvolvingUser(id);
        likeRepo.deleteAllByUserId(id);
        offerRepo.deleteAll(offerRepo.findByOfferedBy_Id(id));
        requestRepo.deleteAll(requestRepo.findByRequestedBy_Id(id));
        userRepo.deleteById(id);
        audit(AdminAuditEntry.Action.DELETE_USER, AdminAuditEntry.TargetType.USER, id);

        return ResponseEntity.noContent().build();
    }

    // ---- Posts ----------------------------------------------------------

    @DeleteMapping("/offers/{id}")
    @Transactional
    public ResponseEntity<?> deleteOffer(@PathVariable UUID id) {
        if (!adminGuard.isAdmin()) return FORBIDDEN;
        return offerRepo.findById(id).map(o -> {
            removeOffer(o);
            audit(AdminAuditEntry.Action.DELETE_OFFER, AdminAuditEntry.TargetType.OFFER, id);
            return ResponseEntity.noContent().build();
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/requests/{id}")
    @Transactional
    public ResponseEntity<?> deleteRequest(@PathVariable UUID id) {
        if (!adminGuard.isAdmin()) return FORBIDDEN;
        return requestRepo.findById(id).map(r -> {
            removeRequest(r);
            audit(AdminAuditEntry.Action.DELETE_REQUEST, AdminAuditEntry.TargetType.REQUEST, id);
            return ResponseEntity.noContent().build();
        }).orElse(ResponseEntity.notFound().build());
    }

    // ---- Reports (moderation queue) -------------------------------------

    @GetMapping("/reports")
    public ResponseEntity<?> listReports(@RequestParam(required = false) String status) {
        if (!adminGuard.isAdmin()) return FORBIDDEN;
        List<Report> reports;
        if (status == null || status.isBlank()) {
            reports = reportRepo.findByStatusOrderByCreatedAtDesc(Report.Status.OPEN);
        } else if ("ALL".equalsIgnoreCase(status)) {
            reports = reportRepo.findAllWithReporter();
        } else {
            try {
                reports = reportRepo.findByStatusOrderByCreatedAtDesc(Report.Status.valueOf(status.toUpperCase()));
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid status."));
            }
        }
        return ResponseEntity.ok(reports.stream().map(this::toReportResponse).toList());
    }

    @PostMapping("/reports/{id}/resolve")
    public ResponseEntity<?> resolveReport(@PathVariable UUID id) {
        return setReportStatus(id, Report.Status.RESOLVED);
    }

    @PostMapping("/reports/{id}/dismiss")
    public ResponseEntity<?> dismissReport(@PathVariable UUID id) {
        return setReportStatus(id, Report.Status.DISMISSED);
    }

    private ResponseEntity<?> setReportStatus(UUID id, Report.Status status) {
        if (!adminGuard.isAdmin()) return FORBIDDEN;
        return reportRepo.findById(id).map(r -> {
            r.setStatus(status);
            r.setResolvedBy(SecurityContext.authenticatedId());
            r.setResolvedAt(Instant.now());
            reportRepo.save(r);
            audit(status == Report.Status.RESOLVED
                            ? AdminAuditEntry.Action.RESOLVE_REPORT
                            : AdminAuditEntry.Action.DISMISS_REPORT,
                    AdminAuditEntry.TargetType.REPORT, id);
            return ResponseEntity.ok((Object) toReportResponse(r));
        }).orElse(ResponseEntity.notFound().build());
    }

    // ---- Audit log -------------------------------------------------------

    @GetMapping("/audit")
    public ResponseEntity<?> auditLog() {
        if (!adminGuard.isAdmin()) return FORBIDDEN;
        List<Map<String, Object>> out = auditRepo
                .findAllByOrderByCreatedAtDesc(org.springframework.data.domain.PageRequest.of(0, 200))
                .stream().map(e -> {
                    Map<String, Object> row = new java.util.LinkedHashMap<>();
                    row.put("id", e.getId().toString());
                    row.put("adminUsername", e.getAdminUsername());
                    row.put("action", e.getAction().name());
                    row.put("targetType", e.getTargetType().name());
                    row.put("targetId", e.getTargetId() != null ? e.getTargetId().toString() : null);
                    row.put("createdAt", java.time.format.DateTimeFormatter.ISO_INSTANT.format(e.getCreatedAt()));
                    return row;
                }).toList();
        return ResponseEntity.ok(out);
    }

    /** Every admin action leaves an immutable audit trail entry. */
    private void audit(AdminAuditEntry.Action action, AdminAuditEntry.TargetType targetType, UUID targetId) {
        UUID adminId = SecurityContext.authenticatedId();
        AdminAuditEntry e = new AdminAuditEntry();
        e.setAdminId(adminId);
        e.setAdminUsername(userRepo.findById(adminId).map(User::getUsername).orElse("unknown"));
        e.setAction(action);
        e.setTargetType(targetType);
        e.setTargetId(targetId);
        auditRepo.save(e);
    }

    // ---- Helpers --------------------------------------------------------

    /** Mirrors OfferController.delete: clears likes, reports, the row and its image. */
    private void removeOffer(Offer o) {
        String imageUrl = o.getImageUrl();
        User owner = o.getOfferedBy();
        String title = o.getTitle();
        likeRepo.deleteAllByTargetTypeAndTargetId(Like.TargetType.OFFER, o.getId());
        reportRepo.deleteAllByTargetTypeAndTargetId(Report.TargetType.OFFER, o.getId());
        offerRepo.delete(o);
        storage.delete(imageUrl);
        emailService.sendPostRemovedEmail(owner.getEmail(), owner.getUsername(), title, "offer");
    }

    private void removeRequest(Request r) {
        String imageUrl = r.getImageUrl();
        User owner = r.getRequestedBy();
        String title = r.getTitle();
        likeRepo.deleteAllByTargetTypeAndTargetId(Like.TargetType.REQUEST, r.getId());
        reportRepo.deleteAllByTargetTypeAndTargetId(Report.TargetType.REQUEST, r.getId());
        requestRepo.delete(r);
        storage.delete(imageUrl);
        emailService.sendPostRemovedEmail(owner.getEmail(), owner.getUsername(), title, "request");
    }

    private UserDtos.AdminResponse toAdminResponse(User u) {
        var out = new UserDtos.AdminResponse();
        out.id = u.getId().toString();
        out.username = u.getUsername();
        out.email = u.getEmail();
        out.createdAt = DateTimeFormatter.ISO_INSTANT.format(u.getCreatedAt());
        out.role = u.getRole().name();
        out.blocked = u.isBlocked();
        out.blockedAt = u.getBlockedAt() == null ? null : DateTimeFormatter.ISO_INSTANT.format(u.getBlockedAt());
        out.offerCount = offerRepo.findByOfferedBy_Id(u.getId()).size();
        out.requestCount = requestRepo.findByRequestedBy_Id(u.getId()).size();
        return out;
    }

    private ReportDtos.AdminResponse toReportResponse(Report r) {
        var out = new ReportDtos.AdminResponse();
        out.id = r.getId().toString();
        out.reporterId = r.getReporter().getId().toString();
        out.reporterUsername = r.getReporter().getUsername();
        out.targetType = r.getTargetType().name();
        out.targetId = r.getTargetId().toString();
        out.reason = r.getReason().name();
        out.note = r.getNote();
        out.status = r.getStatus().name();
        out.createdAt = DateTimeFormatter.ISO_INSTANT.format(r.getCreatedAt());
        out.resolvedBy = r.getResolvedBy() == null ? null : r.getResolvedBy().toString();
        out.resolvedAt = r.getResolvedAt() == null ? null : DateTimeFormatter.ISO_INSTANT.format(r.getResolvedAt());
        out.targetExists = false;

        switch (r.getTargetType()) {
            case OFFER -> offerRepo.findById(r.getTargetId()).ifPresent(o -> {
                out.targetExists = true;
                out.targetTitle = o.getTitle();
                out.targetAuthorId = o.getOfferedBy().getId().toString();
                out.targetAuthorUsername = o.getOfferedBy().getUsername();
                out.targetAuthorBlocked = o.getOfferedBy().isBlocked();
            });
            case REQUEST -> requestRepo.findById(r.getTargetId()).ifPresent(req -> {
                out.targetExists = true;
                out.targetTitle = req.getTitle();
                out.targetAuthorId = req.getRequestedBy().getId().toString();
                out.targetAuthorUsername = req.getRequestedBy().getUsername();
                out.targetAuthorBlocked = req.getRequestedBy().isBlocked();
            });
            case USER -> userRepo.findById(r.getTargetId()).ifPresent(u -> {
                out.targetExists = true;
                out.targetTitle = u.getUsername();
                out.targetAuthorId = u.getId().toString();
                out.targetAuthorUsername = u.getUsername();
                out.targetAuthorBlocked = u.isBlocked();
            });
        }
        return out;
    }
}
