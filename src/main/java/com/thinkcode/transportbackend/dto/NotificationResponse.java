package com.thinkcode.transportbackend.dto;

import com.thinkcode.transportbackend.entity.NotificationType;
import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        String title,
        String message,
        NotificationType type,
        boolean read,
        Instant createdAt
) {
}
