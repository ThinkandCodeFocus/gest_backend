package com.thinkcode.transportbackend.controller;

import com.thinkcode.transportbackend.dto.MaintenanceFraudAlertResponse;
import com.thinkcode.transportbackend.dto.MaintenanceRequest;
import com.thinkcode.transportbackend.dto.MaintenanceResponse;
import com.thinkcode.transportbackend.service.AuthenticatedCompanyProvider;
import com.thinkcode.transportbackend.service.MaintenanceService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/maintenances")
public class MaintenanceController {

    private final MaintenanceService maintenanceService;
    private final AuthenticatedCompanyProvider authenticatedCompanyProvider;

    public MaintenanceController(
            MaintenanceService maintenanceService,
            AuthenticatedCompanyProvider authenticatedCompanyProvider
    ) {
        this.maintenanceService = maintenanceService;
        this.authenticatedCompanyProvider = authenticatedCompanyProvider;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATIONS_MANAGER', 'ASSISTANT')")
    public List<MaintenanceResponse> findAll() {
        return maintenanceService.findAll(authenticatedCompanyProvider.requireCompanyId());
    }

    @GetMapping("/alerts")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATIONS_MANAGER', 'ASSISTANT')")
    public List<MaintenanceFraudAlertResponse> alerts() {
        return maintenanceService.getFraudAlerts();
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATIONS_MANAGER', 'ASSISTANT')")
    public MaintenanceResponse create(@Valid @RequestBody MaintenanceRequest request) {
        return maintenanceService.create(request);
    }

    @PutMapping("/{maintenanceId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATIONS_MANAGER', 'ASSISTANT')")
    public MaintenanceResponse update(@PathVariable UUID maintenanceId, @Valid @RequestBody MaintenanceRequest request) {
        return maintenanceService.update(maintenanceId, request);
    }

    @PatchMapping("/{maintenanceId}/approve")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATIONS_MANAGER')")
    public MaintenanceResponse approve(@PathVariable UUID maintenanceId) {
        return maintenanceService.approve(maintenanceId);
    }

    @PatchMapping("/{maintenanceId}/reject")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATIONS_MANAGER')")
    public MaintenanceResponse reject(@PathVariable UUID maintenanceId) {
        return maintenanceService.reject(maintenanceId);
    }

    @DeleteMapping("/{maintenanceId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATIONS_MANAGER')")
    public void delete(@PathVariable UUID maintenanceId) {
        maintenanceService.delete(maintenanceId);
    }
}
