package com.thinkcode.transportbackend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.thinkcode.transportbackend.dto.DebtPaymentRequest;
import com.thinkcode.transportbackend.entity.Debt;
import com.thinkcode.transportbackend.entity.DebtStatus;
import com.thinkcode.transportbackend.repository.DebtRepository;
import com.thinkcode.transportbackend.repository.DriverRepository;
import com.thinkcode.transportbackend.repository.VehicleRepository;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class DebtServiceOwnershipTest {

    @Mock
    private DebtRepository debtRepository;

    @Mock
    private AuthenticatedCompanyProvider authenticatedCompanyProvider;

    @Mock
    private VehicleRepository vehicleRepository;

    @Mock
    private DriverRepository driverRepository;

    @Mock
    private AuthenticatedUserProvider authenticatedUserProvider;

    @Mock
    private NotificationService notificationService;

    @Mock
    private AuditLogService auditLogService;

    private DebtService debtService;

    @BeforeEach
    void setUp() {
        debtService = new DebtService(
                debtRepository,
                authenticatedCompanyProvider,
                authenticatedUserProvider,
                vehicleRepository,
                driverRepository,
                notificationService,
                auditLogService
        );
    }

    @Test
    void registerPaymentShouldRejectDebtFromAnotherCompany() {
        UUID debtId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();

        when(authenticatedCompanyProvider.requireCompanyId()).thenReturn(companyId);
        when(debtRepository.findByIdAndVehicleCompanyId(debtId, companyId)).thenReturn(Optional.empty());

        ApiException exception = assertThrows(
                ApiException.class,
                () -> debtService.registerPayment(debtId, new DebtPaymentRequest(new BigDecimal("1000")))
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
    }

    @Test
    void registerPaymentShouldUpdateStatusWhenPaymentIsPartial() {
        UUID debtId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();

        Debt debt = new Debt();
        debt.setAmount(new BigDecimal("10000"));
        debt.setPaidAmount(BigDecimal.ZERO);
        debt.setStatus(DebtStatus.OPEN);

        when(authenticatedCompanyProvider.requireCompanyId()).thenReturn(companyId);
        when(debtRepository.findByIdAndVehicleCompanyId(debtId, companyId)).thenReturn(Optional.of(debt));

        Debt updated = debtService.registerPayment(debtId, new DebtPaymentRequest(new BigDecimal("3000")));

        assertEquals(new BigDecimal("3000"), updated.getPaidAmount());
        assertEquals(DebtStatus.PARTIALLY_PAID, updated.getStatus());
    }
}
