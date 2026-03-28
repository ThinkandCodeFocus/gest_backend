package com.thinkcode.transportbackend.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.UUID;

public record InvoiceEmailRequest(
        @NotNull UUID clientId,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate,
        String recipientEmail
) {
}
