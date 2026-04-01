package com.thinkcode.transportbackend.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thinkcode.transportbackend.dto.MaintenanceFraudAlertResponse;
import com.thinkcode.transportbackend.dto.MaintenancePieceRequest;
import com.thinkcode.transportbackend.dto.MaintenanceRequest;
import com.thinkcode.transportbackend.dto.MaintenanceResponse;
import com.thinkcode.transportbackend.entity.MaintenanceRecord;
import com.thinkcode.transportbackend.entity.MaintenanceType;
import com.thinkcode.transportbackend.entity.Vehicle;
import com.thinkcode.transportbackend.repository.MaintenanceRecordRepository;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class MaintenanceService {

    private final MaintenanceRecordRepository maintenanceRecordRepository;
    private final VehicleService vehicleService;
    private final AuthenticatedCompanyProvider authenticatedCompanyProvider;
    private final ObjectMapper objectMapper;

    public MaintenanceService(
            MaintenanceRecordRepository maintenanceRecordRepository,
            VehicleService vehicleService,
            AuthenticatedCompanyProvider authenticatedCompanyProvider,
            ObjectMapper objectMapper
    ) {
        this.maintenanceRecordRepository = maintenanceRecordRepository;
        this.vehicleService = vehicleService;
        this.authenticatedCompanyProvider = authenticatedCompanyProvider;
        this.objectMapper = objectMapper;
    }

    public List<MaintenanceResponse> findAll(UUID companyId) {
        return maintenanceRecordRepository.findAllByVehicleCompanyId(companyId).stream()
                .sorted(Comparator.comparing(MaintenanceRecord::getMaintenanceDate).reversed())
                .map(this::toResponse)
                .toList();
    }

    public MaintenanceRecord findByIdForCompany(UUID maintenanceId, UUID companyId) {
        return maintenanceRecordRepository.findByIdAndVehicleCompanyId(maintenanceId, companyId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Maintenance record not found"));
    }

    public MaintenanceResponse create(MaintenanceRequest request) {
        UUID companyId = authenticatedCompanyProvider.requireCompanyId();
        Vehicle vehicle = vehicleService.findByIdForCompany(request.vehicleId(), companyId);
        MaintenanceRecord record = new MaintenanceRecord();
        applyRequest(record, request, vehicle, companyId);
        return toResponse(maintenanceRecordRepository.save(record));
    }

    public MaintenanceResponse update(UUID maintenanceId, MaintenanceRequest request) {
        UUID companyId = authenticatedCompanyProvider.requireCompanyId();
        MaintenanceRecord record = findByIdForCompany(maintenanceId, companyId);
        Vehicle vehicle = vehicleService.findByIdForCompany(request.vehicleId(), companyId);
        applyRequest(record, request, vehicle, companyId);
        return toResponse(maintenanceRecordRepository.save(record));
    }

    public void delete(UUID maintenanceId) {
        UUID companyId = authenticatedCompanyProvider.requireCompanyId();
        MaintenanceRecord record = findByIdForCompany(maintenanceId, companyId);
        maintenanceRecordRepository.delete(record);
    }

    public List<MaintenanceFraudAlertResponse> getFraudAlerts() {
        UUID companyId = authenticatedCompanyProvider.requireCompanyId();
        return maintenanceRecordRepository.findAllByVehicleCompanyId(companyId).stream()
                .map(this::toAlert)
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(MaintenanceFraudAlertResponse::date).reversed())
                .toList();
    }

    private void applyRequest(MaintenanceRecord record, MaintenanceRequest request, Vehicle vehicle, UUID companyId) {
        List<MaintenancePieceRequest> pieces = sanitizePieces(request.pieces());
        BigDecimal partsCost = pieces.stream()
                .map(MaintenancePieceRequest::prixPiece)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal laborCost = request.laborCost() == null ? BigDecimal.ZERO : request.laborCost();
        BigDecimal total = request.cost();
        if (total == null || total.compareTo(BigDecimal.ZERO) <= 0) {
            total = partsCost.add(laborCost);
        }

        FraudCheck fraudCheck = detectFraud(record.getId(), companyId, vehicle.getId(), request.type(), request.maintenanceDate(), request.description(), partsCost);

        record.setVehicle(vehicle);
        record.setType(request.type());
        record.setMaintenanceDate(request.maintenanceDate());
        record.setCost(total);
        record.setDescription(request.description());
        record.setDocumentUrl(request.documentUrl());
        record.setPartsCost(partsCost);
        record.setLaborCost(laborCost);
        record.setPieceCount(resolvePieceCount(request, pieces));
        record.setProvider(request.provider());
        record.setSuspectedDuplicate(fraudCheck.suspectedDuplicate());
        record.setFraudReason(fraudCheck.reason());
        record.setPieceLinesJson(writePieces(pieces));
    }

    private Integer resolvePieceCount(MaintenanceRequest request, List<MaintenancePieceRequest> pieces) {
        if (!pieces.isEmpty()) {
            return pieces.size();
        }
        if (request.pieceCount() != null && request.pieceCount() > 0) {
            return request.pieceCount();
        }
        return 1;
    }

    private List<MaintenancePieceRequest> sanitizePieces(List<MaintenancePieceRequest> pieces) {
        if (pieces == null) {
            return List.of();
        }
        return pieces.stream()
                .filter(Objects::nonNull)
                .map(piece -> new MaintenancePieceRequest(piece.description().trim(), piece.prixPiece()))
                .toList();
    }

    private FraudCheck detectFraud(UUID currentMaintenanceId, UUID companyId, UUID vehicleId, MaintenanceType type, LocalDate maintenanceDate, String description, BigDecimal partsCost) {
        List<MaintenanceRecord> recent = maintenanceRecordRepository.findRecentForVehicle(
                companyId,
                vehicleId,
                maintenanceDate.minusDays(30),
                maintenanceDate.plusDays(30)
        );

        String normalizedDescription = normalize(description);
        for (MaintenanceRecord existing : recent) {
            if (currentMaintenanceId != null && currentMaintenanceId.equals(existing.getId())) {
                continue;
            }
            if (sameRecordWindow(existing, maintenanceDate, type)) {
                String existingDescription = normalize(existing.getDescription());
                boolean similarDescription = !normalizedDescription.isBlank()
                        && !existingDescription.isBlank()
                        && (existingDescription.contains(normalizedDescription)
                        || normalizedDescription.contains(existingDescription)
                        || similarityScore(existingDescription, normalizedDescription) >= 0.72d);

                boolean closeCost = partsCost.compareTo(BigDecimal.ZERO) > 0
                        && existing.getPartsCost() != null
                        && existing.getPartsCost().subtract(partsCost).abs().compareTo(new BigDecimal("5000")) <= 0;

                if (similarDescription || closeCost) {
                    String reason = similarDescription
                            ? "Description tres proche d'une maintenance recente sur 30 jours"
                            : "Montant pieces tres proche d'une maintenance recente sur 30 jours";
                    return new FraudCheck(true, reason);
                }
            }
        }

        return new FraudCheck(false, null);
    }

    private boolean sameRecordWindow(MaintenanceRecord existing, LocalDate maintenanceDate, MaintenanceType type) {
        return existing.getMaintenanceDate() != null
                && existing.getType() == type
                && !existing.getMaintenanceDate().isAfter(maintenanceDate.plusDays(30))
                && !existing.getMaintenanceDate().isBefore(maintenanceDate.minusDays(30));
    }

    private MaintenanceFraudAlertResponse toAlert(MaintenanceRecord record) {
        if (!record.isSuspectedDuplicate()) {
            return null;
        }
        return new MaintenanceFraudAlertResponse(
                record.getId(),
                record.getVehicle().getId(),
                record.getVehicle().getMatricule(),
                record.getMaintenanceDate(),
                "Suspicion",
                record.getDescription(),
                record.getFraudReason() == null ? "Proximite detectee sur historique maintenance" : record.getFraudReason()
        );
    }

    private MaintenanceResponse toResponse(MaintenanceRecord record) {
        List<MaintenancePieceRequest> pieces = readPieces(record.getPieceLinesJson());
        return new MaintenanceResponse(
                record.getId(),
                record.getMaintenanceDate(),
                record.getVehicle().getId(),
                record.getVehicle().getMatricule(),
                record.getType(),
                record.getDescription(),
                record.getPieceCount(),
                pieces,
                defaultBigDecimal(record.getPartsCost()),
                defaultBigDecimal(record.getLaborCost()),
                defaultBigDecimal(record.getCost()),
                record.getProvider(),
                record.getDocumentUrl(),
                record.isSuspectedDuplicate(),
                record.getFraudReason()
        );
    }

    private BigDecimal defaultBigDecimal(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String writePieces(List<MaintenancePieceRequest> pieces) {
        try {
            return objectMapper.writeValueAsString(pieces == null ? List.of() : pieces);
        } catch (Exception ex) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to store maintenance pieces");
        }
    }

    private List<MaintenancePieceRequest> readPieces(String rawJson) {
        if (rawJson == null || rawJson.isBlank()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(rawJson, new TypeReference<List<MaintenancePieceRequest>>() {});
        } catch (Exception ex) {
            return List.of();
        }
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("[^a-zA-Z0-9\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim()
                .toLowerCase();
        return normalized;
    }

    private double similarityScore(String left, String right) {
        if (left.equals(right)) {
            return 1d;
        }
        if (left.isBlank() || right.isBlank()) {
            return 0d;
        }
        int common = 0;
        String[] leftTokens = left.split(" ");
        String[] rightTokens = right.split(" ");
        for (String leftToken : leftTokens) {
            for (String rightToken : rightTokens) {
                if (leftToken.equals(rightToken)) {
                    common++;
                    break;
                }
            }
        }
        return (2d * common) / (leftTokens.length + rightTokens.length);
    }

    private record FraudCheck(boolean suspectedDuplicate, String reason) {
    }
}
