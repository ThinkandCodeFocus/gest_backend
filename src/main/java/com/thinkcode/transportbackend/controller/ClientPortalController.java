package com.thinkcode.transportbackend.controller;

import com.thinkcode.transportbackend.dto.ClientPortalMonthlyReportResponse;
import com.thinkcode.transportbackend.dto.ClientPortalOverviewResponse;
import com.thinkcode.transportbackend.dto.ClientPortalVehicleResponse;
import com.thinkcode.transportbackend.service.ClientPortalService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/clients/portal")
public class ClientPortalController {

    private final ClientPortalService clientPortalService;

    public ClientPortalController(ClientPortalService clientPortalService) {
        this.clientPortalService = clientPortalService;
    }

    /**
     * Get overview: total revenue, debt, active vehicles for a month
     * Example: /clients/portal/overview?month=2024-03
     */
    @GetMapping("/overview")
    @PreAuthorize("hasRole('CLIENT')")
    public ClientPortalOverviewResponse getOverview(@RequestParam String month) {
        return clientPortalService.getOverview(month);
    }

    /**
     * Get list of client's vehicles with KPIs
     */
    @GetMapping("/vehicles")
    @PreAuthorize("hasRole('CLIENT')")
    public List<ClientPortalVehicleResponse> getVehicles() {
        return clientPortalService.getVehicles();
    }

    /**
     * Get monthly report for a specific vehicle
     * Example: /clients/portal/monthly-report?vehicleId=...&month=2024-03
     */
    @GetMapping("/monthly-report")
    @PreAuthorize("hasRole('CLIENT')")
    public ClientPortalMonthlyReportResponse getMonthlyReport(
            @RequestParam UUID vehicleId,
            @RequestParam String month
    ) {
        return clientPortalService.getMonthlyReport(vehicleId, month);
    }
}
