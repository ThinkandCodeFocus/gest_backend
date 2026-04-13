package com.thinkcode.transportbackend.service;

import com.thinkcode.transportbackend.dto.ResetUserPasswordResponse;
import com.thinkcode.transportbackend.dto.UserManagementResponse;
import com.thinkcode.transportbackend.entity.RoleName;
import com.thinkcode.transportbackend.entity.UserAccount;
import com.thinkcode.transportbackend.repository.AuditLogRepository;
import com.thinkcode.transportbackend.repository.UserAccountRepository;
import java.security.SecureRandom;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserManagementService {

    private static final String PASSWORD_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789@#$!";

    private final UserAccountRepository userAccountRepository;
    private final AuditLogRepository auditLogRepository;
    private final AuthenticatedCompanyProvider authenticatedCompanyProvider;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;
    private final SecureRandom secureRandom = new SecureRandom();

    public UserManagementService(
            UserAccountRepository userAccountRepository,
            AuditLogRepository auditLogRepository,
            AuthenticatedCompanyProvider authenticatedCompanyProvider,
            PasswordEncoder passwordEncoder,
            AuditLogService auditLogService
    ) {
        this.userAccountRepository = userAccountRepository;
        this.auditLogRepository = auditLogRepository;
        this.authenticatedCompanyProvider = authenticatedCompanyProvider;
        this.passwordEncoder = passwordEncoder;
        this.auditLogService = auditLogService;
    }

    public List<UserManagementResponse> findAllUsers() {
        UUID companyId = authenticatedCompanyProvider.requireCompanyId();
        return userAccountRepository.findAllByCompanyIdOrderByFullNameAsc(companyId).stream()
                .map(this::toResponse)
                .toList();
    }

    public List<com.thinkcode.transportbackend.dto.AuditLogResponse> findActivity(UUID userId) {
        UUID companyId = authenticatedCompanyProvider.requireCompanyId();
        UserAccount user = userAccountRepository.findById(userId)
                .filter(account -> account.getCompany().getId().equals(companyId))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Utilisateur introuvable."));
        return auditLogRepository.findAllByCompanyIdOrderByCreatedAtDesc(companyId).stream()
                .filter(log -> user.getEmail().equalsIgnoreCase(log.getActorEmail()))
                .map(log -> new com.thinkcode.transportbackend.dto.AuditLogResponse(
                        log.getId(),
                        log.getCreatedAt(),
                        log.getActorEmail(),
                        log.getAction(),
                        log.getModule(),
                        log.getEntityId(),
                        log.getBeforeData(),
                        log.getAfterData()
                ))
                .toList();
    }

    public ResetUserPasswordResponse resetPassword(UUID userId) {
        UUID companyId = authenticatedCompanyProvider.requireCompanyId();
        UserAccount user = userAccountRepository.findById(userId)
                .filter(account -> account.getCompany().getId().equals(companyId))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Utilisateur introuvable."));

        String generatedPassword = generatePassword(10);
        user.setPasswordHash(passwordEncoder.encode(generatedPassword));
        user.setPasswordChangeRequired(true);
        user.setPasswordResetToken(null);
        user.setPasswordResetTokenExpiresAt(null);
        userAccountRepository.save(user);
        auditLogService.log("RESET_PASSWORD", "USER", user.getId().toString(), null, user.getEmail());
        return new ResetUserPasswordResponse(user.getFullName(), user.getEmail(), generatedPassword);
    }

    private UserManagementResponse toResponse(UserAccount user) {
        return new UserManagementResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                mapRole(user.getRole()),
                user.getCreatedAt(),
                user.getLastLoginAt(),
                user.isPasswordChangeRequired()
        );
    }

    private String mapRole(RoleName role) {
        return switch (role) {
            case ADMIN -> "direction";
            case OPERATIONS_MANAGER -> "responsable";
            case ASSISTANT -> "assistant";
            case DRIVER -> "chauffeur";
            case CLIENT -> "client";
        };
    }

    private String generatePassword(int length) {
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < length; index++) {
            builder.append(PASSWORD_CHARS.charAt(secureRandom.nextInt(PASSWORD_CHARS.length())));
        }
        return builder.toString();
    }
}
