package com.thinkcode.transportbackend.dto;

import java.time.LocalDate;
import java.util.UUID;

public record DriverAbsenceResponse(
        UUID id,
        UUID driverId,
        String driverName,
        LocalDate startDate,
        LocalDate endDate,
        String reason,
        boolean approved
) {
}
