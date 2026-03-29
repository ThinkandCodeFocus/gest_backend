package com.thinkcode.transportbackend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record ClientPortalMonthlyReportResponse(
    UUID vehicleId,
    String vehicleMatricule,
    String month,
    Integer totalTrips,
    BigDecimal grossRevenue,
    BigDecimal companyCut,
    BigDecimal driverEarnings,
    BigDecimal debtGenerated,
    List<DailyTripSummary> days
) {
    public record DailyTripSummary(
        LocalDate date,
        Integer trips,
        BigDecimal revenue,
        BigDecimal debt
    ) {}
}
