package com.thinkcode.transportbackend.dto;

import java.util.UUID;

public record DriverResponse(
        UUID id,
        String fullName,
        String email,
        String phoneNumber,
        String licenseNumber,
        String role,
        String status,
        Integer performanceScore,
        UUID assignedVehicleId,
        String assignedVehicleMatricule,
        Integer incidentsMonth,
        Integer absencesMonth,
        java.math.BigDecimal debtsOpen
) {
}
