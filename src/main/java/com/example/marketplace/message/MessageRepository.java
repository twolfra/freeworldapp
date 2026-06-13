package com.example.marketplace.message;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

public interface MessageRepository extends JpaRepository<Message, UUID> {

    @Query("SELECT m FROM Message m JOIN FETCH m.sender JOIN FETCH m.recipient " +
           "WHERE (m.sender.id = :a AND m.recipient.id = :b) OR (m.sender.id = :b AND m.recipient.id = :a) " +
           "ORDER BY m.createdAt ASC")
    List<Message> findConversation(@Param("a") UUID a, @Param("b") UUID b);

    @Query("SELECT m FROM Message m JOIN FETCH m.sender JOIN FETCH m.recipient " +
           "WHERE m.sender.id = :userId OR m.recipient.id = :userId " +
           "ORDER BY m.createdAt DESC")
    List<Message> findAllForUser(@Param("userId") UUID userId);

    @Modifying
    @Query("UPDATE Message m SET m.readAt = CURRENT_TIMESTAMP WHERE m.recipient.id = :userId AND m.sender.id = :otherId AND m.readAt IS NULL")
    int markConversationRead(@Param("userId") UUID userId, @Param("otherId") UUID otherId);

    @Query("SELECT COUNT(m) FROM Message m WHERE m.recipient.id = :userId AND m.readAt IS NULL")
    long countUnread(@Param("userId") UUID userId);

    @Query("SELECT COUNT(m) FROM Message m WHERE m.recipient.id = :userId AND m.sender.id = :otherId AND m.readAt IS NULL")
    long countUnreadFromSender(@Param("userId") UUID userId, @Param("otherId") UUID otherId);

    @Modifying
    @Transactional
    @Query("DELETE FROM Message m WHERE m.sender.id = :userId OR m.recipient.id = :userId")
    void deleteAllInvolvingUser(@Param("userId") UUID userId);
}
