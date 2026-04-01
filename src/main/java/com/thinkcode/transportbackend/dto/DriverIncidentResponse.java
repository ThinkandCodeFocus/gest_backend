package com.thinkcode.transportbackend.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record DriverIncidentResponse(
        UUID id,
        UUID driverId,
        String driverName,
        String subject,
        String description,
        String status,
        LocalDateTime createdAt
) {
}
