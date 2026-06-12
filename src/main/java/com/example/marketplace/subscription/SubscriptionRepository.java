package com.example.marketplace.subscription;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {
    boolean existsBySubscriber_IdAndSubscribedTo_Id(UUID subscriberId, UUID subscribedToId);
    Optional<Subscription> findBySubscriber_IdAndSubscribedTo_Id(UUID subscriberId, UUID subscribedToId);
    List<Subscription> findBySubscriber_Id(UUID subscriberId);
}
