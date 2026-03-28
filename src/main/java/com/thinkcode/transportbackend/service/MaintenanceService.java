package com.thinkcode.transportbackend.service;

import com.thinkcode.transportbackend.entity.MaintenanceRecord;
import com.thinkcode.transportbackend.repository.MaintenanceRecordRepository;
import com.thinkcode.transportbackend.dto.MaintenanceRequest;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class MaintenanceService {

    private final MaintenanceRecordRepository maintenanceRecordRepository;
    private final VehicleService vehicleService;
    private final AuthenticatedCompanyProvider authenticatedCompanyProvider;

    public MaintenanceService(
            MaintenanceRecordRepository maintenanceRecordRepository,
            VehicleService vehicleService,
            AuthenticatedCompanyProvider authenticatedCompanyProvider
    ) {
        this.maintenanceRecordRepository = maintenanceRecordRepository;
        this.vehicleService = vehicleService;
        this.authenticatedCompanyProvider = authenticatedCompanyProvider;
    }

    public List<MaintenanceRecord> findAll(UUID companyId) {
        return maintenanceRecordRepository.findAllByVehicleCompanyId(companyId);
    }

    public MaintenanceRecord findByIdForCompany(UUID maintenanceId, UUID companyId) {
        return maintenanceRecordRepository.findByIdAndVehicleCompanyId(maintenanceId, companyId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Maintenance record not found"));
    }

    public MaintenanceRecord create(MaintenanceRequest request) {
        UUID authenticatedCompanyId = authenticatedCompanyProvider.requireCompanyId();
        MaintenanceRecord record = new MaintenanceRecord();
        record.setVehicle(vehicleService.findByIdForCompany(request.vehicleId(), authenticatedCompanyId));
        record.setType(request.type());
        record.setMaintenanceDate(request.maintenanceDate());
        record.setCost(request.cost());
        record.setDescription(request.description());
        record.setDocumentUrl(request.documentUrl());
        record.setSuspectedDuplicate(isPotentialDuplicate(request.description()));
        return maintenanceRecordRepository.save(record);
    }

    public MaintenanceRecord update(UUID maintenanceId, MaintenanceRequest request) {
        UUID authenticatedCompanyId = authenticatedCompanyProvider.requireCompanyId();
        MaintenanceRecord record = findByIdForCompany(maintenanceId, authenticatedCompanyId);
        record.setVehicle(vehicleService.findByIdForCompany(request.vehicleId(), authenticatedCompanyId));
        record.setType(request.type());
        record.setMaintenanceDate(request.maintenanceDate());
        record.setCost(request.cost());
        record.setDescription(request.description());
        record.setDocumentUrl(request.documentUrl());
        record.setSuspectedDuplicate(isPotentialDuplicate(request.description()));
        return maintenanceRecordRepository.save(record);
    }

    public void delete(UUID maintenanceId) {
        UUID authenticatedCompanyId = authenticatedCompanyProvider.requireCompanyId();
        MaintenanceRecord record = findByIdForCompany(maintenanceId, authenticatedCompanyId);
        maintenanceRecordRepository.delete(record);
    }

    private boolean isPotentialDuplicate(String description) {
        String normalized = description.trim().toLowerCase();
        return normalized.contains("meme") || normalized.contains("duplicate");
    }
}

