package com.thinkcode.transportbackend.controller;

import com.thinkcode.transportbackend.dto.ClientPortalMonthlyReportResponse;
import com.thinkcode.transportbackend.dto.ClientPortalOverviewResponse;
import com.thinkcode.transportbackend.dto.ClientPortalVehicleResponse;
import com.thinkcode.transportbackend.dto.MaintenanceResponse;
import com.thinkcode.transportbackend.entity.MaintenanceType;
import com.thinkcode.transportbackend.service.ClientPortalService;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/clients/portal")
public class ClientPortalController {

    private final ClientPortalService clientPortalService;

    public ClientPortalController(ClientPortalService clientPortalService) {
        this.clientPortalService = clientPortalService;
    }

    @GetMapping("/overview")
    @PreAuthorize("hasRole('CLIENT')")
    public ClientPortalOverviewResponse getOverview(@RequestParam String month) {
        return clientPortalService.getOverview(month);
    }

    @GetMapping("/vehicles")
    @PreAuthorize("hasRole('CLIENT')")
    public List<ClientPortalVehicleResponse> getVehicles() {
        return clientPortalService.getVehicles();
    }

    @GetMapping("/monthly-report")
    @PreAuthorize("hasRole('CLIENT')")
    public ClientPortalMonthlyReportResponse getMonthlyReport(
            @RequestParam UUID vehicleId,
            @RequestParam String month
    ) {
        return clientPortalService.getMonthlyReport(vehicleId, month);
    }

    @GetMapping("/maintenances")
    @PreAuthorize("hasRole('CLIENT')")
    public List<MaintenanceResponse> getMaintenances(
            @RequestParam(required = false) String month,
            @RequestParam(required = false) UUID vehicleId,
            @RequestParam(required = false) MaintenanceType type,
            @RequestParam(required = false) LocalDate date
    ) {
        return clientPortalService.getMaintenances(month, vehicleId, type, date);
    }
}
