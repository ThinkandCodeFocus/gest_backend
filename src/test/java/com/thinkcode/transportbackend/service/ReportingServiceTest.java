package com.thinkcode.transportbackend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import com.thinkcode.transportbackend.dto.ReportingOverviewResponse;
import com.thinkcode.transportbackend.entity.DailyRevenue;
import com.thinkcode.transportbackend.entity.Debt;
import com.thinkcode.transportbackend.entity.FinancialEntry;
import com.thinkcode.transportbackend.entity.FinancialEntryType;
import com.thinkcode.transportbackend.entity.MaintenanceRecord;
import com.thinkcode.transportbackend.repository.DailyRevenueRepository;
import com.thinkcode.transportbackend.repository.DebtRepository;
import com.thinkcode.transportbackend.repository.FinancialEntryRepository;
import com.thinkcode.transportbackend.repository.MaintenanceRecordRepository;
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
class ReportingServiceTest {

    @Mock
    private DailyRevenueRepository dailyRevenueRepository;

    @Mock
    private DebtRepository debtRepository;

    @Mock
    private MaintenanceRecordRepository maintenanceRecordRepository;

    @Mock
    private FinancialEntryRepository financialEntryRepository;

    @Mock
    private AuthenticatedCompanyProvider authenticatedCompanyProvider;

    private ReportingService reportingService;

    @BeforeEach
    void setUp() {
        reportingService = new ReportingService(
                dailyRevenueRepository,
                debtRepository,
                maintenanceRecordRepository,
                financialEntryRepository,
                authenticatedCompanyProvider
        );
    }

    @Test
    void overviewShouldAggregateAllFinancialIndicators() {
        UUID companyId = UUID.randomUUID();
        LocalDate startDate = LocalDate.now().minusDays(7);
        LocalDate endDate = LocalDate.now();

        DailyRevenue revenue = new DailyRevenue();
        revenue.setAmount(new BigDecimal("1000.00"));

        Debt debt = new Debt();
        debt.setAmount(new BigDecimal("120.00"));

        MaintenanceRecord maintenance = new MaintenanceRecord();
        maintenance.setCost(new BigDecimal("100.00"));

        FinancialEntry income = new FinancialEntry();
        income.setType(FinancialEntryType.INCOME);
        income.setAmount(new BigDecimal("300.00"));

        FinancialEntry expense = new FinancialEntry();
        expense.setType(FinancialEntryType.EXPENSE);
        expense.setAmount(new BigDecimal("50.00"));

        when(authenticatedCompanyProvider.requireCompanyId()).thenReturn(companyId);
        when(dailyRevenueRepository.findAllByVehicleCompanyIdAndRevenueDateBetween(companyId, startDate, endDate))
                .thenReturn(List.of(revenue));
        when(debtRepository.findAllByVehicleCompanyIdAndDebtDateBetween(companyId, startDate, endDate))
                .thenReturn(List.of(debt));
        when(maintenanceRecordRepository.findAllByVehicleCompanyIdAndMaintenanceDateBetween(companyId, startDate, endDate))
                .thenReturn(List.of(maintenance));
        when(financialEntryRepository.findAllByCompanyIdAndEntryDateBetweenOrderByEntryDateAsc(companyId, startDate, endDate))
                .thenReturn(List.of(income, expense));

        ReportingOverviewResponse result = reportingService.overview(startDate, endDate);

        assertEquals(new BigDecimal("1000.00"), result.totalRevenue());
        assertEquals(new BigDecimal("120.00"), result.totalDebt());
        assertEquals(new BigDecimal("100.00"), result.totalMaintenanceCost());
        assertEquals(new BigDecimal("300.00"), result.totalIncome());
        assertEquals(new BigDecimal("50.00"), result.totalExpense());
        assertEquals(new BigDecimal("1150.00"), result.netResult());
    }
}
