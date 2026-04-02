package com.thinkcode.transportbackend.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record DriverResponse(
        UUID id,
        String fullName,
        String email,
        String phoneNumber,
        String licenseNumber,
        String documentUrl,
        String role,
        String status,
        Integer performanceScore,
        UUID assignedVehicleId,
        String assignedVehicleMatricule,
        Integer incidentsMonth,
        Integer absencesMonth,
        BigDecimal debtsOpen
) {
}
