package com.thinkcode.transportbackend.repository;

import com.thinkcode.transportbackend.entity.Debt;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

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

    Optional<Debt> findByIdAndVehicleCompanyId(UUID id, UUID companyId);
}

