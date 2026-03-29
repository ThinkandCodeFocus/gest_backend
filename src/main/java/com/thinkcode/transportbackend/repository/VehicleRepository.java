package com.thinkcode.transportbackend.repository;

import com.thinkcode.transportbackend.entity.Vehicle;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VehicleRepository extends JpaRepository<Vehicle, UUID> {

    List<Vehicle> findAllByCompanyId(UUID companyId);

    Optional<Vehicle> findByIdAndCompanyId(UUID id, UUID companyId);

    Optional<Vehicle> findByMatricule(String matricule);

    List<Vehicle> findByCompanyIdAndClientId(UUID companyId, UUID clientId);
}

