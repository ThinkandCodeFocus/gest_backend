package com.thinkcode.transportbackend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record ClientAccountSummary(
        BigDecimal expectedRevenue,
        BigDecimal actualRevenue,
        BigDecimal balance,
        List<ClientAccountEntry> entries
) {
    public record ClientAccountEntry(
            LocalDate date,
            String type, // "REVENUE", "MAINTENANCE", "DEBT"
            String description,
            BigDecimal amount,
            String vehicleMatricule,
            UUID vehicleId,
            String observation,
            String documentUrl
    ) {}
}
