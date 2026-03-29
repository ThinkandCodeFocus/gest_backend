package com.thinkcode.transportbackend.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ClientPortalOverviewResponse(
    BigDecimal totalRevenue,
    BigDecimal totalDebt,
    Integer activeVehicles,
    Integer completedTrips,
    String month,
    String clientName
) {}
