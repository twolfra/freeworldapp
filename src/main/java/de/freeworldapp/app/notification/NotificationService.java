package de.freeworldapp.app.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.freeworldapp.app.message.ChatWebSocketHandler;
import de.freeworldapp.app.user.User;
import de.freeworldapp.app.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Creates in-app notifications and pushes them live over the existing
 * WebSocket channel as {type:"notification", notification:{...}}.
 * Failures never propagate into the calling business flow.
 */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository repo;
    private final UserRepository userRepo;
    private final ChatWebSocketHandler wsHandler;
    private final ObjectMapper mapper;

    public NotificationService(NotificationRepository repo, UserRepository userRepo,
                               @Lazy ChatWebSocketHandler wsHandler, ObjectMapper mapper) {
        this.repo = repo;
        this.userRepo = userRepo;
        this.wsHandler = wsHandler;
        this.mapper = mapper;
    }

    public void notify(UUID userId, Notification.Type type, Map<String, Object> payload) {
        try {
            User user = userRepo.findById(userId).orElse(null);
            if (user == null || user.isBlocked()) return;

            Notification n = new Notification();
            n.setUser(user);
            n.setType(type);
            n.setPayload(mapper.writeValueAsString(payload));
            n = repo.save(n);

            Map<String, Object> push = new LinkedHashMap<>();
            push.put("type", "notification");
            push.put("notification", NotificationCenterController.toResponse(n, mapper));
            wsHandler.push(userId, push);
        } catch (Exception e) {
            log.warn("Failed to create notification {} for {}: {}", type, userId, e.getMessage());
        }
    }
}
