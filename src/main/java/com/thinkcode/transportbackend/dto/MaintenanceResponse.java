package com.thinkcode.transportbackend.dto;

import com.thinkcode.transportbackend.entity.MaintenanceType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record MaintenanceResponse(
        UUID id,
        LocalDate maintenanceDate,
        UUID vehicleId,
        String matricule,
        MaintenanceType type,
        String description,
        Integer pieceCount,
        List<MaintenancePieceRequest> pieces,
        BigDecimal prixPiece,
        BigDecimal mo,
        BigDecimal total,
        String prestataire,
        String lienPDF,
        boolean suspectedDuplicate,
        String fraudReason
) {
}
