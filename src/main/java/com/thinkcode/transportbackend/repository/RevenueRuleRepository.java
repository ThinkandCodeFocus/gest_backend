package com.thinkcode.transportbackend.repository;

import com.thinkcode.transportbackend.entity.RevenueRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RevenueRuleRepository extends JpaRepository<RevenueRule, UUID> {
    List<RevenueRule> findByCompanyIdAndActiveTrue(UUID companyId);
    Optional<RevenueRule> findByCompanyIdAndRuleType(UUID companyId, String ruleType);
}
