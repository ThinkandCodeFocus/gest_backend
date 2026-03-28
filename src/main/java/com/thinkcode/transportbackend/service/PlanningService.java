package com.thinkcode.transportbackend.service;

import com.thinkcode.transportbackend.dto.DriverScheduleRequest;
import com.thinkcode.transportbackend.dto.MaintenanceScheduleRequest;
import com.thinkcode.transportbackend.entity.DriverSchedule;
import com.thinkcode.transportbackend.entity.MaintenanceSchedule;
import com.thinkcode.transportbackend.entity.ScheduleStatus;
import com.thinkcode.transportbackend.repository.DriverScheduleRepository;
import com.thinkcode.transportbackend.repository.MaintenanceScheduleRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class PlanningService {

    private final DriverScheduleRepository driverScheduleRepository;
    private final MaintenanceScheduleRepository maintenanceScheduleRepository;
    private final DriverService driverService;
    private final VehicleService vehicleService;
    private final CompanyResolver companyResolver;
    private final AuthenticatedCompanyProvider authenticatedCompanyProvider;
    private final AuditLogService auditLogService;

    public PlanningService(
            DriverScheduleRepository driverScheduleRepository,
            MaintenanceScheduleRepository maintenanceScheduleRepository,
            DriverService driverService,
            VehicleService vehicleService,
            CompanyResolver companyResolver,
            AuthenticatedCompanyProvider authenticatedCompanyProvider,
            AuditLogService auditLogService
    ) {
        this.driverScheduleRepository = driverScheduleRepository;
        this.maintenanceScheduleRepository = maintenanceScheduleRepository;
        this.driverService = driverService;
        this.vehicleService = vehicleService;
        this.companyResolver = companyResolver;
        this.authenticatedCompanyProvider = authenticatedCompanyProvider;
        this.auditLogService = auditLogService;
    }

    public List<DriverSchedule> findDriverSchedules(LocalDate startDate, LocalDate endDate) {
        UUID companyId = authenticatedCompanyProvider.requireCompanyId();
        return driverScheduleRepository.findAllByCompanyIdAndScheduleDateBetweenOrderByScheduleDateAsc(companyId, startDate, endDate);
    }

    public DriverSchedule createDriverSchedule(DriverScheduleRequest request) {
        UUID companyId = authenticatedCompanyProvider.requireCompanyId();
        DriverSchedule schedule = new DriverSchedule();
        schedule.setCompany(companyResolver.require(companyId));
        schedule.setDriver(driverService.findByIdForCompany(request.driverId(), companyId));
        schedule.setScheduleDate(request.scheduleDate());
        schedule.setShiftStart(request.shiftStart());
        schedule.setShiftEnd(request.shiftEnd());
        schedule.setStatus(request.status() == null ? ScheduleStatus.PLANNED : request.status());
        schedule.setNote(request.note());
        DriverSchedule saved = driverScheduleRepository.save(schedule);
        auditLogService.log("CREATE", "PLANNING", saved.getId().toString(), null, "driverSchedule");
        return saved;
    }

    public DriverSchedule updateDriverSchedule(UUID scheduleId, DriverScheduleRequest request) {
        UUID companyId = authenticatedCompanyProvider.requireCompanyId();
        DriverSchedule schedule = driverScheduleRepository.findByIdAndCompanyId(scheduleId, companyId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Driver schedule not found"));
        String before = schedule.getScheduleDate() + "|" + schedule.getStatus();

        schedule.setDriver(driverService.findByIdForCompany(request.driverId(), companyId));
        schedule.setScheduleDate(request.scheduleDate());
        schedule.setShiftStart(request.shiftStart());
        schedule.setShiftEnd(request.shiftEnd());
        schedule.setStatus(request.status() == null ? ScheduleStatus.PLANNED : request.status());
        schedule.setNote(request.note());
        DriverSchedule saved = driverScheduleRepository.save(schedule);
        auditLogService.log("UPDATE", "PLANNING", saved.getId().toString(), before, saved.getScheduleDate() + "|" + saved.getStatus());
        return saved;
    }

    public void deleteDriverSchedule(UUID scheduleId) {
        UUID companyId = authenticatedCompanyProvider.requireCompanyId();
        DriverSchedule schedule = driverScheduleRepository.findByIdAndCompanyId(scheduleId, companyId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Driver schedule not found"));
        auditLogService.log("DELETE", "PLANNING", schedule.getId().toString(), schedule.getScheduleDate().toString(), null);
        driverScheduleRepository.delete(schedule);
    }

    public List<MaintenanceSchedule> findMaintenanceSchedules(LocalDate startDate, LocalDate endDate) {
        UUID companyId = authenticatedCompanyProvider.requireCompanyId();
        return maintenanceScheduleRepository.findAllByCompanyIdAndPlannedDateBetweenOrderByPlannedDateAsc(companyId, startDate, endDate);
    }

    public MaintenanceSchedule createMaintenanceSchedule(MaintenanceScheduleRequest request) {
        UUID companyId = authenticatedCompanyProvider.requireCompanyId();
        MaintenanceSchedule schedule = new MaintenanceSchedule();
        schedule.setCompany(companyResolver.require(companyId));
        schedule.setVehicle(vehicleService.findByIdForCompany(request.vehicleId(), companyId));
        schedule.setPlannedDate(request.plannedDate());
        schedule.setStatus(request.status() == null ? ScheduleStatus.PLANNED : request.status());
        schedule.setNote(request.note());
        MaintenanceSchedule saved = maintenanceScheduleRepository.save(schedule);
        auditLogService.log("CREATE", "PLANNING", saved.getId().toString(), null, "maintenanceSchedule");
        return saved;
    }

    public MaintenanceSchedule updateMaintenanceSchedule(UUID scheduleId, MaintenanceScheduleRequest request) {
        UUID companyId = authenticatedCompanyProvider.requireCompanyId();
        MaintenanceSchedule schedule = maintenanceScheduleRepository.findByIdAndCompanyId(scheduleId, companyId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Maintenance schedule not found"));
        String before = schedule.getPlannedDate() + "|" + schedule.getStatus();

        schedule.setVehicle(vehicleService.findByIdForCompany(request.vehicleId(), companyId));
        schedule.setPlannedDate(request.plannedDate());
        schedule.setStatus(request.status() == null ? ScheduleStatus.PLANNED : request.status());
        schedule.setNote(request.note());
        MaintenanceSchedule saved = maintenanceScheduleRepository.save(schedule);
        auditLogService.log("UPDATE", "PLANNING", saved.getId().toString(), before, saved.getPlannedDate() + "|" + saved.getStatus());
        return saved;
    }

    public void deleteMaintenanceSchedule(UUID scheduleId) {
        UUID companyId = authenticatedCompanyProvider.requireCompanyId();
        MaintenanceSchedule schedule = maintenanceScheduleRepository.findByIdAndCompanyId(scheduleId, companyId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Maintenance schedule not found"));
        auditLogService.log("DELETE", "PLANNING", schedule.getId().toString(), schedule.getPlannedDate().toString(), null);
        maintenanceScheduleRepository.delete(schedule);
    }
}
