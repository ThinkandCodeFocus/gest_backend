package com.thinkcode.transportbackend.dto;

import com.thinkcode.transportbackend.entity.VehicleStatus;
import com.thinkcode.transportbackend.entity.VehicleType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record VehicleRequest(
        @NotBlank String matricule,
        @NotNull VehicleType type,
        UUID driverId,
        UUID clientId,
        VehicleStatus status,
        BigDecimal amortization,
        LocalDate startDate,
        BigDecimal dailyTarget
) {
}

