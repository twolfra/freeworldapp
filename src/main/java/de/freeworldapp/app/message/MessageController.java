package de.freeworldapp.app.message;

import de.freeworldapp.app.auth.SecurityContext;
import de.freeworldapp.app.message.dto.MessageDtos;
import de.freeworldapp.app.user.UserRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/messages")
public class MessageController {

    private final MessageRepository messageRepo;
    private final UserRepository userRepo;
    private final ChatWebSocketHandler wsHandler;
    private final MessageNotificationService notificationService;

    public MessageController(MessageRepository messageRepo, UserRepository userRepo,
                             ChatWebSocketHandler wsHandler, MessageNotificationService notificationService) {
        this.messageRepo = messageRepo;
        this.userRepo = userRepo;
        this.wsHandler = wsHandler;
        this.notificationService = notificationService;
    }

    @PostMapping
    public ResponseEntity<?> send(@Valid @RequestBody MessageDtos.Send in) {
        UUID senderId = SecurityContext.authenticatedId();
        UUID recipientId;
        try { recipientId = UUID.fromString(in.recipientId); }
        catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid user id."));
        }

        if (senderId.equals(recipientId))
            return ResponseEntity.badRequest().body(Map.of("error", "You cannot send a message to yourself."));

        var sender    = userRepo.findById(senderId);
        var recipient = userRepo.findById(recipientId);
        if (sender.isEmpty())    return ResponseEntity.badRequest().body(Map.of("error", "Sender not found."));
        if (recipient.isEmpty() || recipient.get().isDeleted())
            return ResponseEntity.badRequest().body(Map.of("error", "Recipient not found."));

        Message m = new Message();
        m.setSender(sender.get());
        m.setRecipient(recipient.get());
        m.setContent(in.content);
        Message saved = messageRepo.save(m);

        MessageDtos.Response resp = toResponse(saved);
        // Push via WebSocket to both parties (covers connected browser tabs)
        Map<String, Object> wsPush = wsHandler.toMessagePayload(saved);
        wsHandler.push(recipientId, wsPush);
        wsHandler.push(senderId, wsPush);

        // Email the recipient if they aren't connected (handled async + presence-checked inside).
        notificationService.notifyNewMessage(recipientId, senderId, saved.getContent());

        return ResponseEntity.created(URI.create("/api/messages/" + saved.getId())).body(resp);
    }

    @GetMapping("/conversations")
    public ResponseEntity<?> getConversations(@RequestParam String userId) {
        UUID uid;
        try { uid = UUID.fromString(userId); }
        catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid user id."));
        }

        UUID callerId = SecurityContext.authenticatedId();
        if (!callerId.equals(uid))
            return ResponseEntity.status(403).body(Map.of("error", "Access denied."));

        List<Message> messages = messageRepo.findAllForUser(uid);

        Map<UUID, Message> latestByPartner = new LinkedHashMap<>();
        for (Message msg : messages) {
            UUID partnerId = msg.getSender().getId().equals(uid)
                    ? msg.getRecipient().getId() : msg.getSender().getId();
            latestByPartner.putIfAbsent(partnerId, msg);
        }

        List<MessageDtos.ConversationSummary> summaries = latestByPartner.entrySet().stream()
                .map(entry -> {
                    Message latest = entry.getValue();
                    var partner = entry.getKey().equals(latest.getSender().getId())
                            ? latest.getSender() : latest.getRecipient();
                    var s = new MessageDtos.ConversationSummary();
                    s.userId        = partner.getId().toString();
                    s.username      = partner.getUsername();
                    s.lastMessage   = latest.getContent();
                    s.lastMessageAt = DateTimeFormatter.ISO_INSTANT.format(latest.getCreatedAt());
                    s.unreadCount   = messageRepo.countUnreadFromSender(uid, partner.getId());
                    s.deleted       = partner.isDeleted();
                    return s;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(summaries);
    }

    @GetMapping("/conversation")
    public ResponseEntity<?> getConversation(@RequestParam String userId, @RequestParam String otherId) {
        UUID uid, oid;
        try {
            uid = UUID.fromString(userId);
            oid = UUID.fromString(otherId);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid user id."));
        }

        UUID callerId = SecurityContext.authenticatedId();
        if (!callerId.equals(uid))
            return ResponseEntity.status(403).body(Map.of("error", "Access denied."));

        List<MessageDtos.Response> result = messageRepo.findConversation(uid, oid).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    @Transactional
    @PostMapping("/mark-read")
    public ResponseEntity<Void> markRead(@RequestParam String userId, @RequestParam String otherId) {
        UUID uid, oid;
        try {
            uid = UUID.fromString(userId);
            oid = UUID.fromString(otherId);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
        messageRepo.markConversationRead(uid, oid);
        wsHandler.push(oid, Map.of("type", "read", "readerId", uid.toString()));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/unread-count")
    public ResponseEntity<?> unreadCount(@RequestParam String userId) {
        UUID uid;
        try { uid = UUID.fromString(userId); }
        catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid user id."));
        }

        UUID callerId = SecurityContext.authenticatedId();
        if (!callerId.equals(uid))
            return ResponseEntity.status(403).body(Map.of("error", "Access denied."));

        return ResponseEntity.ok(Map.of("count", messageRepo.countUnread(uid)));
    }

    private MessageDtos.Response toResponse(Message m) {
        var out = new MessageDtos.Response();
        out.id               = m.getId().toString();
        out.senderId         = m.getSender().getId().toString();
        out.senderUsername   = m.getSender().getUsername();
        out.recipientId      = m.getRecipient().getId().toString();
        out.recipientUsername = m.getRecipient().getUsername();
        out.content          = m.getContent();
        out.createdAt        = DateTimeFormatter.ISO_INSTANT.format(m.getCreatedAt());
        out.readAt           = m.getReadAt() != null
                ? DateTimeFormatter.ISO_INSTANT.format(m.getReadAt()) : null;
        out.senderDeleted    = m.getSender().isDeleted();
        out.recipientDeleted = m.getRecipient().isDeleted();
        out.contextType      = m.getContextType() != null ? m.getContextType().name() : null;
        out.contextId        = m.getContextId() != null ? m.getContextId().toString() : null;
        return out;
    }
}
