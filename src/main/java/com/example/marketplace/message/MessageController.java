package com.example.marketplace.message;

import com.example.marketplace.message.dto.MessageDtos;
import com.example.marketplace.user.UserRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
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

    public MessageController(MessageRepository messageRepo, UserRepository userRepo) {
        this.messageRepo = messageRepo;
        this.userRepo = userRepo;
    }

    @PostMapping
    public ResponseEntity<?> send(@Valid @RequestBody MessageDtos.Send in) {
        UUID senderId, recipientId;
        try {
            senderId = UUID.fromString(in.senderId);
            recipientId = UUID.fromString(in.recipientId);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid user id."));
        }

        var sender = userRepo.findById(senderId);
        var recipient = userRepo.findById(recipientId);
        if (sender.isEmpty()) return ResponseEntity.badRequest().body(Map.of("error", "Sender not found."));
        if (recipient.isEmpty()) return ResponseEntity.badRequest().body(Map.of("error", "Recipient not found."));

        Message m = new Message();
        m.setSender(sender.get());
        m.setRecipient(recipient.get());
        m.setContent(in.content);
        Message saved = messageRepo.save(m);
        return ResponseEntity
                .created(URI.create("/api/messages/" + saved.getId()))
                .body(toResponse(saved));
    }

    @GetMapping("/conversations")
    public ResponseEntity<?> getConversations(@RequestParam String userId) {
        UUID uid;
        try {
            uid = UUID.fromString(userId);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid user id."));
        }

        List<Message> messages = messageRepo.findAllForUser(uid);

        // Group by conversation partner; messages are DESC so first entry per partner is most recent
        Map<UUID, Message> latestByPartner = new LinkedHashMap<>();
        for (Message msg : messages) {
            UUID partnerId = msg.getSender().getId().equals(uid)
                    ? msg.getRecipient().getId()
                    : msg.getSender().getId();
            latestByPartner.putIfAbsent(partnerId, msg);
        }

        List<MessageDtos.ConversationSummary> summaries = latestByPartner.entrySet().stream()
                .map(entry -> {
                    Message latest = entry.getValue();
                    var partner = entry.getKey().equals(latest.getSender().getId())
                            ? latest.getSender()
                            : latest.getRecipient();
                    var s = new MessageDtos.ConversationSummary();
                    s.userId = partner.getId().toString();
                    s.username = partner.getUsername();
                    s.lastMessage = latest.getContent();
                    s.lastMessageAt = DateTimeFormatter.ISO_INSTANT.format(latest.getCreatedAt());
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

        List<MessageDtos.Response> result = messageRepo.findConversation(uid, oid).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    private MessageDtos.Response toResponse(Message m) {
        var out = new MessageDtos.Response();
        out.id = m.getId().toString();
        out.senderId = m.getSender().getId().toString();
        out.senderUsername = m.getSender().getUsername();
        out.recipientId = m.getRecipient().getId().toString();
        out.recipientUsername = m.getRecipient().getUsername();
        out.content = m.getContent();
        out.createdAt = DateTimeFormatter.ISO_INSTANT.format(m.getCreatedAt());
        return out;
    }
}
