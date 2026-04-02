package com.thinkcode.transportbackend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record DailyRevenueResponse(
        UUID id,
        LocalDate revenueDate,
        BigDecimal amount,
        String activityStatus,
        BigDecimal driverShare,
        BigDecimal companyShare,
        BigDecimal clientShare,
        BigDecimal generatedDebt,
        String note,
        String observation,
        VehicleSummary vehicle
) {
    public record VehicleSummary(
            UUID id,
            String matricule
    ) {
    }
}
