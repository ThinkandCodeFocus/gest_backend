package com.thinkcode.transportbackend.dto;

import java.math.BigDecimal;
import java.util.List;

public record ReportingOverviewResponse(
        BigDecimal totalRevenue,
        BigDecimal totalDebt,
        BigDecimal totalMaintenanceCost,
        BigDecimal totalIncome,
        BigDecimal totalExpense,
        BigDecimal netResult,
        List<VehicleRanking> vehicleRankings,
        List<DriverRanking> driverRankings
) {
    public record VehicleRanking(
            String matricule,
            String client,
            BigDecimal revenue,
            BigDecimal debt,
            BigDecimal margin,
            String status
    ) {
    }

    public record DriverRanking(
            String name,
            Integer performanceScore,
            Integer incidentsMonth,
            BigDecimal openDebt
    ) {
    }
}
