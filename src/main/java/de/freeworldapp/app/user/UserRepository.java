package de.freeworldapp.app.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    Optional<User> findByEmailIgnoreCase(String email);
    Optional<User> findByVerificationToken(String token);
    Optional<User> findByUnsubscribeToken(String token);

    @Modifying
    @Query("UPDATE User u SET u.verificationToken = null, u.verificationTokenExpiresAt = null " +
           "WHERE u.verificationTokenExpiresAt < :cutoff")
    int clearExpiredVerificationTokens(@Param("cutoff") java.time.Instant cutoff);
}
