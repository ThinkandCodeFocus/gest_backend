package com.thinkcode.transportbackend.dto;

import com.thinkcode.transportbackend.entity.MaintenanceType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record MaintenanceRequest(
        @NotNull UUID vehicleId,
        @NotNull MaintenanceType type,
        @NotNull LocalDate maintenanceDate,
        BigDecimal cost,
        @NotBlank String description,
        String documentUrl,
        Integer pieceCount,
        @Valid List<MaintenancePieceRequest> pieces,
        @DecimalMin("0.00") BigDecimal laborCost,
        String provider
) {
}

