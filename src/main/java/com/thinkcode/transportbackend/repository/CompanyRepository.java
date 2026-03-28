package com.thinkcode.transportbackend.repository;

import com.thinkcode.transportbackend.entity.Company;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyRepository extends JpaRepository<Company, UUID> {

    Optional<Company> findByCode(String code);
}

