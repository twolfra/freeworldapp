package de.freeworldapp.app.subscription;

import de.freeworldapp.app.user.User;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "subscriptions", uniqueConstraints = {
        @UniqueConstraint(name = "uk_subscription", columnNames = {"subscriber_id", "subscribed_to_id"})
})
public class Subscription {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "subscriber_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_subscription_subscriber"))
    private User subscriber;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "subscribed_to_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_subscription_subscribed_to"))
    private User subscribedTo;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() { this.createdAt = Instant.now(); }

    public UUID getId() { return id; }
    public User getSubscriber() { return subscriber; }
    public User getSubscribedTo() { return subscribedTo; }
    public Instant getCreatedAt() { return createdAt; }

    public void setSubscriber(User subscriber) { this.subscriber = subscriber; }
    public void setSubscribedTo(User subscribedTo) { this.subscribedTo = subscribedTo; }
}
