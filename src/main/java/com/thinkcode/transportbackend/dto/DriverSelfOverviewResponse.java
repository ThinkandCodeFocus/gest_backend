package com.thinkcode.transportbackend.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record DriverSelfOverviewResponse(
        UUID driverId,
        String driverName,
        Integer performanceScore,
        UUID vehicleId,
        String matricule,
        String vehicleType,
        String client,
        BigDecimal openDebts,
        Integer incidentsMonth
) {
}
