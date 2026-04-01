package com.thinkcode.transportbackend.dto;

import java.math.BigDecimal;

public record DriverProductivityResponse(
        String month,
        BigDecimal totalRevenue,
        BigDecimal totalDebt,
        Integer activeDays,
        Integer targetReachedDays
) {
}
