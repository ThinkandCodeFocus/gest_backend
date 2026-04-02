package com.thinkcode.transportbackend.repository;

import com.thinkcode.transportbackend.entity.Debt;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DebtRepository extends JpaRepository<Debt, UUID> {

    List<Debt> findAllByVehicleCompanyId(UUID companyId);

    List<Debt> findAllByVehicleCompanyIdAndDebtDateBetween(
            UUID companyId,
            LocalDate startDate,
            LocalDate endDate
    );

    List<Debt> findAllByVehicleCompanyIdAndVehicleClientIdAndDebtDateBetween(
            UUID companyId,
            UUID clientId,
            LocalDate startDate,
            LocalDate endDate
    );

    List<Debt> findAllByVehicleIdInAndDebtDateBetween(List<UUID> vehicleIds, LocalDate startDate, LocalDate endDate);

    Optional<Debt> findByIdAndVehicleCompanyId(UUID id, UUID companyId);

    @Query("SELECT d FROM Debt d WHERE d.vehicle.company.id = :companyId " +
           "AND d.vehicle.id IN :vehicleIds " +
           "AND d.debtDate BETWEEN :startDate AND :endDate")
    List<Debt> findByCompanyIdAndVehicleIdInAndDateRange(
            @Param("companyId") UUID companyId,
            @Param("vehicleIds") List<UUID> vehicleIds,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("SELECT d FROM Debt d WHERE d.vehicle.company.id = :companyId " +
           "AND d.vehicle.id = :vehicleId " +
           "AND d.debtDate BETWEEN :startDate AND :endDate")
    List<Debt> findByCompanyIdAndVehicleIdAndDateRange(
            @Param("companyId") UUID companyId,
            @Param("vehicleId") UUID vehicleId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}

