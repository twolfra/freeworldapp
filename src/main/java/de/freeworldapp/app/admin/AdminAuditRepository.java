package de.freeworldapp.app.admin;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AdminAuditRepository extends JpaRepository<AdminAuditEntry, UUID> {

    List<AdminAuditEntry> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
