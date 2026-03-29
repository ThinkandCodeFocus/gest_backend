package com.thinkcode.transportbackend.dto;

import java.math.BigDecimal;
import java.util.Map;

public record DashboardDirectionResponse(
    BigDecimal totalProfit,
    BigDecimal costOfOperations,
    BigDecimal roi,
    Integer activeVehicles,
    Integer totalDrivers,
    Map<String, BigDecimal> revenueByVehicle,
    Map<String, Integer> alertsCount
) {}
