package com.thinkcode.transportbackend.dto;

import jakarta.validation.constraints.NotBlank;

public record DebtUpdateRequest(
        String beneficiary,
        String typeDebt,
        @NotBlank String reason,
        String status
) {
}
