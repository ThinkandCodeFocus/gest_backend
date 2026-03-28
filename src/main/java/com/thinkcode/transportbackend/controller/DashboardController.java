package com.thinkcode.transportbackend.controller;

import com.thinkcode.transportbackend.service.DashboardService;
import com.thinkcode.transportbackend.service.AuthenticatedCompanyProvider;
import com.thinkcode.transportbackend.dto.DashboardResponse;
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
}

