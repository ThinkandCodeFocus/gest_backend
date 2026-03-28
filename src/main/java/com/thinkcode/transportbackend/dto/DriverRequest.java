package com.thinkcode.transportbackend.dto;

import jakarta.validation.constraints.NotBlank;

public record DriverRequest(
        @NotBlank String fullName,
        String phoneNumber,
        String licenseNumber
) {
}

