package com.thinkcode.transportbackend.dto;

import com.thinkcode.transportbackend.entity.RoleName;
import java.util.UUID;

public record AuthResponse(
        String token,
        UUID userId,
        String fullName,
        String email,
        RoleName role,
        UUID companyId
) {
}

