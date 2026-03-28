package com.thinkcode.transportbackend.repository;

import com.thinkcode.transportbackend.entity.Driver;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DriverRepository extends JpaRepository<Driver, UUID> {

    List<Driver> findAllByCompanyId(UUID companyId);

    Optional<Driver> findByIdAndCompanyId(UUID id, UUID companyId);
}

