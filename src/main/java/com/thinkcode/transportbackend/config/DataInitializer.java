package com.thinkcode.transportbackend.config;

import com.thinkcode.transportbackend.entity.Client;
import com.thinkcode.transportbackend.entity.Company;
import com.thinkcode.transportbackend.entity.Driver;
import com.thinkcode.transportbackend.entity.RoleName;
import com.thinkcode.transportbackend.entity.UserAccount;
import com.thinkcode.transportbackend.entity.Vehicle;
import com.thinkcode.transportbackend.entity.VehicleStatus;
import com.thinkcode.transportbackend.entity.VehicleType;
import com.thinkcode.transportbackend.repository.ClientRepository;
import com.thinkcode.transportbackend.repository.CompanyRepository;
import com.thinkcode.transportbackend.repository.DriverRepository;
import com.thinkcode.transportbackend.repository.UserAccountRepository;
import com.thinkcode.transportbackend.repository.VehicleRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private final CompanyRepository companyRepository;
    private final UserAccountRepository userAccountRepository;
    private final DriverRepository driverRepository;
    private final ClientRepository clientRepository;
    private final VehicleRepository vehicleRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(
            CompanyRepository companyRepository,
            UserAccountRepository userAccountRepository,
            DriverRepository driverRepository,
            ClientRepository clientRepository,
            VehicleRepository vehicleRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.companyRepository = companyRepository;
        this.userAccountRepository = userAccountRepository;
        this.driverRepository = driverRepository;
        this.clientRepository = clientRepository;
        this.vehicleRepository = vehicleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (companyRepository.count() > 0) {
            return;
        }

        Company company = new Company();
        company.setName("Think Transport");
        company.setCode("TT01");
        company = companyRepository.save(company);

        UserAccount admin = new UserAccount();
        admin.setFullName("Admin Demo");
        admin.setEmail("admin@demo.local");
        admin.setPasswordHash(passwordEncoder.encode("admin123"));
        admin.setRole(RoleName.ADMIN);
        admin.setCompany(company);
        userAccountRepository.save(admin);

        Driver driver = new Driver();
        driver.setFullName("Jean Chauffeur");
        driver.setPhoneNumber("+221700000000");
        driver.setLicenseNumber("PERMIS-001");
        driver.setCompany(company);
        driver = driverRepository.save(driver);

        Client client = new Client();
        client.setName("Client DÃ©mo");
        client.setEmail("client@demo.local");
        client.setPhoneNumber("+221711111111");
        client.setCompany(company);
        client = clientRepository.save(client);

        Vehicle vehicle = new Vehicle();
        vehicle.setMatricule("DK-001-AA");
        vehicle.setType(VehicleType.CAR);
        vehicle.setStatus(VehicleStatus.ASSIGNED);
        vehicle.setDriver(driver);
        vehicle.setClient(client);
        vehicle.setCompany(company);
        vehicle.setDailyTarget(new BigDecimal("25000"));
        vehicle.setAmortization(new BigDecimal("1500000"));
        vehicle.setStartDate(LocalDate.now().minusMonths(2));
        vehicleRepository.save(vehicle);
    }
}

