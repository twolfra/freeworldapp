package de.freeworldapp.app.admin;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Immutable record of an admin action. adminId/adminUsername are denormalized
 * (no FK) so the log survives account deletion.
 */
@Entity
@Table(name = "admin_audit_log")
public class AdminAuditEntry {

    public enum Action {
        BLOCK_USER, UNBLOCK_USER, DELETE_USER,
        DELETE_OFFER, DELETE_REQUEST,
        RESOLVE_REPORT, DISMISS_REPORT
    }

    public enum TargetType { USER, OFFER, REQUEST, REPORT }

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "admin_id", nullable = false)
    private UUID adminId;

    @Column(name = "admin_username", nullable = false, length = 32)
    private String adminUsername;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private Action action;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TargetType targetType;

    @Column(nullable = true)
    private UUID targetId;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() { this.createdAt = Instant.now(); }

    public UUID getId() { return id; }
    public UUID getAdminId() { return adminId; }
    public String getAdminUsername() { return adminUsername; }
    public Action getAction() { return action; }
    public TargetType getTargetType() { return targetType; }
    public UUID getTargetId() { return targetId; }
    public Instant getCreatedAt() { return createdAt; }

    public void setAdminId(UUID adminId) { this.adminId = adminId; }
    public void setAdminUsername(String adminUsername) { this.adminUsername = adminUsername; }
    public void setAction(Action action) { this.action = action; }
    public void setTargetType(TargetType targetType) { this.targetType = targetType; }
    public void setTargetId(UUID targetId) { this.targetId = targetId; }
}
