package com.thinkcode.transportbackend.service;

import com.thinkcode.transportbackend.dto.AuditLogResponse;
import com.thinkcode.transportbackend.entity.AuditLog;
import com.thinkcode.transportbackend.repository.AuditLogRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final CompanyResolver companyResolver;
    private final AuthenticatedCompanyProvider authenticatedCompanyProvider;
    private final AuthenticatedUserProvider authenticatedUserProvider;

    public AuditLogService(
            AuditLogRepository auditLogRepository,
            CompanyResolver companyResolver,
            AuthenticatedCompanyProvider authenticatedCompanyProvider,
            AuthenticatedUserProvider authenticatedUserProvider
    ) {
        this.auditLogRepository = auditLogRepository;
        this.companyResolver = companyResolver;
        this.authenticatedCompanyProvider = authenticatedCompanyProvider;
        this.authenticatedUserProvider = authenticatedUserProvider;
    }

    public void log(String action, String module, String entityId, String beforeData, String afterData) {
        UUID companyId = authenticatedCompanyProvider.requireCompanyId();
        var user = authenticatedUserProvider.requireUser();

        AuditLog log = new AuditLog();
        log.setCompany(companyResolver.require(companyId));
        log.setActorEmail(user.getEmail());
        log.setAction(action);
        log.setModule(module);
        log.setEntityId(entityId);
        log.setBeforeData(beforeData);
        log.setAfterData(afterData);
        auditLogRepository.save(log);
    }

    public List<AuditLog> findAll(Instant from, Instant to) {
        UUID companyId = authenticatedCompanyProvider.requireCompanyId();
        if (from != null && to != null) {
            return auditLogRepository.findAllByCompanyIdAndCreatedAtBetweenOrderByCreatedAtDesc(companyId, from, to);
        }
        return auditLogRepository.findAllByCompanyIdOrderByCreatedAtDesc(companyId);
    }

    public List<AuditLogResponse> findAllResponses(Instant from, Instant to) {
        return findAll(from, to).stream()
                .map(this::toResponse)
                .toList();
    }

    private AuditLogResponse toResponse(AuditLog log) {
        return new AuditLogResponse(
                log.getId(),
                log.getCreatedAt(),
                log.getActorEmail(),
                log.getAction(),
                log.getModule(),
                log.getEntityId(),
                log.getBeforeData(),
                log.getAfterData()
        );
    }
}
