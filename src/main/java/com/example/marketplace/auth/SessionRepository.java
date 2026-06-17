package com.example.marketplace.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface SessionRepository extends JpaRepository<Session, UUID> {
    Optional<Session> findByToken(String token);

    /** Eagerly loads the owning user so AuthFilter can read blocked state outside an open session. */
    @Query("SELECT s FROM Session s JOIN FETCH s.user WHERE s.token = :token")
    Optional<Session> findByTokenWithUser(@Param("token") String token);

    void deleteByUser_Id(UUID userId);
}
