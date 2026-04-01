package com.thinkcode.transportbackend.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record RevenueRuleResponse(
        UUID id,
        String ruleType,
        BigDecimal ruleValue,
        String description,
        Boolean active
) {
}
