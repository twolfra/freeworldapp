package de.freeworldapp.app.message;

import de.freeworldapp.app.user.User;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "messages")
public class Message {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_message_sender"))
    private User sender;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_message_recipient"))
    private User recipient;

    @Column(nullable = false, length = 2000)
    private String content;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "read_at")
    private Instant readAt;

    // Optional post reference (interest flow): OFFER/REQUEST + post id.
    @Enumerated(EnumType.STRING)
    @Column(name = "context_type", length = 20)
    private ContextType contextType;

    @Column(name = "context_id")
    private UUID contextId;

    @PrePersist
    void onCreate() { this.createdAt = Instant.now(); }

    public UUID getId() { return id; }
    public User getSender() { return sender; }
    public User getRecipient() { return recipient; }
    public String getContent() { return content; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getReadAt() { return readAt; }
    public ContextType getContextType() { return contextType; }
    public UUID getContextId() { return contextId; }

    public void setSender(User sender) { this.sender = sender; }
    public void setRecipient(User recipient) { this.recipient = recipient; }
    public void setContent(String content) { this.content = content; }
    public void setReadAt(Instant readAt) { this.readAt = readAt; }
    public void setContextType(ContextType contextType) { this.contextType = contextType; }
    public void setContextId(UUID contextId) { this.contextId = contextId; }

    public enum ContextType { OFFER, REQUEST }
}
