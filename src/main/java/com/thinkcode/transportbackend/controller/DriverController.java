package com.thinkcode.transportbackend.controller;

import com.thinkcode.transportbackend.dto.DebtResponse;
import com.thinkcode.transportbackend.dto.DriverIncidentRequest;
import com.thinkcode.transportbackend.dto.DriverIncidentResponse;
import com.thinkcode.transportbackend.dto.DriverProductivityResponse;
import com.thinkcode.transportbackend.dto.DriverRequest;
import com.thinkcode.transportbackend.dto.DriverResponse;
import com.thinkcode.transportbackend.dto.DriverSelfOverviewResponse;
import com.thinkcode.transportbackend.entity.Driver;
import com.thinkcode.transportbackend.service.AuthenticatedCompanyProvider;
import com.thinkcode.transportbackend.service.DriverService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/drivers")
public class DriverController {

    private final DriverService driverService;
    private final AuthenticatedCompanyProvider authenticatedCompanyProvider;

    public DriverController(DriverService driverService, AuthenticatedCompanyProvider authenticatedCompanyProvider) {
        this.driverService = driverService;
        this.authenticatedCompanyProvider = authenticatedCompanyProvider;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATIONS_MANAGER', 'ASSISTANT')")
    public List<DriverResponse> findAll() {
        return driverService.findAll(authenticatedCompanyProvider.requireCompanyId());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATIONS_MANAGER', 'ASSISTANT')")
    public DriverResponse create(@Valid @RequestBody DriverRequest request) {
        Driver saved = driverService.create(request);
        return driverService.findAll(authenticatedCompanyProvider.requireCompanyId()).stream()
                .filter(item -> item.id().equals(saved.getId()))
                .findFirst()
                .orElseThrow();
    }

    @PutMapping("/{driverId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATIONS_MANAGER', 'ASSISTANT')")
    public DriverResponse update(@PathVariable UUID driverId, @Valid @RequestBody DriverRequest request) {
        Driver saved = driverService.update(driverId, request);
        return driverService.findAll(authenticatedCompanyProvider.requireCompanyId()).stream()
                .filter(item -> item.id().equals(saved.getId()))
                .findFirst()
                .orElseThrow();
    }

    @DeleteMapping("/{driverId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATIONS_MANAGER')")
    public void delete(@PathVariable UUID driverId) {
        driverService.delete(driverId);
    }

    @GetMapping("/me/overview")
    @PreAuthorize("hasRole('DRIVER')")
    public DriverSelfOverviewResponse myOverview() {
        return driverService.getSelfOverview();
    }

    @GetMapping("/me/debts")
    @PreAuthorize("hasRole('DRIVER')")
    public List<DebtResponse> myDebts(@RequestParam String month) {
        return driverService.getSelfDebts(month);
    }

    @GetMapping("/me/productivity")
    @PreAuthorize("hasRole('DRIVER')")
    public DriverProductivityResponse myProductivity(@RequestParam String month) {
        return driverService.getSelfProductivity(month);
    }

    @PostMapping("/me/incidents")
    @PreAuthorize("hasRole('DRIVER')")
    public DriverIncidentResponse reportIncident(@Valid @RequestBody DriverIncidentRequest request) {
        return driverService.reportIncident(request);
    }
}

