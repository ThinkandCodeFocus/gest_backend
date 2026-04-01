package com.thinkcode.transportbackend.service;

import com.thinkcode.transportbackend.dto.RevenueRuleResponse;
import com.thinkcode.transportbackend.entity.Company;
import com.thinkcode.transportbackend.entity.RevenueRule;
import com.thinkcode.transportbackend.repository.CompanyRepository;
import com.thinkcode.transportbackend.repository.RevenueRuleRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class RevenueRuleService {

    private final RevenueRuleRepository revenueRuleRepository;
    private final CompanyRepository companyRepository;

    public RevenueRuleService(RevenueRuleRepository revenueRuleRepository, CompanyRepository companyRepository) {
        this.revenueRuleRepository = revenueRuleRepository;
        this.companyRepository = companyRepository;
    }

    public List<RevenueRule> findByCompanyId(UUID companyId) {
        return revenueRuleRepository.findByCompanyIdAndActiveTrue(companyId);
    }

    public List<RevenueRuleResponse> findResponsesByCompanyId(UUID companyId) {
        return findByCompanyId(companyId).stream()
                .map(this::toResponse)
                .toList();
    }

    public RevenueRule findByCompanyIdAndType(UUID companyId, String ruleType) {
        return revenueRuleRepository.findByCompanyIdAndRuleType(companyId, ruleType).orElse(null);
    }

    public RevenueRule save(UUID companyId, RevenueRule rule) {
        Company company = companyRepository.findById(companyId).orElse(null);
        if (company == null) {
            throw new IllegalArgumentException("Company not found: " + companyId);
        }
        rule.setCompany(company);
        return revenueRuleRepository.save(rule);
    }

    public RevenueRuleResponse saveResponse(UUID companyId, RevenueRule rule) {
        return toResponse(save(companyId, rule));
    }

    public RevenueRule update(UUID ruleId, RevenueRule rule) {
        return revenueRuleRepository.findById(ruleId)
                .map(existing -> {
                    existing.setRuleValue(rule.getRuleValue());
                    existing.setDescription(rule.getDescription());
                    existing.setActive(rule.getActive());
                    return revenueRuleRepository.save(existing);
                })
                .orElse(null);
    }

    public RevenueRuleResponse updateResponse(UUID ruleId, RevenueRule rule) {
        RevenueRule updated = update(ruleId, rule);
        return updated == null ? null : toResponse(updated);
    }

    public void delete(UUID ruleId) {
        revenueRuleRepository.deleteById(ruleId);
    }

    private RevenueRuleResponse toResponse(RevenueRule rule) {
        return new RevenueRuleResponse(
                rule.getId(),
                rule.getRuleType(),
                rule.getRuleValue(),
                rule.getDescription(),
                rule.getActive()
        );
    }
}
