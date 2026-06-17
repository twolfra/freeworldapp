package com.example.marketplace.report;

import com.example.marketplace.auth.SecurityContext;
import com.example.marketplace.offer.OfferRepository;
import com.example.marketplace.report.dto.ReportDtos;
import com.example.marketplace.request.RequestRepository;
import com.example.marketplace.user.User;
import com.example.marketplace.user.UserRepository;
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

    public ReportController(ReportRepository reportRepo, UserRepository userRepo,
                            OfferRepository offerRepo, RequestRepository requestRepo) {
        this.reportRepo = reportRepo;
        this.userRepo = userRepo;
        this.offerRepo = offerRepo;
        this.requestRepo = requestRepo;
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

        return ResponseEntity.ok(Map.of("message", "Thanks — this has been reported to our moderators."));
    }
}
