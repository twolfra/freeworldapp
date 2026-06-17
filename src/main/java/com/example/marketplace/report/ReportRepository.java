package com.example.marketplace.report;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ReportRepository extends JpaRepository<Report, UUID> {

    @Query("SELECT r FROM Report r JOIN FETCH r.reporter WHERE r.status = :status ORDER BY r.createdAt DESC")
    List<Report> findByStatusOrderByCreatedAtDesc(@Param("status") Report.Status status);

    @Query("SELECT r FROM Report r JOIN FETCH r.reporter ORDER BY r.createdAt DESC")
    List<Report> findAllWithReporter();

    long countByStatus(Report.Status status);

    boolean existsByReporter_IdAndTargetTypeAndTargetIdAndStatus(
            UUID reporterId, Report.TargetType targetType, UUID targetId, Report.Status status);

    @Modifying
    @Query("DELETE FROM Report r WHERE r.reporter.id = :userId")
    void deleteAllByReporterId(@Param("userId") UUID userId);

    @Modifying
    @Query("DELETE FROM Report r WHERE r.targetType = :targetType AND r.targetId = :targetId")
    void deleteAllByTargetTypeAndTargetId(@Param("targetType") Report.TargetType targetType, @Param("targetId") UUID targetId);
}
