package com.thinkcode.transportbackend.service;

import com.thinkcode.transportbackend.dto.DebtResponse;
import com.thinkcode.transportbackend.dto.DriverIncidentRequest;
import com.thinkcode.transportbackend.dto.DriverIncidentResponse;
import com.thinkcode.transportbackend.dto.DriverProductivityResponse;
import com.thinkcode.transportbackend.dto.DriverRequest;
import com.thinkcode.transportbackend.dto.DriverResponse;
import com.thinkcode.transportbackend.dto.DriverSelfOverviewResponse;
import com.thinkcode.transportbackend.entity.ActivityStatus;
import com.thinkcode.transportbackend.entity.DailyRevenue;
import com.thinkcode.transportbackend.entity.Driver;
import com.thinkcode.transportbackend.entity.DriverIncident;
import com.thinkcode.transportbackend.entity.NotificationType;
import com.thinkcode.transportbackend.entity.RoleName;
import com.thinkcode.transportbackend.entity.UserAccount;
import com.thinkcode.transportbackend.entity.Vehicle;
import com.thinkcode.transportbackend.repository.DailyRevenueRepository;
import com.thinkcode.transportbackend.repository.DebtRepository;
import com.thinkcode.transportbackend.repository.DriverAbsenceRepository;
import com.thinkcode.transportbackend.repository.DriverIncidentRepository;
import com.thinkcode.transportbackend.repository.DriverRepository;
import com.thinkcode.transportbackend.repository.UserAccountRepository;
import com.thinkcode.transportbackend.repository.VehicleRepository;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class DriverService {

    private final DriverRepository driverRepository;
    private final CompanyResolver companyResolver;
    private final AuthenticatedCompanyProvider authenticatedCompanyProvider;
    private final AuthenticatedUserProvider authenticatedUserProvider;
    private final VehicleRepository vehicleRepository;
    private final DriverAbsenceRepository driverAbsenceRepository;
    private final DriverIncidentRepository driverIncidentRepository;
    private final DebtRepository debtRepository;
    private final DailyRevenueRepository dailyRevenueRepository;
    private final NotificationService notificationService;
    private final DebtService debtService;
    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;

    private static final String DEFAULT_COLLABORATOR_PASSWORD = "demo1234";

    public DriverService(
            DriverRepository driverRepository,
            CompanyResolver companyResolver,
            AuthenticatedCompanyProvider authenticatedCompanyProvider,
            AuthenticatedUserProvider authenticatedUserProvider,
            VehicleRepository vehicleRepository,
            DriverAbsenceRepository driverAbsenceRepository,
            DriverIncidentRepository driverIncidentRepository,
            DebtRepository debtRepository,
            DailyRevenueRepository dailyRevenueRepository,
            NotificationService notificationService,
            DebtService debtService,
            UserAccountRepository userAccountRepository,
            PasswordEncoder passwordEncoder,
            AuditLogService auditLogService
    ) {
        this.driverRepository = driverRepository;
        this.companyResolver = companyResolver;
        this.authenticatedCompanyProvider = authenticatedCompanyProvider;
        this.authenticatedUserProvider = authenticatedUserProvider;
        this.vehicleRepository = vehicleRepository;
        this.driverAbsenceRepository = driverAbsenceRepository;
        this.driverIncidentRepository = driverIncidentRepository;
        this.debtRepository = debtRepository;
        this.dailyRevenueRepository = dailyRevenueRepository;
        this.notificationService = notificationService;
        this.debtService = debtService;
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditLogService = auditLogService;
    }

    public List<DriverResponse> findAll(UUID companyId) {
        UserAccount currentUser = authenticatedUserProvider.requireUser();
        return driverRepository.findAllByCompanyId(companyId).stream()
                .filter(driver -> canViewDriver(currentUser.getRole(), currentUser.getEmail(), driver))
                .map(driver -> toResponse(driver, companyId))
                .toList();
    }

    public Driver findById(UUID id) {
        return driverRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Driver not found"));
    }

    public Driver findByIdForCompany(UUID id, UUID companyId) {
        return driverRepository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Driver not found"));
    }

    public Driver findByNameForCompany(String fullName, UUID companyId) {
        if (fullName == null || fullName.isBlank()) {
            return null;
        }
        return driverRepository.findByCompanyIdAndFullNameIgnoreCase(companyId, fullName.trim())
                .orElse(null);
    }

    public Driver create(DriverRequest request) {
        UUID companyId = authenticatedCompanyProvider.requireCompanyId();
        RoleName targetRole = request.role() == null ? RoleName.DRIVER : request.role();
        assertAllowedManagedRole(targetRole);

        Driver driver = new Driver();
        applyRequest(driver, request, companyId);
        Driver saved = driverRepository.save(driver);
        syncUserAccount(saved, null, companyId, request.password());
        assignVehicleIfRequested(saved, request.assignedVehicleId(), companyId);
        auditLogService.log("CREATE", "DRIVER", saved.getId().toString(), null, saved.getFullName() + "|" + saved.getRole().name());
        return saved;
    }

    public Driver update(UUID driverId, DriverRequest request) {
        UUID companyId = authenticatedCompanyProvider.requireCompanyId();
        Driver driver = findByIdForCompany(driverId, companyId);
        RoleName targetRole = request.role() == null ? RoleName.DRIVER : request.role();
        assertAllowedManagedRole(targetRole);

        String before = driver.getFullName() + "|" + driver.getRole().name() + "|" + driver.getEmail();
        String previousEmail = driver.getEmail();
        applyRequest(driver, request, companyId);
        Driver saved = driverRepository.save(driver);
        syncUserAccount(saved, previousEmail, companyId, request.password());
        assignVehicleIfRequested(saved, request.assignedVehicleId(), companyId);
        auditLogService.log("UPDATE", "DRIVER", saved.getId().toString(), before, saved.getFullName() + "|" + saved.getRole().name() + "|" + saved.getEmail());
        return saved;
    }

    public void delete(UUID driverId) {
        UUID companyId = authenticatedCompanyProvider.requireCompanyId();
        Driver driver = findByIdForCompany(driverId, companyId);
        vehicleRepository.findAllByCompanyId(companyId).stream()
                .filter(vehicle -> vehicle.getDriver() != null && vehicle.getDriver().getId().equals(driverId))
                .forEach(vehicle -> vehicle.setDriver(null));
        driverRepository.delete(driver);
        auditLogService.log("DELETE", "DRIVER", driver.getId().toString(), driver.getFullName() + "|" + driver.getRole().name(), null);
    }

    public DriverSelfOverviewResponse getSelfOverview() {
        UUID companyId = authenticatedCompanyProvider.requireCompanyId();
        Driver driver = requireCurrentDriver(companyId);
        Vehicle vehicle = findAssignedVehicle(companyId, driver.getId());
        return new DriverSelfOverviewResponse(
                driver.getId(),
                driver.getFullName(),
                driver.getPerformanceScore(),
                vehicle == null ? null : vehicle.getId(),
                vehicle == null ? null : vehicle.getMatricule(),
                vehicle == null || vehicle.getType() == null ? null : vehicle.getType().name(),
                vehicle == null || vehicle.getClient() == null ? null : vehicle.getClient().getName(),
                debtService.openDebtTotalForDriver(driver.getId()),
                monthlyIncidentCount(companyId, driver.getId(), YearMonth.now())
        );
    }

    public List<DebtResponse> getSelfDebts(String month) {
        UUID companyId = authenticatedCompanyProvider.requireCompanyId();
        Driver driver = requireCurrentDriver(companyId);
        return debtService.findForDriver(driver.getId(), month);
    }

    public DriverProductivityResponse getSelfProductivity(String month) {
        UUID companyId = authenticatedCompanyProvider.requireCompanyId();
        Driver driver = requireCurrentDriver(companyId);
        Vehicle vehicle = findAssignedVehicle(companyId, driver.getId());
        YearMonth yearMonth = YearMonth.parse(month);

        if (vehicle == null) {
            return new DriverProductivityResponse(month, BigDecimal.ZERO, BigDecimal.ZERO, 0, 0);
        }

        List<DailyRevenue> revenues = dailyRevenueRepository.findByCompanyIdAndVehicleIdAndDateRange(
                companyId,
                vehicle.getId(),
                yearMonth.atDay(1),
                yearMonth.atEndOfMonth()
        );

        BigDecimal totalRevenue = revenues.stream().map(DailyRevenue::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalDebt = revenues.stream().map(DailyRevenue::getGeneratedDebt).reduce(BigDecimal.ZERO, BigDecimal::add);
        int activeDays = (int) revenues.stream().filter(revenue -> revenue.getActivityStatus() == ActivityStatus.ACTIVE).count();
        int targetReachedDays = (int) revenues.stream()
                .filter(revenue -> revenue.getActivityStatus() == ActivityStatus.ACTIVE)
                .filter(revenue -> revenue.getGeneratedDebt() == null || revenue.getGeneratedDebt().compareTo(BigDecimal.ZERO) == 0)
                .count();

        return new DriverProductivityResponse(month, totalRevenue, totalDebt, activeDays, targetReachedDays);
    }

    public DriverIncidentResponse reportIncident(DriverIncidentRequest request) {
        UUID companyId = authenticatedCompanyProvider.requireCompanyId();
        Driver driver = requireCurrentDriver(companyId);
        DriverIncident incident = new DriverIncident();
        incident.setCompany(companyResolver.require(companyId));
        incident.setDriver(driver);
        incident.setSubject(request.subject());
        incident.setDescription(request.description());
        incident.setStatus("OPEN");
        DriverIncident saved = driverIncidentRepository.save(incident);

        notificationService.createForCompany(
                companyId,
                "Incident chauffeur",
                driver.getFullName() + " a signale: " + request.subject(),
                "/dashboard/messages",
                NotificationType.WARNING
        );

        return toIncidentResponse(saved);
    }

    private void applyRequest(Driver driver, DriverRequest request, UUID companyId) {
        driver.setFullName(request.fullName());
        driver.setEmail(request.email());
        driver.setPhoneNumber(request.phoneNumber());
        driver.setLicenseNumber(request.licenseNumber());
        driver.setDocumentUrl(request.documentUrl());
        driver.setRole(request.role() == null ? RoleName.DRIVER : request.role());
        driver.setStatus(request.status());
        driver.setPerformanceScore(request.performanceScore() == null ? 0 : request.performanceScore());
        driver.setCompany(companyResolver.require(companyId));
    }

    private void assertAllowedManagedRole(RoleName targetRole) {
        RoleName currentRole = authenticatedUserProvider.requireUser().getRole();
        if (targetRole == RoleName.ADMIN || targetRole == RoleName.CLIENT) {
            throw new ApiException(HttpStatus.FORBIDDEN, "This role cannot be managed from the collaborator screen");
        }
        if ((targetRole == RoleName.ASSISTANT || targetRole == RoleName.OPERATIONS_MANAGER) && currentRole != RoleName.ADMIN) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only direction can create or update assistant and operations manager profiles");
        }
        if (targetRole == RoleName.DRIVER && currentRole != RoleName.ADMIN && currentRole != RoleName.OPERATIONS_MANAGER && currentRole != RoleName.ASSISTANT) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You are not allowed to manage driver profiles");
        }
    }

    private boolean canViewDriver(RoleName currentRole, String currentEmail, Driver driver) {
        if (currentRole == RoleName.ADMIN) {
            return true;
        }
        if (currentRole == RoleName.OPERATIONS_MANAGER) {
            return driver.getRole() == RoleName.DRIVER || driver.getRole() == RoleName.ASSISTANT;
        }
        if (currentRole == RoleName.ASSISTANT) {
            return driver.getRole() == RoleName.DRIVER
                    || (driver.getEmail() != null && driver.getEmail().equalsIgnoreCase(currentEmail));
        }
        return driver.getRole() == RoleName.DRIVER;
    }

    private void assignVehicleIfRequested(Driver driver, UUID vehicleId, UUID companyId) {
        if (vehicleId == null) {
            return;
        }
        Vehicle vehicle = vehicleRepository.findByIdAndCompanyId(vehicleId, companyId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Assigned vehicle not found"));
        vehicle.setDriver(driver);
        vehicleRepository.save(vehicle);
    }

    private DriverResponse toResponse(Driver driver, UUID companyId) {
        Vehicle vehicle = findAssignedVehicle(companyId, driver.getId());
        YearMonth currentMonth = YearMonth.now();
        int absences = driverAbsenceRepository.findAllByCompanyIdAndStartDateBetweenOrderByStartDateAsc(
                companyId,
                currentMonth.atDay(1),
                currentMonth.atEndOfMonth()
        ).stream().filter(absence -> absence.getDriver().getId().equals(driver.getId())).toList().size();

        return new DriverResponse(
                driver.getId(),
                driver.getFullName(),
                driver.getEmail(),
                driver.getPhoneNumber(),
                driver.getLicenseNumber(),
                driver.getDocumentUrl(),
                driver.getRole().name(),
                driver.getStatus(),
                driver.getPerformanceScore(),
                vehicle == null ? null : vehicle.getId(),
                vehicle == null ? null : vehicle.getMatricule(),
                monthlyIncidentCount(companyId, driver.getId(), currentMonth),
                absences,
                debtService.openDebtTotalForDriver(driver.getId())
        );
    }

    private int monthlyIncidentCount(UUID companyId, UUID driverId, YearMonth month) {
        return (int) driverIncidentRepository.countByCompanyIdAndDriverIdAndCreatedAtAfter(
                companyId,
                driverId,
                month.atDay(1).atStartOfDay(java.time.ZoneOffset.UTC).toInstant()
        );
    }

    private DriverIncidentResponse toIncidentResponse(DriverIncident incident) {
        return new DriverIncidentResponse(
                incident.getId(),
                incident.getDriver().getId(),
                incident.getDriver().getFullName(),
                incident.getSubject(),
                incident.getDescription(),
                incident.getStatus(),
                incident.getCreatedAt().atOffset(java.time.ZoneOffset.UTC).toLocalDateTime()
        );
    }

    private Driver requireCurrentDriver(UUID companyId) {
        String email = authenticatedUserProvider.requireUser().getEmail();
        return driverRepository.findByCompanyIdAndEmail(companyId, email)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "No driver profile found for current user"));
    }

    private Vehicle findAssignedVehicle(UUID companyId, UUID driverId) {
        return vehicleRepository.findAllByCompanyId(companyId).stream()
                .filter(vehicle -> vehicle.getDriver() != null && vehicle.getDriver().getId().equals(driverId))
                .findFirst()
                .orElse(null);
    }

    private void syncUserAccount(Driver driver, String previousEmail, UUID companyId, String rawPassword) {
        if (driver.getEmail() == null || driver.getEmail().isBlank()) {
            return;
        }

        String normalizedEmail = driver.getEmail().trim().toLowerCase();
        UserAccount account = null;

        if (previousEmail != null && !previousEmail.isBlank()) {
            account = userAccountRepository.findByCompanyIdAndEmail(companyId, previousEmail.trim().toLowerCase()).orElse(null);
        }

        if (account == null) {
            account = userAccountRepository.findByCompanyIdAndEmail(companyId, normalizedEmail).orElse(null);
        }

        if (account == null) {
            account = new UserAccount();
            account.setCompany(companyResolver.require(companyId));
            account.setPasswordHash(passwordEncoder.encode(rawPassword == null || rawPassword.isBlank() ? DEFAULT_COLLABORATOR_PASSWORD : rawPassword));
        } else if (rawPassword != null && !rawPassword.isBlank()) {
            account.setPasswordHash(passwordEncoder.encode(rawPassword));
        }

        UserAccount conflictingAccount = userAccountRepository.findByCompanyIdAndEmail(companyId, normalizedEmail).orElse(null);
        if (conflictingAccount != null && (account.getId() == null || !conflictingAccount.getId().equals(account.getId()))) {
            throw new ApiException(HttpStatus.CONFLICT, "A user account already exists with this email");
        }

        account.setFullName(driver.getFullName());
        account.setEmail(normalizedEmail);
        account.setRole(driver.getRole());
        userAccountRepository.save(account);
    }
}
