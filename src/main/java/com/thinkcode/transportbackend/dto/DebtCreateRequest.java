package com.thinkcode.transportbackend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record DebtCreateRequest(
        @NotNull UUID vehicleId,
        UUID driverId,
        @NotNull BigDecimal amount,
        @NotNull LocalDate debtDate,
        @NotBlank String reason,
        String beneficiary,
        String typeDebt
) {
}
