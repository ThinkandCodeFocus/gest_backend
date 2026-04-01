package com.thinkcode.transportbackend.dto;

import java.time.Instant;
import java.util.UUID;

public record AuditLogResponse(
        UUID id,
        Instant createdAt,
        String actorEmail,
        String action,
        String module,
        String entityId,
        String beforeData,
        String afterData
) {
}
