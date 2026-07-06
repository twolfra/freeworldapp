package de.freeworldapp.app.report;

import de.freeworldapp.app.auth.SecurityContext;
import de.freeworldapp.app.email.EmailService;
import de.freeworldapp.app.offer.OfferRepository;
import de.freeworldapp.app.report.dto.ReportDtos;
import de.freeworldapp.app.request.RequestRepository;
import de.freeworldapp.app.thanks.ThanksRepository;
import de.freeworldapp.app.user.Role;
import de.freeworldapp.app.user.User;
import de.freeworldapp.app.user.UserRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportRepository reportRepo;
    private final UserRepository userRepo;
    private final OfferRepository offerRepo;
    private final RequestRepository requestRepo;
    private final EmailService emailService;
    private final ThanksRepository thanksRepo;

    public ReportController(ReportRepository reportRepo, UserRepository userRepo,
                            OfferRepository offerRepo, RequestRepository requestRepo,
                            EmailService emailService,
                            ThanksRepository thanksRepo) {
        this.reportRepo = reportRepo;
        this.userRepo = userRepo;
        this.offerRepo = offerRepo;
        this.requestRepo = requestRepo;
        this.emailService = emailService;
        this.thanksRepo = thanksRepo;
    }

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody ReportDtos.Create in) {
        UUID reporterId = SecurityContext.authenticatedId();
        User reporter = userRepo.findById(reporterId).orElse(null);
        if (reporter == null) return ResponseEntity.status(401).build();

        Report.TargetType type;
        Report.Reason reason;
        UUID targetId;
        try {
            type = Report.TargetType.valueOf(in.targetType.toUpperCase());
            reason = Report.Reason.valueOf(in.reason.toUpperCase());
            targetId = UUID.fromString(in.targetId);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid report parameters."));
        }

        // Verify the target exists and determine its owner (to block self-reporting)
        UUID ownerId;
        switch (type) {
            case OFFER -> {
                var offer = offerRepo.findById(targetId).orElse(null);
                if (offer == null) return ResponseEntity.status(404).body(Map.of("error", "Offer not found."));
                ownerId = offer.getOfferedBy().getId();
            }
            case REQUEST -> {
                var request = requestRepo.findById(targetId).orElse(null);
                if (request == null) return ResponseEntity.status(404).body(Map.of("error", "Request not found."));
                ownerId = request.getRequestedBy().getId();
            }
            case THANKS -> {
                var thanks = thanksRepo.findById(targetId).orElse(null);
                if (thanks == null) return ResponseEntity.status(404).body(Map.of("error", "Thanks not found."));
                ownerId = thanks.getFromUser().getId();
            }
            default -> { // USER
                if (!userRepo.existsById(targetId)) return ResponseEntity.status(404).body(Map.of("error", "User not found."));
                ownerId = targetId;
            }
        }

        if (reporterId.equals(ownerId)) {
            return ResponseEntity.badRequest().body(Map.of("error", "You cannot report your own content."));
        }

        // Prevent duplicate open reports from the same user against the same target
        if (reportRepo.existsByReporter_IdAndTargetTypeAndTargetIdAndStatus(
                reporterId, type, targetId, Report.Status.OPEN)) {
            return ResponseEntity.status(409).body(Map.of("error", "You have already reported this. It's under review."));
        }

        Report report = new Report();
        report.setReporter(reporter);
        report.setTargetType(type);
        report.setTargetId(targetId);
        report.setReason(reason);
        report.setNote(in.note);
        report.setStatus(Report.Status.OPEN);
        reportRepo.save(report);

        // Notify all admins by email
        String targetTitle = switch (type) {
            case OFFER    -> offerRepo.findById(targetId).map(o -> o.getTitle()).orElse(targetId.toString());
            case REQUEST  -> requestRepo.findById(targetId).map(r -> r.getTitle()).orElse(targetId.toString());
            case USER     -> userRepo.findById(targetId).map(User::getUsername).orElse(targetId.toString());
            case THANKS   -> thanksRepo.findById(targetId)
                    .map(t -> "Thanks on \"" + t.getOfferTitle() + "\"").orElse(targetId.toString());
        };
        userRepo.findAll().stream()
                .filter(u -> u.getRole() == Role.ADMIN)
                .forEach(admin -> emailService.sendNewReportEmail(
                        admin.getEmail(), reporter.getUsername(),
                        type.name(), targetTitle, reason.name()));

        return ResponseEntity.ok(Map.of("message", "Thanks — this has been reported to our moderators."));
    }
}
