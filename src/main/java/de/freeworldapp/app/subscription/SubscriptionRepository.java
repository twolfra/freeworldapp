package de.freeworldapp.app.subscription;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {
    boolean existsBySubscriber_IdAndSubscribedTo_Id(UUID subscriberId, UUID subscribedToId);
    Optional<Subscription> findBySubscriber_IdAndSubscribedTo_Id(UUID subscriberId, UUID subscribedToId);
    List<Subscription> findBySubscriber_Id(UUID subscriberId);

    @Modifying
    @Transactional
    @Query("DELETE FROM Subscription s WHERE s.subscriber.id = :userId OR s.subscribedTo.id = :userId")
    void deleteAllInvolvingUser(@Param("userId") UUID userId);
}
