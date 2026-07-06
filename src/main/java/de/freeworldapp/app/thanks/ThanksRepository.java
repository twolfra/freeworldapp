package de.freeworldapp.app.thanks;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ThanksRepository extends JpaRepository<Thanks, UUID> {

    boolean existsByOfferId(UUID offerId);

    List<Thanks> findByToUser_IdOrderByCreatedAtDesc(UUID toUserId);

    List<Thanks> findByFromUser_IdOrderByCreatedAtDesc(UUID fromUserId);

    void deleteByFromUser_IdOrToUser_Id(UUID fromUserId, UUID toUserId);
}
