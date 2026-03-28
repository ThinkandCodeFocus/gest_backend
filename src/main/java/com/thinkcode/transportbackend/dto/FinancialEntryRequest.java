package com.thinkcode.transportbackend.dto;

import com.thinkcode.transportbackend.entity.FinancialEntryType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public record FinancialEntryRequest(
        @NotNull LocalDate entryDate,
        @NotNull FinancialEntryType type,
        @NotBlank String category,
        @NotNull BigDecimal amount,
        String reference,
        String note
) {
}
