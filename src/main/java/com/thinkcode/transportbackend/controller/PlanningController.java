package com.thinkcode.transportbackend.controller;

import com.thinkcode.transportbackend.dto.DriverScheduleRequest;
import com.thinkcode.transportbackend.dto.MaintenanceScheduleRequest;
import com.thinkcode.transportbackend.entity.DriverSchedule;
import com.thinkcode.transportbackend.entity.MaintenanceSchedule;
import com.thinkcode.transportbackend.service.PlanningService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/planning")
public class PlanningController {

    private final PlanningService planningService;

    public PlanningController(PlanningService planningService) {
        this.planningService = planningService;
    }

    @GetMapping("/driver-schedules")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATIONS_MANAGER', 'ASSISTANT')")
    public List<DriverSchedule> findDriverSchedules(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        return planningService.findDriverSchedules(startDate, endDate);
    }

    @PostMapping("/driver-schedules")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATIONS_MANAGER')")
    public DriverSchedule createDriverSchedule(@Valid @RequestBody DriverScheduleRequest request) {
        return planningService.createDriverSchedule(request);
    }

    @PutMapping("/driver-schedules/{scheduleId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATIONS_MANAGER')")
    public DriverSchedule updateDriverSchedule(
            @PathVariable UUID scheduleId,
            @Valid @RequestBody DriverScheduleRequest request
    ) {
        return planningService.updateDriverSchedule(scheduleId, request);
    }

    @DeleteMapping("/driver-schedules/{scheduleId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATIONS_MANAGER')")
    public void deleteDriverSchedule(@PathVariable UUID scheduleId) {
        planningService.deleteDriverSchedule(scheduleId);
    }

    @GetMapping("/maintenance-schedules")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATIONS_MANAGER', 'ASSISTANT')")
    public List<MaintenanceSchedule> findMaintenanceSchedules(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        return planningService.findMaintenanceSchedules(startDate, endDate);
    }

    @PostMapping("/maintenance-schedules")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATIONS_MANAGER')")
    public MaintenanceSchedule createMaintenanceSchedule(@Valid @RequestBody MaintenanceScheduleRequest request) {
        return planningService.createMaintenanceSchedule(request);
    }

    @PutMapping("/maintenance-schedules/{scheduleId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATIONS_MANAGER')")
    public MaintenanceSchedule updateMaintenanceSchedule(
            @PathVariable UUID scheduleId,
            @Valid @RequestBody MaintenanceScheduleRequest request
    ) {
        return planningService.updateMaintenanceSchedule(scheduleId, request);
    }

    @DeleteMapping("/maintenance-schedules/{scheduleId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATIONS_MANAGER')")
    public void deleteMaintenanceSchedule(@PathVariable UUID scheduleId) {
        planningService.deleteMaintenanceSchedule(scheduleId);
    }
}
