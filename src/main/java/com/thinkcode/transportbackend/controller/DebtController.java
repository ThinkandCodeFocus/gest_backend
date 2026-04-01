package com.thinkcode.transportbackend.controller;

import com.thinkcode.transportbackend.dto.DebtCreateRequest;
import com.thinkcode.transportbackend.dto.DebtPaymentRequest;
import com.thinkcode.transportbackend.dto.DebtResponse;
import com.thinkcode.transportbackend.dto.DebtUpdateRequest;
import com.thinkcode.transportbackend.entity.Debt;
import com.thinkcode.transportbackend.service.AuthenticatedCompanyProvider;
import com.thinkcode.transportbackend.service.DebtService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/debts")
public class DebtController {

    private final DebtService debtService;
    private final AuthenticatedCompanyProvider authenticatedCompanyProvider;

    public DebtController(DebtService debtService, AuthenticatedCompanyProvider authenticatedCompanyProvider) {
        this.debtService = debtService;
        this.authenticatedCompanyProvider = authenticatedCompanyProvider;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATIONS_MANAGER', 'ASSISTANT')")
    public List<DebtResponse> findAll(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String beneficiary,
            @RequestParam(required = false) UUID driverId,
            @RequestParam(required = false) UUID vehicleId,
            @RequestParam(required = false) String client,
            @RequestParam(required = false) String month
    ) {
        return debtService.findAll(authenticatedCompanyProvider.requireCompanyId(), query, status, beneficiary, driverId, vehicleId, client, month);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATIONS_MANAGER', 'ASSISTANT')")
    public DebtResponse createManual(@Valid @RequestBody DebtCreateRequest request) {
        return debtService.createManualDebtResponse(request);
    }

    @PatchMapping("/{debtId}/payments")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATIONS_MANAGER', 'ASSISTANT')")
    public Debt registerPayment(@PathVariable UUID debtId, @Valid @RequestBody DebtPaymentRequest request) {
        return debtService.registerPayment(debtId, request);
    }

    @PatchMapping("/{debtId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATIONS_MANAGER', 'ASSISTANT')")
    public DebtResponse updateInline(@PathVariable UUID debtId, @Valid @RequestBody DebtUpdateRequest request) {
        return debtService.updateInline(debtId, request);
    }

    @PatchMapping("/{debtId}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATIONS_MANAGER')")
    public Debt cancel(@PathVariable UUID debtId) {
        return debtService.cancel(debtId);
    }
}

