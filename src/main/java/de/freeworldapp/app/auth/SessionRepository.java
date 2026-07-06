package de.freeworldapp.app.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface SessionRepository extends JpaRepository<Session, UUID> {

    Optional<Session> findByTokenHash(String tokenHash);

    /** Eagerly loads the owning user so AuthFilter can read blocked state outside an open session. */
    @Query("SELECT s FROM Session s JOIN FETCH s.user WHERE s.tokenHash = :tokenHash")
    Optional<Session> findByTokenHashWithUser(@Param("tokenHash") String tokenHash);

    /** Look up by the raw token a client presented — only the hash ever touches the DB. */
    default Optional<Session> findByRawToken(String rawToken) {
        return findByTokenHash(Tokens.sha256(rawToken));
    }

    default Optional<Session> findByRawTokenWithUser(String rawToken) {
        return findByTokenHashWithUser(Tokens.sha256(rawToken));
    }

    void deleteByUser_Id(UUID userId);

    @Modifying
    @Query("DELETE FROM Session s WHERE s.expiresAt < :cutoff")
    int deleteByExpiresAtBefore(@Param("cutoff") java.time.Instant cutoff);

    void deleteByUser_IdAndTokenHashNot(UUID userId, String tokenHash);
}
