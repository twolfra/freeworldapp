package com.example.marketplace.report;

import com.example.marketplace.user.User;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "reports")
public class Report {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id", nullable = false)
    private User reporter;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private TargetType targetType;

    @Column(nullable = false)
    private UUID targetId;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private Reason reason;

    @Column(length = 1000)
    private String note;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private Status status = Status.OPEN;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = true)
    private UUID resolvedBy;

    @Column(nullable = true)
    private Instant resolvedAt;

    @PrePersist
    void onCreate() { this.createdAt = Instant.now(); }

    public UUID getId() { return id; }
    public User getReporter() { return reporter; }
    public TargetType getTargetType() { return targetType; }
    public UUID getTargetId() { return targetId; }
    public Reason getReason() { return reason; }
    public String getNote() { return note; }
    public Status getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public UUID getResolvedBy() { return resolvedBy; }
    public Instant getResolvedAt() { return resolvedAt; }

    public void setReporter(User reporter) { this.reporter = reporter; }
    public void setTargetType(TargetType targetType) { this.targetType = targetType; }
    public void setTargetId(UUID targetId) { this.targetId = targetId; }
    public void setReason(Reason reason) { this.reason = reason; }
    public void setNote(String note) { this.note = note; }
    public void setStatus(Status status) { this.status = status; }
    public void setResolvedBy(UUID resolvedBy) { this.resolvedBy = resolvedBy; }
    public void setResolvedAt(Instant resolvedAt) { this.resolvedAt = resolvedAt; }

    public enum TargetType { OFFER, REQUEST, USER }

    public enum Reason { SPAM, INAPPROPRIATE, SCAM, HARASSMENT, OTHER }

    public enum Status { OPEN, RESOLVED, DISMISSED }
}
