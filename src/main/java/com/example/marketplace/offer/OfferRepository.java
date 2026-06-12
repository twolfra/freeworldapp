package com.example.marketplace.offer;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface OfferRepository extends JpaRepository<Offer, UUID> {
    List<Offer> findByOfferedBy_Id(UUID userId);

    @Query("SELECT o FROM Offer o JOIN FETCH o.offeredBy WHERE o.offeredBy.id IN :ids")
    List<Offer> findByOfferedBy_IdIn(@Param("ids") List<UUID> ids);
}
