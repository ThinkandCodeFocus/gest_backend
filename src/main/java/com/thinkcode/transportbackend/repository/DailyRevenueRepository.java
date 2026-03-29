package com.thinkcode.transportbackend.repository;

import com.thinkcode.transportbackend.entity.DailyRevenue;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DailyRevenueRepository extends JpaRepository<DailyRevenue, UUID> {

    List<DailyRevenue> findAllByVehicleCompanyId(UUID companyId);

    List<DailyRevenue> findAllByVehicleCompanyIdAndVehicleClientIdAndRevenueDateBetween(
            UUID companyId,
            UUID clientId,
            LocalDate startDate,
            LocalDate endDate
    );

    List<DailyRevenue> findAllByVehicleCompanyIdAndRevenueDateBetween(
            UUID companyId,
            LocalDate startDate,
            LocalDate endDate
    );

    Optional<DailyRevenue> findByIdAndVehicleCompanyId(UUID id, UUID companyId);

    boolean existsByVehicleIdAndRevenueDate(UUID vehicleId, LocalDate revenueDate);

    @Query("SELECT dr FROM DailyRevenue dr WHERE dr.vehicle.company.id = :companyId " +
           "AND dr.vehicle.id IN :vehicleIds " +
           "AND dr.revenueDate BETWEEN :startDate AND :endDate")
    List<DailyRevenue> findByCompanyIdAndVehicleIdInAndDateRange(
            @Param("companyId") UUID companyId,
            @Param("vehicleIds") List<UUID> vehicleIds,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("SELECT dr FROM DailyRevenue dr WHERE dr.vehicle.company.id = :companyId " +
           "AND dr.vehicle.id = :vehicleId " +
           "AND dr.revenueDate BETWEEN :startDate AND :endDate")
    List<DailyRevenue> findByCompanyIdAndVehicleIdAndDateRange(
            @Param("companyId") UUID companyId,
            @Param("vehicleId") UUID vehicleId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}

