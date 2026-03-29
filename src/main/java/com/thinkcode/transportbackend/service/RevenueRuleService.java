package com.thinkcode.transportbackend.service;

import com.thinkcode.transportbackend.entity.Company;
import com.thinkcode.transportbackend.entity.RevenueRule;
import com.thinkcode.transportbackend.repository.CompanyRepository;
import com.thinkcode.transportbackend.repository.RevenueRuleRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

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

    public void delete(UUID ruleId) {
        revenueRuleRepository.deleteById(ruleId);
    }
}
