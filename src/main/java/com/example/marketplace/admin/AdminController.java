package com.example.marketplace.admin;

import com.example.marketplace.auth.AdminGuard;
import com.example.marketplace.auth.SecurityContext;
import com.example.marketplace.auth.SessionRepository;
import com.example.marketplace.image.StorageService;
import com.example.marketplace.like.Like;
import com.example.marketplace.like.LikeRepository;
import com.example.marketplace.offer.Offer;
import com.example.marketplace.offer.OfferRepository;
import com.example.marketplace.report.Report;
import com.example.marketplace.report.ReportRepository;
import com.example.marketplace.report.dto.ReportDtos;
import com.example.marketplace.request.Request;
import com.example.marketplace.request.RequestRepository;
import com.example.marketplace.user.User;
import com.example.marketplace.user.UserRepository;
import com.example.marketplace.user.dto.UserDtos;
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
    private final StorageService storage;

    public AdminController(AdminGuard adminGuard, UserRepository userRepo, OfferRepository offerRepo,
                           RequestRepository requestRepo, LikeRepository likeRepo, ReportRepository reportRepo,
                           SessionRepository sessionRepo, StorageService storage) {
        this.adminGuard = adminGuard;
        this.userRepo = userRepo;
        this.offerRepo = offerRepo;
        this.requestRepo = requestRepo;
        this.likeRepo = likeRepo;
        this.reportRepo = reportRepo;
        this.sessionRepo = sessionRepo;
        this.storage = storage;
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
    public ResponseEntity<?> block(@PathVariable UUID id) {
        if (!adminGuard.isAdmin()) return FORBIDDEN;
        if (id.equals(SecurityContext.authenticatedId()))
            return ResponseEntity.badRequest().body(Map.of("error", "You cannot block your own account."));
        return userRepo.findById(id).map(u -> {
            u.setBlocked(true);
            u.setBlockedAt(Instant.now());
            userRepo.save(u);
            sessionRepo.deleteByUser_Id(id); // kill any live sessions immediately
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
            return ResponseEntity.ok((Object) toAdminResponse(u));
        }).orElse(ResponseEntity.notFound().build());
    }

    // ---- Posts ----------------------------------------------------------

    @DeleteMapping("/offers/{id}")
    @Transactional
    public ResponseEntity<?> deleteOffer(@PathVariable UUID id) {
        if (!adminGuard.isAdmin()) return FORBIDDEN;
        return offerRepo.findById(id).map(o -> {
            removeOffer(o);
            return ResponseEntity.noContent().build();
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/requests/{id}")
    @Transactional
    public ResponseEntity<?> deleteRequest(@PathVariable UUID id) {
        if (!adminGuard.isAdmin()) return FORBIDDEN;
        return requestRepo.findById(id).map(r -> {
            removeRequest(r);
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
            return ResponseEntity.ok((Object) toReportResponse(r));
        }).orElse(ResponseEntity.notFound().build());
    }

    // ---- Helpers --------------------------------------------------------

    /** Mirrors OfferController.delete: clears likes, reports, the row and its image. */
    private void removeOffer(Offer o) {
        String imageUrl = o.getImageUrl();
        likeRepo.deleteAllByTargetTypeAndTargetId(Like.TargetType.OFFER, o.getId());
        reportRepo.deleteAllByTargetTypeAndTargetId(Report.TargetType.OFFER, o.getId());
        offerRepo.delete(o);
        storage.delete(imageUrl);
    }

    private void removeRequest(Request r) {
        String imageUrl = r.getImageUrl();
        likeRepo.deleteAllByTargetTypeAndTargetId(Like.TargetType.REQUEST, r.getId());
        reportRepo.deleteAllByTargetTypeAndTargetId(Report.TargetType.REQUEST, r.getId());
        requestRepo.delete(r);
        storage.delete(imageUrl);
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
