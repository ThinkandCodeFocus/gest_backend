package com.thinkcode.transportbackend.dto;

import java.time.Instant;
import java.util.UUID;

public record UserManagementResponse(
        UUID id,
        String fullName,
        String email,
        String role,
        Instant createdAt,
        Instant lastLoginAt,
        boolean passwordChangeRequired
) {
}
