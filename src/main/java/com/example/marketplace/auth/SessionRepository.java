package com.example.marketplace.auth;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SessionRepository extends JpaRepository<Session, UUID> {
    Optional<Session> findByToken(String token);
    void deleteByUser_Id(UUID userId);
}
