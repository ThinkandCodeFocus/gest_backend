package com.thinkcode.transportbackend.dto;

import java.time.LocalDate;
import java.util.UUID;

public record PlanningEventResponse(
        UUID id,
        LocalDate date,
        String slot,
        String type,
        String title,
        UUID ownerId,
        String ownerName,
        String priority
) {
}
