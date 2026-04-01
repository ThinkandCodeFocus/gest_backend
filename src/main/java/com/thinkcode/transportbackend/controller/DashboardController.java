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
        return dashboardService.getDirectionDashboard(authenticatedCompanyProvider.requireCompanyId());
    }

    @GetMapping("/operations")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATIONS_MANAGER')")
    public DashboardOperationsResponse getOperationsDashboard() {
        return dashboardService.getOperationsDashboard(authenticatedCompanyProvider.requireCompanyId());
    }

    @GetMapping("/assistant")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATIONS_MANAGER', 'ASSISTANT')")
    public DashboardAssistantResponse getAssistantDashboard() {
        return dashboardService.getAssistantDashboard(authenticatedCompanyProvider.requireCompanyId());
    }
}

