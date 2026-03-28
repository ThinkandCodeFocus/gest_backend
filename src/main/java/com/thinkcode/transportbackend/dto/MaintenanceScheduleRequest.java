package com.thinkcode.transportbackend.dto;

import com.thinkcode.transportbackend.entity.ScheduleStatus;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.UUID;

public record MaintenanceScheduleRequest(
        @NotNull UUID vehicleId,
        @NotNull LocalDate plannedDate,
        ScheduleStatus status,
        String note
) {
}
