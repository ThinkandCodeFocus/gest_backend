package com.thinkcode.transportbackend.repository;

import com.thinkcode.transportbackend.entity.DailyRevenue;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

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
}

