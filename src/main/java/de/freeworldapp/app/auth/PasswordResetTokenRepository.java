package de.freeworldapp.app.auth;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {

    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    default Optional<PasswordResetToken> findByRawToken(String rawToken) {
        return findByTokenHash(Tokens.sha256(rawToken));
    }

    void deleteByUser_Id(UUID userId);

    void deleteByExpiresAtBefore(Instant cutoff);
}
