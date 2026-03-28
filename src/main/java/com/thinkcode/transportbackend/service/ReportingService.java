package com.thinkcode.transportbackend.service;

import com.thinkcode.transportbackend.dto.ReportingOverviewResponse;
import com.thinkcode.transportbackend.entity.Debt;
import com.thinkcode.transportbackend.entity.DailyRevenue;
import com.thinkcode.transportbackend.entity.FinancialEntry;
import com.thinkcode.transportbackend.entity.MaintenanceRecord;
import com.thinkcode.transportbackend.repository.DailyRevenueRepository;
import com.thinkcode.transportbackend.repository.DebtRepository;
import com.thinkcode.transportbackend.repository.FinancialEntryRepository;
import com.thinkcode.transportbackend.repository.MaintenanceRecordRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class ReportingService {

    private final DailyRevenueRepository dailyRevenueRepository;
    private final DebtRepository debtRepository;
    private final MaintenanceRecordRepository maintenanceRecordRepository;
    private final FinancialEntryRepository financialEntryRepository;
    private final AuthenticatedCompanyProvider authenticatedCompanyProvider;

    public ReportingService(
            DailyRevenueRepository dailyRevenueRepository,
            DebtRepository debtRepository,
            MaintenanceRecordRepository maintenanceRecordRepository,
            FinancialEntryRepository financialEntryRepository,
            AuthenticatedCompanyProvider authenticatedCompanyProvider
    ) {
        this.dailyRevenueRepository = dailyRevenueRepository;
        this.debtRepository = debtRepository;
        this.maintenanceRecordRepository = maintenanceRecordRepository;
        this.financialEntryRepository = financialEntryRepository;
        this.authenticatedCompanyProvider = authenticatedCompanyProvider;
    }

    public ReportingOverviewResponse overview(LocalDate startDate, LocalDate endDate) {
        validateRange(startDate, endDate);
        UUID companyId = authenticatedCompanyProvider.requireCompanyId();

        List<DailyRevenue> revenues = dailyRevenueRepository.findAllByVehicleCompanyIdAndRevenueDateBetween(companyId, startDate, endDate);
        List<Debt> debts = debtRepository.findAllByVehicleCompanyIdAndDebtDateBetween(companyId, startDate, endDate);
        List<MaintenanceRecord> maintenances = maintenanceRecordRepository.findAllByVehicleCompanyIdAndMaintenanceDateBetween(companyId, startDate, endDate);
        List<FinancialEntry> entries = financialEntryRepository.findAllByCompanyIdAndEntryDateBetweenOrderByEntryDateAsc(companyId, startDate, endDate);

        BigDecimal totalRevenue = revenues.stream().map(DailyRevenue::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalDebt = debts.stream().map(Debt::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalMaintenance = maintenances.stream().map(MaintenanceRecord::getCost).reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalIncome = entries.stream()
                .filter(entry -> entry.getType().name().equals("INCOME"))
                .map(FinancialEntry::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalExpense = entries.stream()
                .filter(entry -> entry.getType().name().equals("EXPENSE"))
                .map(FinancialEntry::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal netResult = totalRevenue.add(totalIncome).subtract(totalExpense).subtract(totalMaintenance);
        return new ReportingOverviewResponse(totalRevenue, totalDebt, totalMaintenance, totalIncome, totalExpense, netResult);
    }

    public String exportCsv(LocalDate startDate, LocalDate endDate) {
        ReportingOverviewResponse overview = overview(startDate, endDate);
        StringBuilder csv = new StringBuilder();
        csv.append("metric,value\n");
        csv.append("totalRevenue,").append(overview.totalRevenue()).append('\n');
        csv.append("totalDebt,").append(overview.totalDebt()).append('\n');
        csv.append("totalMaintenanceCost,").append(overview.totalMaintenanceCost()).append('\n');
        csv.append("totalIncome,").append(overview.totalIncome()).append('\n');
        csv.append("totalExpense,").append(overview.totalExpense()).append('\n');
        csv.append("netResult,").append(overview.netResult()).append('\n');
        return csv.toString();
    }

    private void validateRange(LocalDate startDate, LocalDate endDate) {
        if (endDate.isBefore(startDate)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "endDate must be greater than or equal to startDate");
        }
    }
}
