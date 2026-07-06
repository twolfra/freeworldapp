package de.freeworldapp.app.message;

import de.freeworldapp.app.auth.SessionRepository;
import de.freeworldapp.app.user.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private final SessionRepository sessionRepo;
    private final MessageRepository messageRepo;
    private final UserRepository userRepo;
    private final ObjectMapper mapper;
    private final MessageNotificationService notificationService;

    // Fan-out: one user may have multiple open tabs/connections
    private final Map<UUID, CopyOnWriteArrayList<WebSocketSession>> userSessions = new ConcurrentHashMap<>();
    private final Map<String, UUID> wsToUser = new ConcurrentHashMap<>();

    // Connections that have not yet sent a valid {type:"auth"} frame.
    private final Map<String, WebSocketSession> pendingAuth = new ConcurrentHashMap<>();
    private final ScheduledExecutorService authTimeout = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "ws-auth-timeout");
        t.setDaemon(true);
        return t;
    });

    @Value("${app.ws.auth-timeout-ms:5000}")
    private long authTimeoutMs;

    public ChatWebSocketHandler(SessionRepository sessionRepo, MessageRepository messageRepo,
                                UserRepository userRepo, ObjectMapper mapper,
                                MessageNotificationService notificationService) {
        this.sessionRepo = sessionRepo;
        this.messageRepo = messageRepo;
        this.userRepo = userRepo;
        this.mapper = mapper;
        this.notificationService = notificationService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession ws) {
        // The token never travels in the URL (query params end up in access logs
        // and proxies). Clients must authenticate with their first frame:
        // {"type":"auth","token":...} — otherwise the connection is closed.
        pendingAuth.put(ws.getId(), ws);
        authTimeout.schedule(() -> {
            WebSocketSession stale = pendingAuth.remove(ws.getId());
            if (stale != null && stale.isOpen()) {
                try { stale.close(CloseStatus.POLICY_VIOLATION); } catch (IOException ignored) {}
            }
        }, authTimeoutMs, TimeUnit.MILLISECONDS);
    }

    @Override
    protected void handleTextMessage(WebSocketSession ws, TextMessage raw) throws Exception {
        UUID senderId = wsToUser.get(ws.getId());
        if (senderId == null) {
            handleAuthFrame(ws, raw);
            return;
        }

        Map<?, ?> body;
        try { body = mapper.readValue(raw.getPayload(), Map.class); }
        catch (Exception e) { return; }

        if (!"send".equals(body.get("type"))) return;

        String recipientIdStr = (String) body.get("recipientId");
        String content        = (String) body.get("content");
        if (recipientIdStr == null || content == null || content.isBlank()) return;

        UUID recipientId;
        try { recipientId = UUID.fromString(recipientIdStr); }
        catch (IllegalArgumentException e) { return; }

        if (senderId.equals(recipientId)) return;

        var sender    = userRepo.findById(senderId);
        var recipient = userRepo.findById(recipientId);
        if (sender.isEmpty() || recipient.isEmpty()) return;

        Message m = new Message();
        m.setSender(sender.get());
        m.setRecipient(recipient.get());
        m.setContent(content.trim());
        Message saved = messageRepo.save(m);

        Map<String, Object> payload = toMessagePayload(saved);
        push(recipientId, payload);
        push(senderId, payload);   // confirms the message to the sender with server-assigned id/timestamp

        // Email the recipient if they aren't connected (handled async + presence-checked inside).
        notificationService.notifyNewMessage(recipientId, senderId, saved.getContent());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession ws, CloseStatus status) {
        pendingAuth.remove(ws.getId());
        UUID userId = wsToUser.remove(ws.getId());
        if (userId == null) return;
        CopyOnWriteArrayList<WebSocketSession> list = userSessions.get(userId);
        if (list != null) {
            list.remove(ws);
            if (list.isEmpty()) userSessions.remove(userId, list);
        }
    }

    /** True if the user has at least one live WebSocket connection (any tab/page). */
    public boolean isOnline(UUID userId) {
        CopyOnWriteArrayList<WebSocketSession> list = userSessions.get(userId);
        return list != null && list.stream().anyMatch(WebSocketSession::isOpen);
    }

    /** Push an arbitrary payload to all open WebSocket sessions for userId. */
    public void push(UUID userId, Object payload) {
        CopyOnWriteArrayList<WebSocketSession> list = userSessions.get(userId);
        if (list == null || list.isEmpty()) return;
        String json;
        try { json = mapper.writeValueAsString(payload); }
        catch (Exception e) { return; }
        List<WebSocketSession> dead = new ArrayList<>();
        for (WebSocketSession ws : list) {
            try {
                synchronized (ws) {
                    if (ws.isOpen()) ws.sendMessage(new TextMessage(json));
                }
            } catch (IOException e) {
                dead.add(ws);
            }
        }
        if (!dead.isEmpty()) list.removeAll(dead);
    }

    private void handleAuthFrame(WebSocketSession ws, TextMessage raw) {
        String token = null;
        try {
            Map<?, ?> body = mapper.readValue(raw.getPayload(), Map.class);
            if ("auth".equals(body.get("type"))) token = (String) body.get("token");
        } catch (Exception ignored) {}

        UUID userId = token == null ? null : sessionRepo.findByRawTokenWithUser(token)
                .filter(s -> s.getExpiresAt() == null || s.getExpiresAt().isAfter(Instant.now()))
                .filter(s -> !s.getUser().isBlocked())
                .map(s -> s.getUser().getId())
                .orElse(null);

        if (userId == null) {
            pendingAuth.remove(ws.getId());
            try { ws.close(CloseStatus.POLICY_VIOLATION); } catch (IOException ignored) {}
            return;
        }

        pendingAuth.remove(ws.getId());
        wsToUser.put(ws.getId(), userId);
        userSessions.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>()).add(ws);
        try {
            synchronized (ws) {
                ws.sendMessage(new TextMessage("{\"type\":\"auth_ok\"}"));
            }
        } catch (IOException ignored) {}
    }

    public Map<String, Object> toMessagePayload(Message m) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("type", "message");
        map.put("id", m.getId().toString());
        map.put("senderId", m.getSender().getId().toString());
        map.put("senderUsername", m.getSender().getUsername());
        map.put("recipientId", m.getRecipient().getId().toString());
        map.put("recipientUsername", m.getRecipient().getUsername());
        map.put("content", m.getContent());
        map.put("createdAt", DateTimeFormatter.ISO_INSTANT.format(m.getCreatedAt()));
        map.put("readAt", m.getReadAt() != null ? DateTimeFormatter.ISO_INSTANT.format(m.getReadAt()) : null);
        map.put("contextType", m.getContextType() != null ? m.getContextType().name() : null);
        map.put("contextId", m.getContextId() != null ? m.getContextId().toString() : null);
        return map;
    }
}
