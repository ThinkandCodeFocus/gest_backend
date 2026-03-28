package com.thinkcode.transportbackend.repository;

import com.thinkcode.transportbackend.entity.DriverSchedule;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DriverScheduleRepository extends JpaRepository<DriverSchedule, UUID> {

    List<DriverSchedule> findAllByCompanyIdAndScheduleDateBetweenOrderByScheduleDateAsc(UUID companyId, LocalDate startDate, LocalDate endDate);

    Optional<DriverSchedule> findByIdAndCompanyId(UUID scheduleId, UUID companyId);
}
