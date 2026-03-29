package com.thinkcode.transportbackend.dto;

import java.math.BigDecimal;
import java.util.List;

public record DashboardOperationsResponse(
    Integer activeTrips,
    Integer maintenanceAlerts,
    Integer pendingDebts,
    BigDecimal totalDebt,
    List<String> criticalAlerts,
    Long unreadNotifications
) {}
