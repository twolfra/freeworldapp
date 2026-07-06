package de.freeworldapp.app.auth;

import de.freeworldapp.app.user.User;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "sessions")
public class Session {

    @Id
    @GeneratedValue
    private UUID id;

    // SHA-256 hex of the bearer token — the raw token is never persisted.
    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = true)
    private Instant expiresAt;

    @PrePersist
    void onCreate() { this.createdAt = Instant.now(); }

    public UUID getId()              { return id; }
    public String getTokenHash()     { return tokenHash; }
    public User getUser()            { return user; }
    public Instant getCreatedAt()    { return createdAt; }
    public Instant getExpiresAt()    { return expiresAt; }
    public void setTokenHash(String tokenHash) { this.tokenHash = tokenHash; }
    public void setUser(User user)           { this.user = user; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
}
