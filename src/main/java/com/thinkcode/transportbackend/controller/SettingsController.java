package com.thinkcode.transportbackend.controller;

import com.thinkcode.transportbackend.dto.RevenueRuleRequest;
import com.thinkcode.transportbackend.dto.SystemSettingRequest;
import com.thinkcode.transportbackend.entity.RevenueRule;
import com.thinkcode.transportbackend.entity.SystemSetting;
import com.thinkcode.transportbackend.service.AuthenticatedCompanyProvider;
import com.thinkcode.transportbackend.service.RevenueRuleService;
import com.thinkcode.transportbackend.service.SystemSettingService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/settings")
public class SettingsController {

    private final SystemSettingService systemSettingService;
    private final RevenueRuleService revenueRuleService;
    private final AuthenticatedCompanyProvider authenticatedCompanyProvider;

    public SettingsController(
            SystemSettingService systemSettingService,
            RevenueRuleService revenueRuleService,
            AuthenticatedCompanyProvider authenticatedCompanyProvider
    ) {
        this.systemSettingService = systemSettingService;
        this.revenueRuleService = revenueRuleService;
        this.authenticatedCompanyProvider = authenticatedCompanyProvider;
    }

    // System Settings - ADMIN ONLY
    @GetMapping("/system")
    @PreAuthorize("hasRole('ADMIN')")
    public List<SystemSetting> getSystemSettings() {
        return systemSettingService.getAll();
    }

    @PutMapping("/system/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public SystemSetting updateSystemSetting(
            @PathVariable UUID id,
            @Valid @RequestBody SystemSettingRequest request
    ) {
        SystemSetting setting = new SystemSetting();
        setting.setSettingValue(request.settingValue());
        setting.setDescription(request.description());
        return systemSettingService.update(id, setting);
    }

    // Revenue Rules - ADMIN ONLY
    @GetMapping("/revenue-rules")
    @PreAuthorize("hasRole('ADMIN')")
    public List<RevenueRule> getRevenueRules() {
        UUID companyId = authenticatedCompanyProvider.requireCompanyId();
        return revenueRuleService.findByCompanyId(companyId);
    }

    @PostMapping("/revenue-rules")
    @PreAuthorize("hasRole('ADMIN')")
    public RevenueRule createRevenueRule(@Valid @RequestBody RevenueRuleRequest request) {
        UUID companyId = authenticatedCompanyProvider.requireCompanyId();
        RevenueRule rule = new RevenueRule();
        rule.setRuleType(request.ruleType());
        rule.setRuleValue(request.ruleValue());
        rule.setDescription(request.description());
        rule.setActive(request.active() != null ? request.active() : true);
        return revenueRuleService.save(companyId, rule);
    }

    @PutMapping("/revenue-rules/{ruleId}")
    @PreAuthorize("hasRole('ADMIN')")
    public RevenueRule updateRevenueRule(
            @PathVariable UUID ruleId,
            @Valid @RequestBody RevenueRuleRequest request
    ) {
        RevenueRule rule = new RevenueRule();
        rule.setRuleValue(request.ruleValue());
        rule.setDescription(request.description());
        rule.setActive(request.active());
        return revenueRuleService.update(ruleId, rule);
    }

    @DeleteMapping("/revenue-rules/{ruleId}")
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteRevenueRule(@PathVariable UUID ruleId) {
        revenueRuleService.delete(ruleId);
    }

    // Vehicle Types (metadata)
    @GetMapping("/vehicle-types")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATIONS_MANAGER', 'ASSISTANT')")
    public List<String> getVehicleTypes() {
        return java.util.Arrays.asList(
            "CAR",
            "MOTO_TAXI",
            "MOTORBIKE"
        );
    }

    // Day Statuses (metadata)
    @GetMapping("/day-statuses")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATIONS_MANAGER', 'ASSISTANT')")
    public List<String> getDayStatuses() {
        return java.util.Arrays.asList(
            "WORKING",
            "WEEKEND",
            "HOLIDAY",
            "OFF"
        );
    }
}
