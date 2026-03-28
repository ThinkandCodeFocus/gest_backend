package com.thinkcode.transportbackend.repository;

import com.thinkcode.transportbackend.entity.MaintenanceSchedule;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MaintenanceScheduleRepository extends JpaRepository<MaintenanceSchedule, UUID> {

    List<MaintenanceSchedule> findAllByCompanyIdAndPlannedDateBetweenOrderByPlannedDateAsc(UUID companyId, LocalDate startDate, LocalDate endDate);

    Optional<MaintenanceSchedule> findByIdAndCompanyId(UUID scheduleId, UUID companyId);
}
