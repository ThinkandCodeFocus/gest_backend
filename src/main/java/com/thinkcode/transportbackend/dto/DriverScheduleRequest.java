package com.thinkcode.transportbackend.dto;

import com.thinkcode.transportbackend.entity.ScheduleStatus;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record DriverScheduleRequest(
        @NotNull UUID driverId,
        @NotNull LocalDate scheduleDate,
        LocalTime shiftStart,
        LocalTime shiftEnd,
        ScheduleStatus status,
        String note
) {
}
