package com.thinkcode.transportbackend.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record DebtPaymentRequest(@NotNull BigDecimal amount) {
}

