package com.thinkcode.transportbackend.controller;

import com.thinkcode.transportbackend.entity.Vehicle;
import com.thinkcode.transportbackend.service.AuthenticatedCompanyProvider;
import com.thinkcode.transportbackend.service.VehicleService;
import com.thinkcode.transportbackend.dto.VehicleRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;

@RestController
@RequestMapping("/vehicles")
public class VehicleController {

    private final VehicleService vehicleService;
    private final AuthenticatedCompanyProvider authenticatedCompanyProvider;

    public VehicleController(VehicleService vehicleService, AuthenticatedCompanyProvider authenticatedCompanyProvider) {
        this.vehicleService = vehicleService;
        this.authenticatedCompanyProvider = authenticatedCompanyProvider;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATIONS_MANAGER', 'ASSISTANT')")
    public List<Vehicle> findAll() {
        return vehicleService.findAll(authenticatedCompanyProvider.requireCompanyId());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATIONS_MANAGER')")
    public Vehicle create(@Valid @RequestBody VehicleRequest request) {
        return vehicleService.create(request);
    }

    @PutMapping("/{vehicleId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATIONS_MANAGER')")
    public Vehicle update(@PathVariable UUID vehicleId, @Valid @RequestBody VehicleRequest request) {
        return vehicleService.update(vehicleId, request);
    }

    @DeleteMapping("/{vehicleId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATIONS_MANAGER')")
    public void delete(@PathVariable UUID vehicleId) {
        vehicleService.delete(vehicleId);
    }
}

