package com.thinkcode.transportbackend.repository;

import com.thinkcode.transportbackend.entity.MaintenanceRecord;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MaintenanceRecordRepository extends JpaRepository<MaintenanceRecord, UUID> {

    List<MaintenanceRecord> findAllByVehicleCompanyId(UUID companyId);

    List<MaintenanceRecord> findAllByVehicleCompanyIdAndMaintenanceDateBetween(
            UUID companyId,
            LocalDate startDate,
            LocalDate endDate
    );

    @Query("SELECT mr FROM MaintenanceRecord mr WHERE mr.vehicle.company.id = :companyId " +
           "AND mr.vehicle.id = :vehicleId " +
           "AND mr.maintenanceDate BETWEEN :startDate AND :endDate " +
           "ORDER BY mr.maintenanceDate DESC")
    List<MaintenanceRecord> findRecentForVehicle(
            @Param("companyId") UUID companyId,
            @Param("vehicleId") UUID vehicleId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("SELECT mr FROM MaintenanceRecord mr WHERE mr.vehicle.company.id = :companyId " +
           "AND mr.vehicle.id = :vehicleId " +
           "AND mr.maintenanceDate BETWEEN :startDate AND :endDate " +
           "ORDER BY mr.maintenanceDate ASC")
    List<MaintenanceRecord> findByCompanyVehicleAndDateRange(
            @Param("companyId") UUID companyId,
            @Param("vehicleId") UUID vehicleId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    Optional<MaintenanceRecord> findByIdAndVehicleCompanyId(UUID id, UUID companyId);
}

