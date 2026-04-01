package com.thinkcode.transportbackend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.thinkcode.transportbackend.dto.VehicleRequest;
import com.thinkcode.transportbackend.entity.Company;
import com.thinkcode.transportbackend.entity.Vehicle;
import com.thinkcode.transportbackend.entity.VehicleType;
import com.thinkcode.transportbackend.repository.VehicleRepository;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class VehicleServiceOwnershipTest {

    @Mock
    private VehicleRepository vehicleRepository;

    @Mock
    private CompanyResolver companyResolver;

    @Mock
    private DriverService driverService;

    @Mock
    private ClientService clientService;

    @Mock
    private AuthenticatedCompanyProvider authenticatedCompanyProvider;

    private VehicleService vehicleService;

    @BeforeEach
    void setUp() {
        vehicleService = new VehicleService(
                vehicleRepository,
                companyResolver,
                driverService,
                clientService,
                authenticatedCompanyProvider
        );
    }

    @Test
    void createShouldUseAuthenticatedCompany() {
        UUID authCompanyId = UUID.randomUUID();
        Company company = new Company();
        Vehicle saved = new Vehicle();

        when(authenticatedCompanyProvider.requireCompanyId()).thenReturn(authCompanyId);
        when(vehicleRepository.findByMatricule("DK-100-AA")).thenReturn(Optional.empty());
        when(companyResolver.require(authCompanyId)).thenReturn(company);
        when(vehicleRepository.save(any(Vehicle.class))).thenReturn(saved);

        VehicleRequest request = new VehicleRequest(
                "DK-100-AA",
                VehicleType.CAR,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                BigDecimal.ZERO,
                null,
                BigDecimal.ZERO
        );

        Vehicle result = vehicleService.create(request);

        assertEquals(saved, result);
        verify(companyResolver).require(authCompanyId);
    }

    @Test
    void findByIdForCompanyShouldReturnVehicleWhenOwnedByCompany() {
        UUID vehicleId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        Vehicle vehicle = new Vehicle();

        when(vehicleRepository.findByIdAndCompanyId(vehicleId, companyId)).thenReturn(Optional.of(vehicle));

        Vehicle result = vehicleService.findByIdForCompany(vehicleId, companyId);
        assertEquals(vehicle, result);
    }

    @Test
    void findByIdForCompanyShouldThrowWhenVehicleOutsideCompany() {
        UUID vehicleId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();

        when(vehicleRepository.findByIdAndCompanyId(any(), any())).thenReturn(Optional.empty());

        ApiException exception = assertThrows(ApiException.class, () -> vehicleService.findByIdForCompany(vehicleId, companyId));
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
    }
}
