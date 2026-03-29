package com.thinkcode.transportbackend.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ClientPortalVehicleResponse(
    UUID id,
    String matricule,
    String immatriculation,
    String type,
    String status,
    BigDecimal monthlyRevenue,
    BigDecimal monthlyDebt,
    Integer tripsThisMonth,
    String driver
) {}
