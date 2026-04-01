package com.thinkcode.transportbackend.dto;

import com.thinkcode.transportbackend.entity.RoleName;
import java.util.UUID;
import jakarta.validation.constraints.NotBlank;

public record DriverRequest(
        @NotBlank String fullName,
        String email,
        String phoneNumber,
        String licenseNumber,
        RoleName role,
        String status,
        Integer performanceScore,
        UUID assignedVehicleId
) {
}

