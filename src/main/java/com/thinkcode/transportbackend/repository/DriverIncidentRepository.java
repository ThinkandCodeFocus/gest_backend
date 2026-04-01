package com.thinkcode.transportbackend.repository;

import com.thinkcode.transportbackend.entity.DriverIncident;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DriverIncidentRepository extends JpaRepository<DriverIncident, UUID> {

    List<DriverIncident> findAllByCompanyIdAndDriverIdOrderByCreatedAtDesc(UUID companyId, UUID driverId);

    long countByCompanyIdAndDriverIdAndCreatedAtAfter(UUID companyId, UUID driverId, Instant createdAt);
}
