package com.thinkcode.transportbackend.service;

import com.thinkcode.transportbackend.dto.DriverAbsenceRequest;
import com.thinkcode.transportbackend.dto.DriverAbsenceResponse;
import com.thinkcode.transportbackend.entity.Driver;
import com.thinkcode.transportbackend.entity.DriverAbsence;
import com.thinkcode.transportbackend.entity.RoleName;
import com.thinkcode.transportbackend.repository.DriverAbsenceRepository;
import com.thinkcode.transportbackend.repository.DriverRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class HrService {

    private final DriverAbsenceRepository driverAbsenceRepository;
    private final DriverService driverService;
    private final DriverRepository driverRepository;
    private final CompanyResolver companyResolver;
    private final AuthenticatedCompanyProvider authenticatedCompanyProvider;
    private final AuthenticatedUserProvider authenticatedUserProvider;
    private final AuditLogService auditLogService;

    public HrService(
            DriverAbsenceRepository driverAbsenceRepository,
            DriverService driverService,
            DriverRepository driverRepository,
            CompanyResolver companyResolver,
            AuthenticatedCompanyProvider authenticatedCompanyProvider,
            AuthenticatedUserProvider authenticatedUserProvider,
            AuditLogService auditLogService
    ) {
        this.driverAbsenceRepository = driverAbsenceRepository;
        this.driverService = driverService;
        this.driverRepository = driverRepository;
        this.companyResolver = companyResolver;
        this.authenticatedCompanyProvider = authenticatedCompanyProvider;
        this.authenticatedUserProvider = authenticatedUserProvider;
        this.auditLogService = auditLogService;
    }

    public List<DriverAbsenceResponse> findAbsences(LocalDate startDate, LocalDate endDate) {
        UUID companyId = authenticatedCompanyProvider.requireCompanyId();
        return driverAbsenceRepository.findAllByCompanyIdAndStartDateBetweenOrderByStartDateAsc(companyId, startDate, endDate)
                .stream()
                .map(this::mapAbsenceResponse)
                .toList();
    }

    public DriverAbsenceResponse createAbsence(DriverAbsenceRequest request) {
        UUID companyId = authenticatedCompanyProvider.requireCompanyId();
        RoleName currentRole = authenticatedUserProvider.requireUser().getRole();
        if (request.endDate().isBefore(request.startDate())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "endDate must be greater than or equal to startDate");
        }

        Driver driver = driverService.findByIdForCompany(request.driverId(), companyId);
        assertAbsenceScope(currentRole, driver);

        DriverAbsence absence = new DriverAbsence();
        absence.setCompany(companyResolver.require(companyId));
        absence.setDriver(driver);
        absence.setStartDate(request.startDate());
        absence.setEndDate(request.endDate());
        absence.setReason(request.reason());
        absence.setApproved(currentRole == RoleName.ASSISTANT ? false : request.approved());
        DriverAbsence saved = driverAbsenceRepository.save(absence);

        driver.setStatus("Absent");
        driverRepository.save(driver);

        auditLogService.log("CREATE", "HR", saved.getId().toString(), null, "absence|" + driver.getFullName() + "|" + saved.isApproved());
        return mapAbsenceResponse(saved);
    }

    public DriverAbsenceResponse updateAbsence(UUID absenceId, DriverAbsenceRequest request) {
        UUID companyId = authenticatedCompanyProvider.requireCompanyId();
        DriverAbsence absence = driverAbsenceRepository.findByIdAndCompanyId(absenceId, companyId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Absence not found"));
        String before = absence.getStartDate() + "|" + absence.getEndDate() + "|" + absence.isApproved();

        Driver driver = driverService.findByIdForCompany(request.driverId(), companyId);
        absence.setDriver(driver);
        absence.setStartDate(request.startDate());
        absence.setEndDate(request.endDate());
        absence.setReason(request.reason());
        absence.setApproved(request.approved());
        DriverAbsence saved = driverAbsenceRepository.save(absence);

        driver.setStatus("Absent");
        driverRepository.save(driver);

        auditLogService.log("UPDATE", "HR", saved.getId().toString(), before, saved.getStartDate() + "|" + saved.getEndDate() + "|" + saved.isApproved());
        return mapAbsenceResponse(saved);
    }

    public void deleteAbsence(UUID absenceId) {
        UUID companyId = authenticatedCompanyProvider.requireCompanyId();
        DriverAbsence absence = driverAbsenceRepository.findByIdAndCompanyId(absenceId, companyId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Absence not found"));
        auditLogService.log("DELETE", "HR", absence.getId().toString(), absence.getReason(), null);
        driverAbsenceRepository.delete(absence);
    }

    private void assertAbsenceScope(RoleName currentRole, Driver driver) {
        if (currentRole == RoleName.ASSISTANT && driver.getRole() != RoleName.DRIVER) {
            throw new ApiException(HttpStatus.FORBIDDEN, "An assistant can only declare absences for drivers");
        }
        if (currentRole == RoleName.OPERATIONS_MANAGER && driver.getRole() != RoleName.DRIVER && driver.getRole() != RoleName.ASSISTANT) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Operations manager can only manage driver and assistant absences here");
        }
    }

    private DriverAbsenceResponse mapAbsenceResponse(DriverAbsence absence) {
        return new DriverAbsenceResponse(
                absence.getId(),
                absence.getDriver().getId(),
                absence.getDriver().getFullName(),
                absence.getStartDate(),
                absence.getEndDate(),
                absence.getReason(),
                absence.isApproved()
        );
    }
}
