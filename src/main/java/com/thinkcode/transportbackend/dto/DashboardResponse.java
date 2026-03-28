package com.thinkcode.transportbackend.dto;

import java.math.BigDecimal;

public record DashboardResponse(
        long totalVehicles,
        long activeVehicles,
        BigDecimal totalRevenue,
        BigDecimal openDebt,
        BigDecimal maintenanceCost
) {
}

