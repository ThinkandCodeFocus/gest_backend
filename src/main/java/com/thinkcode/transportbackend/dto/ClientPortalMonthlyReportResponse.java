package com.thinkcode.transportbackend.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record ClientPortalMonthlyReportResponse(
        UUID vehicleId,
        String matricule,
        String month,
        String clientName,
        String vehicleType,
        String chauffeur,
        String emailClient,
        String phoneClient,
        BigDecimal amortissement,
        MonthlySummary summary,
        List<DailyVehicleRow> days
) {

    public record MonthlySummary(
            int nbJours,
            int joursAvecActivite,
            BigDecimal recetteCaisse,
            BigDecimal recetteChauffeur,
            BigDecimal recetteService,
            BigDecimal recetteClient,
            BigDecimal amendes,
            BigDecimal vidange,
            BigDecimal pannes,
            BigDecimal autres,
            BigDecimal totalDepenses,
            BigDecimal recetteMoisPrecedent,
            BigDecimal rentabilite,
            BigDecimal detteChauffeur
    ) {
    }

    public record DailyVehicleRow(
            String iso,
            String label,
            BigDecimal recetteCaisse,
            BigDecimal recetteChauffeur,
            BigDecimal recetteService,
            BigDecimal recetteClient,
            BigDecimal amendes,
            BigDecimal vidange,
            BigDecimal pannes,
            BigDecimal autres,
            String commentaires,
            String origineFonds
    ) {
    }
}
