package com.thinkcode.transportbackend.dto;

import java.time.LocalDate;
import java.util.UUID;

public record MaintenanceFraudAlertResponse(
        UUID maintenanceId,
        UUID vehicleId,
        String matricule,
        LocalDate date,
        String severity,
        String description,
        String reason
) {
}
