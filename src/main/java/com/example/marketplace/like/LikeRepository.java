package com.example.marketplace.like;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LikeRepository extends JpaRepository<Like, UUID> {
    Optional<Like> findByUserIdAndTargetTypeAndTargetId(UUID userId, Like.TargetType targetType, UUID targetId);

    boolean existsByUserIdAndTargetTypeAndTargetId(UUID userId, Like.TargetType targetType, UUID targetId);

    @Query("SELECT l FROM Like l WHERE l.user.id = :userId AND l.targetType = :targetType ORDER BY l.createdAt DESC")
    List<Like> findByUserIdAndTargetType(@Param("userId") UUID userId, @Param("targetType") Like.TargetType targetType);

    long countByTargetTypeAndTargetId(Like.TargetType targetType, UUID targetId);

    @Modifying
    @Query("DELETE FROM Like l WHERE l.user.id = :userId")
    void deleteAllByUserId(@Param("userId") UUID userId);

    @Modifying
    @Query("DELETE FROM Like l WHERE l.targetType = :targetType AND l.targetId = :targetId")
    void deleteAllByTargetTypeAndTargetId(@Param("targetType") Like.TargetType targetType, @Param("targetId") UUID targetId);
}
