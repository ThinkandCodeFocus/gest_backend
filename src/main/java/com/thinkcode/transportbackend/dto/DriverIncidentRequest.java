package com.thinkcode.transportbackend.dto;

import jakarta.validation.constraints.NotBlank;

public record DriverIncidentRequest(
        @NotBlank String subject,
        @NotBlank String description
) {
}
