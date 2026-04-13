package com.thinkcode.transportbackend.controller;

import com.thinkcode.transportbackend.dto.RevenueRuleRequest;
import com.thinkcode.transportbackend.dto.RevenueRuleResponse;
import com.thinkcode.transportbackend.dto.SystemSettingRequest;
import com.thinkcode.transportbackend.entity.RevenueRule;
import com.thinkcode.transportbackend.entity.SystemSetting;
import com.thinkcode.transportbackend.service.AuthenticatedCompanyProvider;
import com.thinkcode.transportbackend.service.DatabaseBackupService;
import com.thinkcode.transportbackend.service.RevenueRuleService;
import com.thinkcode.transportbackend.service.SystemSettingService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/settings")
public class SettingsController {

    private final SystemSettingService systemSettingService;
    private final RevenueRuleService revenueRuleService;
    private final AuthenticatedCompanyProvider authenticatedCompanyProvider;
    private final DatabaseBackupService databaseBackupService;

    public SettingsController(
            SystemSettingService systemSettingService,
            RevenueRuleService revenueRuleService,
            AuthenticatedCompanyProvider authenticatedCompanyProvider,
            DatabaseBackupService databaseBackupService
    ) {
        this.systemSettingService = systemSettingService;
        this.revenueRuleService = revenueRuleService;
        this.authenticatedCompanyProvider = authenticatedCompanyProvider;
        this.databaseBackupService = databaseBackupService;
    }

    // System Settings - ADMIN ONLY
    @GetMapping("/system")
    @PreAuthorize("hasRole('ADMIN')")
    public List<SystemSetting> getSystemSettings() {
        return systemSettingService.getAll();
    }

    @PostMapping("/system")
    @PreAuthorize("hasRole('ADMIN')")
    public SystemSetting createSystemSetting(@Valid @RequestBody SystemSettingRequest request) {
        SystemSetting setting = new SystemSetting();
        setting.setSettingKey(request.settingKey());
        setting.setSettingValue(request.settingValue());
        setting.setDescription(request.description());
        return systemSettingService.save(setting);
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
    public List<RevenueRuleResponse> getRevenueRules() {
        UUID companyId = authenticatedCompanyProvider.requireCompanyId();
        return revenueRuleService.findResponsesByCompanyId(companyId);
    }

    @PostMapping("/revenue-rules")
    @PreAuthorize("hasRole('ADMIN')")
    public RevenueRuleResponse createRevenueRule(@Valid @RequestBody RevenueRuleRequest request) {
        UUID companyId = authenticatedCompanyProvider.requireCompanyId();
        RevenueRule rule = new RevenueRule();
        rule.setRuleType(request.ruleType());
        rule.setRuleValue(request.ruleValue());
        rule.setDescription(request.description());
        rule.setActive(request.active() != null ? request.active() : true);
        return revenueRuleService.saveResponse(companyId, rule);
    }

    @PutMapping("/revenue-rules/{ruleId}")
    @PreAuthorize("hasRole('ADMIN')")
    public RevenueRuleResponse updateRevenueRule(
            @PathVariable UUID ruleId,
            @Valid @RequestBody RevenueRuleRequest request
    ) {
        RevenueRule rule = new RevenueRule();
        rule.setRuleValue(request.ruleValue());
        rule.setDescription(request.description());
        rule.setActive(request.active());
        return revenueRuleService.updateResponse(ruleId, rule);
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
            "ACTIVE",
            "RAIN",
            "BREAKDOWN",
            "SICK",
            "PARKED",
            "MEETING"
        );
    }

    @GetMapping("/database/export")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> exportDatabaseSql() {
        DatabaseBackupService.DatabaseBackup backup = databaseBackupService.exportSqlDump();
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + backup.fileName() + "\"")
                .body(backup.content());
    }
}

