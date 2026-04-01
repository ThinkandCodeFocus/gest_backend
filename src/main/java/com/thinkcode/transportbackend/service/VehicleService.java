package com.thinkcode.transportbackend.service;

import com.thinkcode.transportbackend.dto.VehicleResponse;
import com.thinkcode.transportbackend.entity.Vehicle;
import com.thinkcode.transportbackend.entity.VehicleStatus;
import com.thinkcode.transportbackend.repository.VehicleRepository;
import com.thinkcode.transportbackend.dto.VehicleRequest;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class VehicleService {

    private final VehicleRepository vehicleRepository;
    private final CompanyResolver companyResolver;
    private final DriverService driverService;
    private final ClientService clientService;
    private final AuthenticatedCompanyProvider authenticatedCompanyProvider;

    public VehicleService(
            VehicleRepository vehicleRepository,
            CompanyResolver companyResolver,
            DriverService driverService,
            ClientService clientService,
            AuthenticatedCompanyProvider authenticatedCompanyProvider
    ) {
        this.vehicleRepository = vehicleRepository;
        this.companyResolver = companyResolver;
        this.driverService = driverService;
        this.clientService = clientService;
        this.authenticatedCompanyProvider = authenticatedCompanyProvider;
    }

    public List<VehicleResponse> findAll(UUID companyId) {
        return vehicleRepository.findAllByCompanyId(companyId).stream().map(this::toResponse).toList();
    }

    public Vehicle findById(UUID id) {
        return vehicleRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Vehicle not found"));
    }

    public Vehicle findByIdForCompany(UUID id, UUID companyId) {
        return vehicleRepository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Vehicle not found"));
    }

    public Vehicle create(VehicleRequest request) {
        UUID authenticatedCompanyId = authenticatedCompanyProvider.requireCompanyId();
        vehicleRepository.findByMatricule(request.matricule().trim().toUpperCase())
                .ifPresent(vehicle -> {
                    throw new ApiException(HttpStatus.CONFLICT, "Vehicle matricule already exists");
                });

        Vehicle vehicle = new Vehicle();
        vehicle.setMatricule(request.matricule());
        vehicle.setType(request.type());
        vehicle.setCompany(companyResolver.require(authenticatedCompanyId));
        vehicle.setStatus(request.status() == null ? VehicleStatus.AVAILABLE : request.status());
        vehicle.setAmortization(request.amortization());
        vehicle.setStartDate(request.startDate());
        vehicle.setDailyTarget(request.dailyTarget() == null ? BigDecimal.ZERO : request.dailyTarget());

        if (request.driverId() != null) {
            vehicle.setDriver(driverService.findByIdForCompany(request.driverId(), authenticatedCompanyId));
        } else if (request.driverName() != null && !request.driverName().isBlank()) {
            vehicle.setDriver(driverService.findByNameForCompany(request.driverName(), authenticatedCompanyId));
        }
        if (request.clientId() != null) {
            var client = clientService.findByIdForCompany(request.clientId(), authenticatedCompanyId);
            if (request.clientEmail() != null && !request.clientEmail().isBlank()) {
                client.setEmail(request.clientEmail().trim());
            }
            if (request.clientPhoneNumber() != null && !request.clientPhoneNumber().isBlank()) {
                client.setPhoneNumber(request.clientPhoneNumber().trim());
            }
            vehicle.setClient(client);
        } else if (request.clientName() != null && !request.clientName().isBlank()) {
            vehicle.setClient(clientService.findOrCreateForCompany(
                    authenticatedCompanyId,
                    request.clientName(),
                    request.clientEmail(),
                    request.clientPhoneNumber()
            ));
        }
        return vehicleRepository.save(vehicle);
    }

    public Vehicle update(UUID vehicleId, VehicleRequest request) {
        UUID authenticatedCompanyId = authenticatedCompanyProvider.requireCompanyId();
        Vehicle vehicle = findByIdForCompany(vehicleId, authenticatedCompanyId);

        String normalizedMatricule = request.matricule().trim().toUpperCase();
        if (!normalizedMatricule.equals(vehicle.getMatricule())) {
            vehicleRepository.findByMatricule(normalizedMatricule)
                    .ifPresent(existing -> {
                        throw new ApiException(HttpStatus.CONFLICT, "Vehicle matricule already exists");
                    });
        }

        vehicle.setMatricule(request.matricule());
        vehicle.setType(request.type());
        vehicle.setStatus(request.status() == null ? VehicleStatus.AVAILABLE : request.status());
        vehicle.setAmortization(request.amortization());
        vehicle.setStartDate(request.startDate());
        vehicle.setDailyTarget(request.dailyTarget() == null ? BigDecimal.ZERO : request.dailyTarget());

        if (request.driverId() != null) {
            vehicle.setDriver(driverService.findByIdForCompany(request.driverId(), authenticatedCompanyId));
        } else if (request.driverName() != null && !request.driverName().isBlank()) {
            vehicle.setDriver(driverService.findByNameForCompany(request.driverName(), authenticatedCompanyId));
        } else {
            vehicle.setDriver(null);
        }

        if (request.clientId() != null) {
            var client = clientService.findByIdForCompany(request.clientId(), authenticatedCompanyId);
            if (request.clientEmail() != null && !request.clientEmail().isBlank()) {
                client.setEmail(request.clientEmail().trim());
            }
            if (request.clientPhoneNumber() != null && !request.clientPhoneNumber().isBlank()) {
                client.setPhoneNumber(request.clientPhoneNumber().trim());
            }
            vehicle.setClient(client);
        } else if (request.clientName() != null && !request.clientName().isBlank()) {
            vehicle.setClient(clientService.findOrCreateForCompany(
                    authenticatedCompanyId,
                    request.clientName(),
                    request.clientEmail(),
                    request.clientPhoneNumber()
            ));
        } else {
            vehicle.setClient(null);
        }

        return vehicleRepository.save(vehicle);
    }

    public void delete(UUID vehicleId) {
        UUID authenticatedCompanyId = authenticatedCompanyProvider.requireCompanyId();
        Vehicle vehicle = findByIdForCompany(vehicleId, authenticatedCompanyId);
        vehicleRepository.delete(vehicle);
    }

    public VehicleResponse toResponse(Vehicle vehicle) {
        return new VehicleResponse(
                vehicle.getId(),
                vehicle.getMatricule(),
                vehicle.getType() == null ? null : switch (vehicle.getType()) {
                    case CAR -> "Voiture";
                    case MOTO_TAXI -> "Moto-taxi";
                    case MOTORBIKE -> "Moto";
                },
                vehicle.getDriver() == null ? null : vehicle.getDriver().getId(),
                vehicle.getDriver() == null ? null : vehicle.getDriver().getFullName(),
                vehicle.getClient() == null ? null : vehicle.getClient().getId(),
                vehicle.getClient() == null ? null : vehicle.getClient().getName(),
                vehicle.getClient() == null ? null : vehicle.getClient().getEmail(),
                vehicle.getClient() == null ? null : vehicle.getClient().getPhoneNumber(),
                vehicle.getStartDate(),
                vehicle.getAmortization(),
                vehicle.getStatus() == null ? null : switch (vehicle.getStatus()) {
                    case AVAILABLE, ASSIGNED -> "Actif";
                    case IN_MAINTENANCE -> "Maintenance";
                    case OUT_OF_SERVICE -> "Panne";
                },
                vehicle.getDailyTarget()
        );
    }
}

