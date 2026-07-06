package de.freeworldapp.app.request;

import de.freeworldapp.app.user.User;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "requests")
public class Request {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, length = 140)
    private String title;

    @Column(nullable = false, length = 4000)
    private String description;

    @Column(nullable = false, length = 140)
    private String region;

    @Column(nullable = false, length = 140)
    private String category;

    @Column(nullable = false)
    private Integer quantity;

    @Column(length = 500)
    private String imageUrl;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "requested_by_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_request_requested_by"))
    private User requestedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16, columnDefinition = "varchar(16) DEFAULT 'OPEN'")
    private Status status = Status.OPEN;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() { this.createdAt = Instant.now(); }

    public UUID getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getRegion() { return region; }
    public String getCategory() { return category; }
    public Integer getQuantity() { return quantity; }
    public String getImageUrl() { return imageUrl; }
    public User getRequestedBy() { return requestedBy; }
    public Instant getCreatedAt() { return createdAt; }
    public Status getStatus() { return status; }

    public void setTitle(String title) { this.title = title; }
    public void setDescription(String description) { this.description = description; }
    public void setRegion(String region) { this.region = region; }
    public void setCategory(String category) { this.category = category; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public void setRequestedBy(User requestedBy) { this.requestedBy = requestedBy; }
    public void setStatus(Status status) { this.status = status; }

    public enum Status { OPEN, FULFILLED }
}
