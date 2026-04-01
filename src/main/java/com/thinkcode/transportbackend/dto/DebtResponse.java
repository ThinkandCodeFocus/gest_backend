package com.thinkcode.transportbackend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record DebtResponse(
        UUID id,
        Integer rowNumber,
        LocalDate date,
        UUID vehicleId,
        String matricule,
        UUID driverId,
        String chauffeur,
        String beneficiaire,
        String client,
        String typeDette,
        String motif,
        BigDecimal montant,
        String statut,
        BigDecimal paidAmount,
        BigDecimal remainingAmount
) {
}
