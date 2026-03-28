package com.thinkcode.transportbackend.repository;

import com.thinkcode.transportbackend.entity.DriverAbsence;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DriverAbsenceRepository extends JpaRepository<DriverAbsence, UUID> {

    List<DriverAbsence> findAllByCompanyIdAndStartDateBetweenOrderByStartDateAsc(UUID companyId, LocalDate startDate, LocalDate endDate);

    Optional<DriverAbsence> findByIdAndCompanyId(UUID absenceId, UUID companyId);
}
