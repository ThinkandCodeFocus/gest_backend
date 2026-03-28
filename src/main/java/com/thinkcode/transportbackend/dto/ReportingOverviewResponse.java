package com.thinkcode.transportbackend.dto;

import java.math.BigDecimal;

public record ReportingOverviewResponse(
        BigDecimal totalRevenue,
        BigDecimal totalDebt,
        BigDecimal totalMaintenanceCost,
        BigDecimal totalIncome,
        BigDecimal totalExpense,
        BigDecimal netResult
) {
}
