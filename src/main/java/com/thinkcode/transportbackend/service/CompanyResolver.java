package com.thinkcode.transportbackend.service;

import com.thinkcode.transportbackend.entity.Company;
import com.thinkcode.transportbackend.repository.CompanyRepository;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class CompanyResolver {

    private final CompanyRepository companyRepository;

    public CompanyResolver(CompanyRepository companyRepository) {
        this.companyRepository = companyRepository;
    }

    public Company require(UUID companyId) {
        return companyRepository.findById(companyId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Company not found"));
    }
}

