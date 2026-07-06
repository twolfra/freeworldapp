package de.freeworldapp.app.notification;

import de.freeworldapp.app.user.User;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notifications")
public class Notification {

    public enum Type { NEW_MESSAGE, NEW_POST_FROM_SUB, INTEREST, THANKS, ADMIN_NOTICE }

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_notification_user"))
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Type type;

    /** JSON blob with type-specific display data (ids, titles, usernames). */
    @Column(columnDefinition = "text")
    private String payload;

    @Column(name = "read_at")
    private Instant readAt;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() { this.createdAt = Instant.now(); }

    public UUID getId() { return id; }
    public User getUser() { return user; }
    public Type getType() { return type; }
    public String getPayload() { return payload; }
    public Instant getReadAt() { return readAt; }
    public Instant getCreatedAt() { return createdAt; }

    public void setUser(User user) { this.user = user; }
    public void setType(Type type) { this.type = type; }
    public void setPayload(String payload) { this.payload = payload; }
    public void setReadAt(Instant readAt) { this.readAt = readAt; }
}
