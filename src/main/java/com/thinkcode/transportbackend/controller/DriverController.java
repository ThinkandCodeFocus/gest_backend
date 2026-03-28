package com.thinkcode.transportbackend.controller;

import com.thinkcode.transportbackend.entity.Driver;
import com.thinkcode.transportbackend.service.AuthenticatedCompanyProvider;
import com.thinkcode.transportbackend.service.DriverService;
import com.thinkcode.transportbackend.dto.DriverRequest;
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
    public List<Driver> findAll() {
        return driverService.findAll(authenticatedCompanyProvider.requireCompanyId());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATIONS_MANAGER')")
    public Driver create(@Valid @RequestBody DriverRequest request) {
        return driverService.create(request);
    }

    @PutMapping("/{driverId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATIONS_MANAGER')")
    public Driver update(@PathVariable UUID driverId, @Valid @RequestBody DriverRequest request) {
        return driverService.update(driverId, request);
    }

    @DeleteMapping("/{driverId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATIONS_MANAGER')")
    public void delete(@PathVariable UUID driverId) {
        driverService.delete(driverId);
    }
}

