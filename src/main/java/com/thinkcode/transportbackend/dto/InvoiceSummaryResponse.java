package com.thinkcode.transportbackend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record InvoiceSummaryResponse(
        String id,
        UUID clientId,
        String client,
        String clientEmail,
        List<UUID> vehicleIds,
        LocalDate periodStart,
        LocalDate periodEnd,
        BigDecimal amount,
        String status,
        String generatedFromTemplate,
        String repairReportTemplate,
        BigDecimal availableCash,
        BigDecimal breakdownRate,
        BigDecimal debtTotal,
        BigDecimal amortization,
        BigDecimal previousMonthRevenue,
        BigDecimal cumulativeRevenue
) {
}
