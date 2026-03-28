package com.thinkcode.transportbackend.repository;

import com.thinkcode.transportbackend.entity.MaintenanceRecord;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MaintenanceRecordRepository extends JpaRepository<MaintenanceRecord, UUID> {

    List<MaintenanceRecord> findAllByVehicleCompanyId(UUID companyId);

    List<MaintenanceRecord> findAllByVehicleCompanyIdAndMaintenanceDateBetween(
            UUID companyId,
            LocalDate startDate,
            LocalDate endDate
    );

    Optional<MaintenanceRecord> findByIdAndVehicleCompanyId(UUID id, UUID companyId);
}

