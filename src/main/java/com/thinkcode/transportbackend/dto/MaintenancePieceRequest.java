package com.thinkcode.transportbackend.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record MaintenancePieceRequest(
        @NotBlank String description,
        @NotNull @DecimalMin("0.00") BigDecimal prixPiece
) {
}
