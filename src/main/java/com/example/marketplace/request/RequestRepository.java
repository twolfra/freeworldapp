package com.example.marketplace.request;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface RequestRepository extends JpaRepository<Request, UUID> {
    List<Request> findByRequestedBy_Id(UUID userId);

    @Query("SELECT r FROM Request r JOIN FETCH r.requestedBy WHERE r.requestedBy.id IN :ids")
    List<Request> findByRequestedBy_IdIn(@Param("ids") List<UUID> ids);
}
