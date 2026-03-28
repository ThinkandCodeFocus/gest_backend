package com.thinkcode.transportbackend.repository;

import com.thinkcode.transportbackend.entity.AuditLog;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    List<AuditLog> findAllByCompanyIdAndCreatedAtBetweenOrderByCreatedAtDesc(UUID companyId, Instant from, Instant to);

    List<AuditLog> findAllByCompanyIdOrderByCreatedAtDesc(UUID companyId);
}
