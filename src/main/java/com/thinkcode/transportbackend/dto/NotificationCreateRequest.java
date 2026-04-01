package com.thinkcode.transportbackend.dto;

import com.thinkcode.transportbackend.entity.NotificationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record NotificationCreateRequest(
        @NotBlank String title,
        @NotBlank String message,
        String link,
        @NotNull NotificationType type
) {
}
