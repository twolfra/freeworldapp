package de.freeworldapp.app.user;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity

// specificaion of the constraints that define which columns may not have
// double entries (unique keys)

@Table(name = "users", uniqueConstraints = {
        @UniqueConstraint(name = "uk_users_username", columnNames = "username"),
        @UniqueConstraint(name = "uk_users_email", columnNames = "email")
})
public class User {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, length = 32)
    private String username;

    @Column(nullable = false, length = 255)
    private String email;

    @Column(nullable = false, length = 60) // bcrypt length
    private String passwordHash;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false, columnDefinition = "boolean DEFAULT false")
    private boolean emailVerified = false;

    @Column(nullable = true, length = 36)
    private String verificationToken;

    @Column(nullable = true)
    private Instant verificationTokenExpiresAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16, columnDefinition = "varchar(16) DEFAULT 'USER'")
    private Role role = Role.USER;

    @Column(nullable = false, columnDefinition = "boolean DEFAULT false")
    private boolean blocked = false;

    @Column(nullable = true)
    private Instant blockedAt;

    // Email notification preference for incoming direct messages.
    @Column(nullable = false, columnDefinition = "boolean DEFAULT true")
    private boolean notifyOnMessage = true;

    // Stable per-user secret used for login-free one-click unsubscribe links.
    @Column(nullable = true, length = 36)
    private String unsubscribeToken;

    // UI/email language preference (ISO code, e.g. "en" / "de").
    @Column(nullable = false, length = 8, columnDefinition = "varchar(8) DEFAULT 'en'")
    private String language = "en";

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now(); // sets current timestamp of creation
        if (this.unsubscribeToken == null) this.unsubscribeToken = UUID.randomUUID().toString();
    }

    public UUID getId() { return id; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public Instant getCreatedAt() { return createdAt; }
    public boolean isEmailVerified() { return emailVerified; }
    public String getVerificationToken() { return verificationToken; }
    public Instant getVerificationTokenExpiresAt() { return verificationTokenExpiresAt; }
    public Role getRole() { return role; }
    public boolean isBlocked() { return blocked; }
    public Instant getBlockedAt() { return blockedAt; }
    public boolean isNotifyOnMessage() { return notifyOnMessage; }
    public String getUnsubscribeToken() { return unsubscribeToken; }
    public String getLanguage() { return language; }

    public void setUsername(String username) { this.username = username; }
    public void setEmail(String email) { this.email = email; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public void setEmailVerified(boolean emailVerified) { this.emailVerified = emailVerified; }
    public void setVerificationToken(String verificationToken) { this.verificationToken = verificationToken; }
    public void setVerificationTokenExpiresAt(Instant verificationTokenExpiresAt) { this.verificationTokenExpiresAt = verificationTokenExpiresAt; }
    public void setRole(Role role) { this.role = role; }
    public void setBlocked(boolean blocked) { this.blocked = blocked; }
    public void setBlockedAt(Instant blockedAt) { this.blockedAt = blockedAt; }
    public void setNotifyOnMessage(boolean notifyOnMessage) { this.notifyOnMessage = notifyOnMessage; }
    public void setUnsubscribeToken(String unsubscribeToken) { this.unsubscribeToken = unsubscribeToken; }
    public void setLanguage(String language) { this.language = language; }
}
