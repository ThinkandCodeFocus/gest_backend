package com.thinkcode.transportbackend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.UUID;

public record DriverAbsenceRequest(
        @NotNull UUID driverId,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate,
        @NotBlank String reason,
        boolean approved
) {
}
