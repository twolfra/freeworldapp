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

    @Column(nullable = false, unique = true, length = 36)
    private String token;

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
    public String getToken()         { return token; }
    public User getUser()            { return user; }
    public Instant getCreatedAt()    { return createdAt; }
    public Instant getExpiresAt()    { return expiresAt; }
    public void setToken(String token)       { this.token = token; }
    public void setUser(User user)           { this.user = user; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
}
