package com.thinkcode.transportbackend.controller;

import com.thinkcode.transportbackend.service.DashboardService;
import com.thinkcode.transportbackend.service.AuthenticatedCompanyProvider;
import com.thinkcode.transportbackend.dto.DashboardResponse;
import com.thinkcode.transportbackend.dto.DashboardDirectionResponse;
import com.thinkcode.transportbackend.dto.DashboardOperationsResponse;
import com.thinkcode.transportbackend.dto.DashboardAssistantResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;
    private final AuthenticatedCompanyProvider authenticatedCompanyProvider;

    public DashboardController(
            DashboardService dashboardService,
            AuthenticatedCompanyProvider authenticatedCompanyProvider
    ) {
        this.dashboardService = dashboardService;
        this.authenticatedCompanyProvider = authenticatedCompanyProvider;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATIONS_MANAGER')")
    public DashboardResponse getMetrics() {
        return dashboardService.getMetrics(authenticatedCompanyProvider.requireCompanyId());
    }

    @GetMapping("/direction")
    @PreAuthorize("hasRole('ADMIN')")
    public DashboardDirectionResponse getDirectionDashboard() {
        // Mock response for MVP
        return new DashboardDirectionResponse(
            BigDecimal.valueOf(150000),
            BigDecimal.valueOf(50000),
            BigDecimal.valueOf(3.0),
            25,
            52,
            Collections.emptyMap(),
            Map.of("critical", 2, "warning", 5, "info", 12)
        );
    }

    @GetMapping("/operations")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATIONS_MANAGER')")
    public DashboardOperationsResponse getOperationsDashboard() {
        // Mock response for MVP
        return new DashboardOperationsResponse(
            45,
            3,
            18,
            BigDecimal.valueOf(25000),
            List.of("Vehicle #001 down for maintenance"),
            5L
        );
    }

    @GetMapping("/assistant")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATIONS_MANAGER', 'ASSISTANT')")
    public DashboardAssistantResponse getAssistantDashboard() {
        // Mock response for MVP
        return new DashboardAssistantResponse(
            12,
            5,
            3,
            List.of("Driver shift 08:00", "Vehicle maintenance 14:00"),
            8L
        );
    }
}

