package com.thinkcode.transportbackend.repository;

import com.thinkcode.transportbackend.entity.PlanningEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface PlanningEventRepository extends JpaRepository<PlanningEvent, UUID> {

    @Query("SELECT pe FROM PlanningEvent pe WHERE pe.company.id = :companyId " +
           "AND pe.eventDate BETWEEN :startDate AND :endDate " +
           "ORDER BY pe.eventDate ASC, pe.slot ASC")
    List<PlanningEvent> findByCompanyAndDateRange(UUID companyId, LocalDate startDate, LocalDate endDate);

    @Query("SELECT pe FROM PlanningEvent pe WHERE pe.company.id = :companyId " +
           "AND pe.type = :type AND pe.eventDate BETWEEN :startDate AND :endDate " +
           "ORDER BY pe.eventDate ASC")
    List<PlanningEvent> findByCompanyAndType(UUID companyId, String type, LocalDate startDate, LocalDate endDate);

    @Query("SELECT pe FROM PlanningEvent pe WHERE pe.company.id = :companyId " +
           "AND pe.owner.id = :userId AND pe.eventDate >= CURRENT_DATE " +
           "ORDER BY pe.eventDate ASC")
    List<PlanningEvent> findUserEvents(UUID companyId, UUID userId);
}
