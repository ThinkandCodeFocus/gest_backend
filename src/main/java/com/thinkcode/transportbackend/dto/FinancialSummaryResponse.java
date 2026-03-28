package com.thinkcode.transportbackend.dto;

import java.math.BigDecimal;

public record FinancialSummaryResponse(
        BigDecimal totalIncome,
        BigDecimal totalExpense,
        BigDecimal balance
) {
}
