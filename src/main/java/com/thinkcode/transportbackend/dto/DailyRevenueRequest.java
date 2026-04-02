package com.thinkcode.transportbackend.dto;

import com.thinkcode.transportbackend.entity.ActivityStatus;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record DailyRevenueRequest(
        @NotNull UUID vehicleId,
        @NotNull LocalDate revenueDate,
        @NotNull BigDecimal amount,
        @NotNull ActivityStatus activityStatus,
        BigDecimal driverShare,
        BigDecimal companyShare,
        BigDecimal clientShare,
        BigDecimal generatedDebt,
        String note,
        String observation
) {
}

