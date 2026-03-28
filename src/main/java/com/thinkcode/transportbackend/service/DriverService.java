package com.thinkcode.transportbackend.service;

import com.thinkcode.transportbackend.entity.Driver;
import com.thinkcode.transportbackend.repository.DriverRepository;
import com.thinkcode.transportbackend.dto.DriverRequest;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class DriverService {

    private final DriverRepository driverRepository;
    private final CompanyResolver companyResolver;
    private final AuthenticatedCompanyProvider authenticatedCompanyProvider;

    public DriverService(
            DriverRepository driverRepository,
            CompanyResolver companyResolver,
            AuthenticatedCompanyProvider authenticatedCompanyProvider
    ) {
        this.driverRepository = driverRepository;
        this.companyResolver = companyResolver;
        this.authenticatedCompanyProvider = authenticatedCompanyProvider;
    }

    public List<Driver> findAll(UUID companyId) {
        return driverRepository.findAllByCompanyId(companyId);
    }

    public Driver findById(UUID id) {
        return driverRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Driver not found"));
    }

    public Driver findByIdForCompany(UUID id, UUID companyId) {
        return driverRepository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Driver not found"));
    }

    public Driver create(DriverRequest request) {
        UUID authenticatedCompanyId = authenticatedCompanyProvider.requireCompanyId();
        Driver driver = new Driver();
        driver.setFullName(request.fullName());
        driver.setPhoneNumber(request.phoneNumber());
        driver.setLicenseNumber(request.licenseNumber());
        driver.setCompany(companyResolver.require(authenticatedCompanyId));
        return driverRepository.save(driver);
    }

    public Driver update(UUID driverId, DriverRequest request) {
        UUID authenticatedCompanyId = authenticatedCompanyProvider.requireCompanyId();
        Driver driver = findByIdForCompany(driverId, authenticatedCompanyId);
        driver.setFullName(request.fullName());
        driver.setPhoneNumber(request.phoneNumber());
        driver.setLicenseNumber(request.licenseNumber());
        return driverRepository.save(driver);
    }

    public void delete(UUID driverId) {
        UUID authenticatedCompanyId = authenticatedCompanyProvider.requireCompanyId();
        Driver driver = findByIdForCompany(driverId, authenticatedCompanyId);
        driverRepository.delete(driver);
    }
}

