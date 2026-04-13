package com.thinkcode.transportbackend.controller;

import com.thinkcode.transportbackend.dto.AuditLogResponse;
import com.thinkcode.transportbackend.dto.ResetUserPasswordResponse;
import com.thinkcode.transportbackend.dto.UserManagementResponse;
import com.thinkcode.transportbackend.service.UserManagementService;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
public class UserManagementController {

    private final UserManagementService userManagementService;

    public UserManagementController(UserManagementService userManagementService) {
        this.userManagementService = userManagementService;
    }

    @GetMapping("/management")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATIONS_MANAGER', 'ASSISTANT')")
    public List<UserManagementResponse> findAll() {
        return userManagementService.findAllUsers();
    }

    @GetMapping("/{userId}/activity")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATIONS_MANAGER', 'ASSISTANT')")
    public List<AuditLogResponse> findActivity(@PathVariable UUID userId) {
        return userManagementService.findActivity(userId);
    }

    @PostMapping("/{userId}/reset-password")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATIONS_MANAGER', 'ASSISTANT')")
    public ResetUserPasswordResponse resetPassword(@PathVariable UUID userId) {
        return userManagementService.resetPassword(userId);
    }
}
