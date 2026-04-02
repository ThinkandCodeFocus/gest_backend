package com.thinkcode.transportbackend.dto;

import com.thinkcode.transportbackend.entity.RoleName;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

public record DriverRequest(
        @NotBlank String fullName,
        String email,
        String phoneNumber,
        String licenseNumber,
        String documentUrl,
        String password,
        RoleName role,
        String status,
        Integer performanceScore,
        UUID assignedVehicleId
) {
}
