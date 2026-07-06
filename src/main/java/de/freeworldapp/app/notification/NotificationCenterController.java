package de.freeworldapp.app.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.freeworldapp.app.auth.SecurityContext;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * In-app notification centre. Distinct from NotificationController, which
 * owns the (partly public) email-preference endpoints.
 */
@RestController
@RequestMapping("/api/notifications")
public class NotificationCenterController {

    private final NotificationRepository repo;
    private final ObjectMapper mapper;

    public NotificationCenterController(NotificationRepository repo, ObjectMapper mapper) {
        this.repo = repo;
        this.mapper = mapper;
    }

    /** Latest 50 notifications of the caller plus the unread count. */
    @GetMapping
    public ResponseEntity<?> list() {
        UUID callerId = SecurityContext.authenticatedId();
        if (callerId == null) return ResponseEntity.status(401).build();
        List<Map<String, Object>> items = repo
                .findByUser_IdOrderByCreatedAtDesc(callerId, PageRequest.of(0, 50))
                .stream().map(n -> toResponse(n, mapper)).toList();
        return ResponseEntity.ok(Map.of(
                "items", items,
                "unread", repo.countByUser_IdAndReadAtIsNull(callerId)));
    }

    @PostMapping("/mark-all-read")
    @Transactional
    public ResponseEntity<?> markAllRead() {
        UUID callerId = SecurityContext.authenticatedId();
        if (callerId == null) return ResponseEntity.status(401).build();
        repo.markAllRead(callerId, Instant.now());
        return ResponseEntity.ok(Map.of("message", "All notifications marked as read."));
    }

    static Map<String, Object> toResponse(Notification n, ObjectMapper mapper) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("id", n.getId().toString());
        out.put("type", n.getType().name());
        Object payload = null;
        try {
            if (n.getPayload() != null) payload = mapper.readValue(n.getPayload(), Map.class);
        } catch (Exception ignored) {}
        out.put("payload", payload);
        out.put("readAt", n.getReadAt() != null ? DateTimeFormatter.ISO_INSTANT.format(n.getReadAt()) : null);
        out.put("createdAt", DateTimeFormatter.ISO_INSTANT.format(n.getCreatedAt()));
        return out;
    }
}
