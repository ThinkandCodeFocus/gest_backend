package com.thinkcode.transportbackend.controller;

import com.thinkcode.transportbackend.entity.DailyRevenue;
import com.thinkcode.transportbackend.service.AuthenticatedCompanyProvider;
import com.thinkcode.transportbackend.service.DailyRevenueService;
import com.thinkcode.transportbackend.dto.DailyRevenueRequest;
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
@RequestMapping("/revenues")
public class DailyRevenueController {

    private final DailyRevenueService dailyRevenueService;
    private final AuthenticatedCompanyProvider authenticatedCompanyProvider;

    public DailyRevenueController(
            DailyRevenueService dailyRevenueService,
            AuthenticatedCompanyProvider authenticatedCompanyProvider
    ) {
        this.dailyRevenueService = dailyRevenueService;
        this.authenticatedCompanyProvider = authenticatedCompanyProvider;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATIONS_MANAGER', 'ASSISTANT')")
    public List<DailyRevenue> findAll() {
        return dailyRevenueService.findAll(authenticatedCompanyProvider.requireCompanyId());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATIONS_MANAGER', 'ASSISTANT')")
    public DailyRevenue create(@Valid @RequestBody DailyRevenueRequest request) {
        return dailyRevenueService.create(request);
    }

    @PutMapping("/{revenueId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATIONS_MANAGER', 'ASSISTANT')")
    public DailyRevenue update(@PathVariable UUID revenueId, @Valid @RequestBody DailyRevenueRequest request) {
        return dailyRevenueService.update(revenueId, request);
    }

    @DeleteMapping("/{revenueId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATIONS_MANAGER')")
    public void delete(@PathVariable UUID revenueId) {
        dailyRevenueService.delete(revenueId);
    }
}

