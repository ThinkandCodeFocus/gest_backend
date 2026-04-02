package com.thinkcode.transportbackend.service;

import com.thinkcode.transportbackend.dto.DebtCreateRequest;
import com.thinkcode.transportbackend.dto.DebtPaymentRequest;
import com.thinkcode.transportbackend.dto.DebtResponse;
import com.thinkcode.transportbackend.dto.DebtUpdateRequest;
import com.thinkcode.transportbackend.entity.Debt;
import com.thinkcode.transportbackend.entity.DebtStatus;
import com.thinkcode.transportbackend.entity.Driver;
import com.thinkcode.transportbackend.entity.NotificationType;
import com.thinkcode.transportbackend.entity.RoleName;
import com.thinkcode.transportbackend.entity.UserAccount;
import com.thinkcode.transportbackend.entity.Vehicle;
import com.thinkcode.transportbackend.repository.DebtRepository;
import com.thinkcode.transportbackend.repository.DriverRepository;
import com.thinkcode.transportbackend.repository.VehicleRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DebtService {

    private final DebtRepository debtRepository;
    private final AuthenticatedCompanyProvider authenticatedCompanyProvider;
    private final AuthenticatedUserProvider authenticatedUserProvider;
    private final VehicleRepository vehicleRepository;
    private final DriverRepository driverRepository;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;

    public DebtService(
            DebtRepository debtRepository,
            AuthenticatedCompanyProvider authenticatedCompanyProvider,
            AuthenticatedUserProvider authenticatedUserProvider,
            VehicleRepository vehicleRepository,
            DriverRepository driverRepository,
            NotificationService notificationService,
            AuditLogService auditLogService
    ) {
        this.debtRepository = debtRepository;
        this.authenticatedCompanyProvider = authenticatedCompanyProvider;
        this.authenticatedUserProvider = authenticatedUserProvider;
        this.vehicleRepository = vehicleRepository;
        this.driverRepository = driverRepository;
        this.notificationService = notificationService;
        this.auditLogService = auditLogService;
    }

    public List<DebtResponse> findAll(
            UUID companyId,
            String query,
            String status,
            String beneficiary,
            UUID driverId,
            UUID vehicleId,
            String client,
            String month
    ) {
        List<Debt> debts = debtRepository.findAllByVehicleCompanyId(companyId);
        return debts.stream()
                .filter(debt -> matchesFilter(debt, query, status, beneficiary, driverId, vehicleId, client, month))
                .sorted((left, right) -> right.getDebtDate().compareTo(left.getDebtDate()))
                .map(debt -> toResponse(debt, debts.indexOf(debt) + 1))
                .toList();
    }

    public Debt findById(UUID id) {
        return debtRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Debt not found"));
    }

    public Debt findByIdForCompany(UUID id, UUID companyId) {
        return debtRepository.findByIdAndVehicleCompanyId(id, companyId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Debt not found"));
    }

    public Debt createAutomaticDebt(Vehicle vehicle, Driver driver, BigDecimal amount, LocalDate debtDate, String reason) {
        return createAutomaticDebt(vehicle, driver, amount, debtDate, reason, "chauffeur", "Objectif non atteint");
    }

    public Debt createAutomaticDebt(
            Vehicle vehicle,
            Driver driver,
            BigDecimal amount,
            LocalDate debtDate,
            String reason,
            String beneficiary,
            String typeDebt
    ) {
        Debt debt = new Debt();
        debt.setVehicle(vehicle);
        debt.setDriver(driver);
        debt.setAmount(amount);
        debt.setDebtDate(debtDate);
        debt.setReason(reason);
        debt.setBeneficiary(normalizeBeneficiary(beneficiary));
        debt.setTypeDebt(typeDebt == null || typeDebt.isBlank() ? "Regularisation" : typeDebt);
        debt.setStatus(DebtStatus.OPEN);
        debt.setValidationStatus("Validee");
        debt.setCreatedBy("systeme");
        debt.setValidatedBy("systeme");
        Debt saved = debtRepository.save(debt);

        notificationService.createForCompany(
            vehicle.getCompany().getId(),
            "Nouvelle dette",
            "Dette creee pour vehicule " + vehicle.getMatricule() + ": " + amount,
            "/dashboard/debts",
            NotificationType.WARNING
        );

        auditLogService.log("CREATE", "DEBT", saved.getId().toString(), null, saved.getValidationStatus() + "|" + saved.getTypeDebt());
        return saved;
    }

    public Debt createManualDebt(DebtCreateRequest request) {
        UUID authenticatedCompanyId = authenticatedCompanyProvider.requireCompanyId();
        Vehicle vehicle = vehicleRepository.findByIdAndCompanyId(request.vehicleId(), authenticatedCompanyId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Vehicle not found"));
        Driver driver = request.driverId() == null
                ? vehicle.getDriver()
                : driverRepository.findByIdAndCompanyId(request.driverId(), authenticatedCompanyId)
                        .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Driver not found"));

        if (request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Debt amount must be greater than zero");
        }

        UserAccount user = authenticatedUserProvider.requireUser();
        Debt debt = new Debt();
        debt.setVehicle(vehicle);
        debt.setDriver(driver);
        debt.setAmount(request.amount());
        debt.setDebtDate(request.debtDate());
        debt.setReason(request.reason());
        debt.setBeneficiary(normalizeBeneficiary(request.beneficiary()));
        debt.setTypeDebt(request.typeDebt() == null || request.typeDebt().isBlank() ? "Regularisation" : request.typeDebt());
        debt.setStatus(DebtStatus.OPEN);
        debt.setCreatedBy(user.getEmail());
        if (requiresValidation(debt.getTypeDebt()) && !canValidate(user)) {
            debt.setValidationStatus("En attente");
            debt.setValidatedBy(null);
        } else {
            debt.setValidationStatus("Validee");
            debt.setValidatedBy(user.getEmail());
        }
        Debt saved = debtRepository.save(debt);
        auditLogService.log("CREATE", "DEBT", saved.getId().toString(), null, saved.getValidationStatus() + "|" + saved.getTypeDebt());
        return saved;
    }

    public DebtResponse createManualDebtResponse(DebtCreateRequest request) {
        Debt created = createManualDebt(request);
        return toResponse(created, 1);
    }

    @Transactional
    public Debt registerPayment(UUID debtId, DebtPaymentRequest request) {
        UUID authenticatedCompanyId = authenticatedCompanyProvider.requireCompanyId();
        Debt debt = findByIdForCompany(debtId, authenticatedCompanyId);
        if (request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Payment amount must be greater than zero");
        }
        BigDecimal newPaidAmount = debt.getPaidAmount().add(request.amount());
        if (newPaidAmount.compareTo(debt.getAmount()) > 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Payment exceeds remaining debt");
        }
        BigDecimal previousPaidAmount = debt.getPaidAmount();
        debt.setPaidAmount(newPaidAmount);
        if (newPaidAmount.compareTo(BigDecimal.ZERO) > 0 && newPaidAmount.compareTo(debt.getAmount()) < 0) {
            debt.setStatus(DebtStatus.PARTIALLY_PAID);
        } else if (newPaidAmount.compareTo(debt.getAmount()) == 0) {
            debt.setStatus(DebtStatus.PAID);
            notificationService.createForCompany(
                    authenticatedCompanyId,
                    "Dette soldee",
                    "La dette du vehicule " + debt.getVehicle().getMatricule() + " a ete totalement reglee.",
                    "/dashboard/debts",
                    NotificationType.SUCCESS
            );
        }
        auditLogService.log("PAY", "DEBT", debt.getId().toString(), previousPaidAmount.toPlainString(), newPaidAmount.toPlainString());
        return debt;
    }

    public Debt cancel(UUID debtId) {
        UUID authenticatedCompanyId = authenticatedCompanyProvider.requireCompanyId();
        Debt debt = findByIdForCompany(debtId, authenticatedCompanyId);
        debt.setStatus(DebtStatus.CANCELLED);

        notificationService.createForCompany(
                authenticatedCompanyId,
                "Dette annulee",
                "La dette du vehicule " + debt.getVehicle().getMatricule() + " a ete annulee.",
                "/dashboard/debts",
                NotificationType.INFO
        );

        auditLogService.log("CANCEL", "DEBT", debt.getId().toString(), null, debt.getVehicle().getMatricule());
        return debt;
    }

    public DebtResponse updateInline(UUID debtId, DebtUpdateRequest request) {
        UUID companyId = authenticatedCompanyProvider.requireCompanyId();
        Debt debt = findByIdForCompany(debtId, companyId);
        String before = debt.getReason() + "|" + debt.getStatus().name();
        debt.setReason(request.reason());
        if (request.beneficiary() != null && !request.beneficiary().isBlank()) {
            debt.setBeneficiary(normalizeBeneficiary(request.beneficiary()));
        }
        if (request.typeDebt() != null && !request.typeDebt().isBlank()) {
            debt.setTypeDebt(request.typeDebt());
        }
        if (request.status() != null && !request.status().isBlank()) {
            if ("En attente".equalsIgnoreCase(debt.getValidationStatus())) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Debt must be validated before changing its payment status");
            }
            debt.setStatus(parseStatus(request.status()));
        }
        debt.setCreatedBy(debt.getCreatedBy() == null ? authenticatedUserProvider.requireUser().getEmail() : debt.getCreatedBy());
        Debt saved = debtRepository.save(debt);
        auditLogService.log("UPDATE", "DEBT", saved.getId().toString(), before, saved.getReason() + "|" + saved.getStatus().name());
        return toResponse(saved, 1);
    }

    public DebtResponse approve(UUID debtId) {
        return updateValidation(debtId, "Validee", "VALIDATE");
    }

    public DebtResponse reject(UUID debtId) {
        return updateValidation(debtId, "Rejetee", "REJECT");
    }

    public List<DebtResponse> findForDriver(UUID driverId, String month) {
        UUID companyId = authenticatedCompanyProvider.requireCompanyId();
        return findAll(companyId, null, null, null, driverId, null, null, month);
    }

    public BigDecimal openDebtTotalForDriver(UUID driverId) {
        UUID companyId = authenticatedCompanyProvider.requireCompanyId();
        return debtRepository.findAllByVehicleCompanyId(companyId).stream()
                .filter(debt -> debt.getDriver() != null && debt.getDriver().getId().equals(driverId))
                .filter(debt -> debt.getStatus() == DebtStatus.OPEN || debt.getStatus() == DebtStatus.PARTIALLY_PAID)
                .map(Debt::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private DebtResponse updateValidation(UUID debtId, String nextStatus, String action) {
        UUID companyId = authenticatedCompanyProvider.requireCompanyId();
        UserAccount user = authenticatedUserProvider.requireUser();
        if (!canValidate(user)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only direction or operations manager can validate this debt");
        }
        Debt debt = findByIdForCompany(debtId, companyId);
        String before = debt.getValidationStatus();
        debt.setValidationStatus(nextStatus);
        debt.setValidatedBy(user.getEmail());
        if ("Rejetee".equals(nextStatus)) {
            debt.setStatus(DebtStatus.CANCELLED);
        }
        Debt saved = debtRepository.save(debt);
        auditLogService.log(action, "DEBT", saved.getId().toString(), before, nextStatus + "|" + user.getEmail());
        return toResponse(saved, 1);
    }

    private boolean requiresValidation(String typeDebt) {
        if (typeDebt == null) {
            return true;
        }
        String normalized = typeDebt.trim().toLowerCase();
        return !normalized.contains("objectif") && !normalized.contains("recette");
    }

    private boolean canValidate(UserAccount user) {
        return user.getRole() == RoleName.ADMIN || user.getRole() == RoleName.OPERATIONS_MANAGER;
    }

    private boolean matchesFilter(
            Debt debt,
            String query,
            String status,
            String beneficiary,
            UUID driverId,
            UUID vehicleId,
            String client,
            String month
    ) {
        boolean queryMatch = query == null || query.isBlank() || containsAny(debt, query);
        boolean statusMatch = status == null || status.isBlank() || "all".equalsIgnoreCase(status) || mapFrontendStatus(status) == debt.getStatus();
        boolean beneficiaryMatch = beneficiary == null || beneficiary.isBlank() || debt.getBeneficiary().equalsIgnoreCase(beneficiary);
        boolean driverMatch = driverId == null || (debt.getDriver() != null && debt.getDriver().getId().equals(driverId));
        boolean vehicleMatch = vehicleId == null || (debt.getVehicle() != null && debt.getVehicle().getId().equals(vehicleId));
        boolean clientMatch = client == null || client.isBlank() || (debt.getVehicle() != null && debt.getVehicle().getClient() != null
                && debt.getVehicle().getClient().getName() != null
                && debt.getVehicle().getClient().getName().toLowerCase().contains(client.toLowerCase()));
        boolean monthMatch = month == null || month.isBlank() || debt.getDebtDate().toString().startsWith(month);
        return queryMatch && statusMatch && beneficiaryMatch && driverMatch && vehicleMatch && clientMatch && monthMatch;
    }

    private boolean containsAny(Debt debt, String query) {
        String needle = query.toLowerCase();
        return (debt.getVehicle() != null && debt.getVehicle().getMatricule() != null && debt.getVehicle().getMatricule().toLowerCase().contains(needle))
                || (debt.getDriver() != null && debt.getDriver().getFullName() != null && debt.getDriver().getFullName().toLowerCase().contains(needle))
                || (debt.getVehicle() != null && debt.getVehicle().getClient() != null && debt.getVehicle().getClient().getName() != null
                && debt.getVehicle().getClient().getName().toLowerCase().contains(needle))
                || (debt.getReason() != null && debt.getReason().toLowerCase().contains(needle))
                || (debt.getTypeDebt() != null && debt.getTypeDebt().toLowerCase().contains(needle));
    }

    private DebtStatus parseStatus(String value) {
        return switch (value.toLowerCase()) {
            case "en_cours", "open" -> DebtStatus.OPEN;
            case "payee", "paid" -> DebtStatus.PAID;
            case "annulee", "cancelled" -> DebtStatus.CANCELLED;
            case "partially_paid", "partielle" -> DebtStatus.PARTIALLY_PAID;
            default -> throw new ApiException(HttpStatus.BAD_REQUEST, "Unsupported debt status");
        };
    }

    private DebtStatus mapFrontendStatus(String value) {
        return parseStatus(value);
    }

    private String normalizeBeneficiary(String value) {
        if (value == null || value.isBlank()) {
            return "chauffeur";
        }
        return value.trim().toLowerCase();
    }

    private DebtResponse toResponse(Debt debt, int rowNumber) {
        BigDecimal remaining = debt.getAmount().subtract(debt.getPaidAmount() == null ? BigDecimal.ZERO : debt.getPaidAmount());
        return new DebtResponse(
                debt.getId(),
                rowNumber,
                debt.getDebtDate(),
                debt.getVehicle() == null ? null : debt.getVehicle().getId(),
                debt.getVehicle() == null ? null : debt.getVehicle().getMatricule(),
                debt.getDriver() == null ? null : debt.getDriver().getId(),
                debt.getDriver() == null ? null : debt.getDriver().getFullName(),
                debt.getBeneficiary(),
                debt.getVehicle() != null && debt.getVehicle().getClient() != null ? debt.getVehicle().getClient().getName() : null,
                debt.getTypeDebt(),
                debt.getReason(),
                debt.getAmount(),
                switch (debt.getStatus()) {
                    case OPEN -> "en_cours";
                    case PAID -> "payee";
                    case CANCELLED -> "annulee";
                    case PARTIALLY_PAID -> "en_cours";
                },
                debt.getPaidAmount(),
                remaining,
                debt.getValidationStatus(),
                debt.getCreatedBy(),
                debt.getValidatedBy()
        );
    }
}
