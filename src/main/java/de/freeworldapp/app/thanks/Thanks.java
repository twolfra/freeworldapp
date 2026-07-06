package de.freeworldapp.app.thanks;

import de.freeworldapp.app.user.User;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * One qualitative thank-you per completed gift — deliberately no score,
 * no rating, no ranking (gift economy). offerId/offerTitle are denormalized
 * so the entry survives offer deletion.
 */
@Entity
@Table(name = "thanks", uniqueConstraints = {
        @UniqueConstraint(name = "uk_thanks_offer", columnNames = "offer_id")
})
public class Thanks {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "from_user_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_thanks_from"))
    private User fromUser;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "to_user_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_thanks_to"))
    private User toUser;

    @Column(name = "offer_id", nullable = false)
    private UUID offerId;

    @Column(name = "offer_title", nullable = false, length = 140)
    private String offerTitle;

    @Column(length = 280)
    private String text;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() { this.createdAt = Instant.now(); }

    public UUID getId() { return id; }
    public User getFromUser() { return fromUser; }
    public User getToUser() { return toUser; }
    public UUID getOfferId() { return offerId; }
    public String getOfferTitle() { return offerTitle; }
    public String getText() { return text; }
    public Instant getCreatedAt() { return createdAt; }

    public void setFromUser(User fromUser) { this.fromUser = fromUser; }
    public void setToUser(User toUser) { this.toUser = toUser; }
    public void setOfferId(UUID offerId) { this.offerId = offerId; }
    public void setOfferTitle(String offerTitle) { this.offerTitle = offerTitle; }
    public void setText(String text) { this.text = text; }
}
