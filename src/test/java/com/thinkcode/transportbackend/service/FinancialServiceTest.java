package com.thinkcode.transportbackend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import com.thinkcode.transportbackend.dto.FinancialSummaryResponse;
import com.thinkcode.transportbackend.entity.FinancialEntry;
import com.thinkcode.transportbackend.entity.FinancialEntryType;
import com.thinkcode.transportbackend.repository.FinancialEntryRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FinancialServiceTest {

    @Mock
    private FinancialEntryRepository financialEntryRepository;

    @Mock
    private CompanyResolver companyResolver;

    @Mock
    private AuthenticatedCompanyProvider authenticatedCompanyProvider;

    @Mock
    private AuditLogService auditLogService;

    private FinancialService financialService;

    @BeforeEach
    void setUp() {
        financialService = new FinancialService(
                financialEntryRepository,
                companyResolver,
                authenticatedCompanyProvider,
                auditLogService
        );
    }

    @Test
    void summaryShouldAggregateIncomeExpenseAndBalance() {
        UUID companyId = UUID.randomUUID();
        LocalDate startDate = LocalDate.now().minusDays(30);
        LocalDate endDate = LocalDate.now();

        FinancialEntry income = new FinancialEntry();
        income.setType(FinancialEntryType.INCOME);
        income.setAmount(new BigDecimal("1200.00"));

        FinancialEntry expense = new FinancialEntry();
        expense.setType(FinancialEntryType.EXPENSE);
        expense.setAmount(new BigDecimal("450.00"));

        when(authenticatedCompanyProvider.requireCompanyId()).thenReturn(companyId);
        when(financialEntryRepository.findAllByCompanyIdAndEntryDateBetweenOrderByEntryDateAsc(companyId, startDate, endDate))
                .thenReturn(List.of(income, expense));

        FinancialSummaryResponse result = financialService.summary(startDate, endDate);

        assertEquals(new BigDecimal("1200.00"), result.totalIncome());
        assertEquals(new BigDecimal("450.00"), result.totalExpense());
        assertEquals(new BigDecimal("750.00"), result.balance());
    }
}
