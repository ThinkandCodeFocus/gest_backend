package com.thinkcode.transportbackend.repository;

import com.thinkcode.transportbackend.entity.Client;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClientRepository extends JpaRepository<Client, UUID> {

    List<Client> findAllByCompanyId(UUID companyId);

    Optional<Client> findByIdAndCompanyId(UUID id, UUID companyId);

    Optional<Client> findByCompanyIdAndEmail(UUID companyId, String email);

    Optional<Client> findByCompanyIdAndNameIgnoreCase(UUID companyId, String name);
}

