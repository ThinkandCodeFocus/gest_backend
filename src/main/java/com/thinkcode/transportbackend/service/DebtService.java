package com.thinkcode.transportbackend.service;

import com.thinkcode.transportbackend.entity.Debt;
import com.thinkcode.transportbackend.entity.DebtStatus;
import com.thinkcode.transportbackend.entity.Driver;
import com.thinkcode.transportbackend.entity.NotificationType;
import com.thinkcode.transportbackend.entity.Vehicle;
import com.thinkcode.transportbackend.repository.DebtRepository;
import com.thinkcode.transportbackend.dto.DebtCreateRequest;
import com.thinkcode.transportbackend.dto.DebtPaymentRequest;
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
    private final VehicleService vehicleService;
    private final DriverService driverService;
    private final NotificationService notificationService;

    public DebtService(
            DebtRepository debtRepository,
            AuthenticatedCompanyProvider authenticatedCompanyProvider,
            VehicleService vehicleService,
            DriverService driverService,
            NotificationService notificationService
    ) {
        this.debtRepository = debtRepository;
        this.authenticatedCompanyProvider = authenticatedCompanyProvider;
        this.vehicleService = vehicleService;
        this.driverService = driverService;
        this.notificationService = notificationService;
    }

    public List<Debt> findAll(UUID companyId) {
        return debtRepository.findAllByVehicleCompanyId(companyId);
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
        Debt debt = new Debt();
        debt.setVehicle(vehicle);
        debt.setDriver(driver);
        debt.setAmount(amount);
        debt.setDebtDate(debtDate);
        debt.setReason(reason);
        debt.setStatus(DebtStatus.OPEN);
        Debt saved = debtRepository.save(debt);

        notificationService.createForCompany(
            vehicle.getCompany().getId(),
            "Nouvelle dette",
            "Dette creee pour vehicule " + vehicle.getMatricule() + ": " + amount,
            NotificationType.WARNING
        );

        return saved;
    }

    public Debt createManualDebt(DebtCreateRequest request) {
        UUID authenticatedCompanyId = authenticatedCompanyProvider.requireCompanyId();
        Vehicle vehicle = vehicleService.findByIdForCompany(request.vehicleId(), authenticatedCompanyId);
        Driver driver = request.driverId() == null
                ? vehicle.getDriver()
                : driverService.findByIdForCompany(request.driverId(), authenticatedCompanyId);

        if (request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Debt amount must be greater than zero");
        }

        return createAutomaticDebt(vehicle, driver, request.amount(), request.debtDate(), request.reason());
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
        debt.setPaidAmount(newPaidAmount);
        if (newPaidAmount.compareTo(BigDecimal.ZERO) > 0 && newPaidAmount.compareTo(debt.getAmount()) < 0) {
            debt.setStatus(DebtStatus.PARTIALLY_PAID);
        } else if (newPaidAmount.compareTo(debt.getAmount()) == 0) {
            debt.setStatus(DebtStatus.PAID);
            notificationService.createForCompany(
                    authenticatedCompanyId,
                    "Dette soldee",
                    "La dette du vehicule " + debt.getVehicle().getMatricule() + " a ete totalement reglee.",
                    NotificationType.SUCCESS
            );
        }
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
                NotificationType.INFO
        );

        return debt;
    }
}

