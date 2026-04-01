package com.thinkcode.transportbackend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record VehicleResponse(
        UUID id,
        String matricule,
        String type,
        UUID driverId,
        String chauffeur,
        UUID clientId,
        String client,
        String emailClient,
        String numeroTelephoneClient,
        LocalDate dateDebut,
        BigDecimal amortissement,
        String statutGlobal,
        BigDecimal dailyTarget
) {
}
