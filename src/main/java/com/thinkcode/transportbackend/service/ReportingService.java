package com.thinkcode.transportbackend.service;

import com.lowagie.text.Document;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.thinkcode.transportbackend.dto.ReportingOverviewResponse;
import com.thinkcode.transportbackend.entity.Debt;
import com.thinkcode.transportbackend.entity.DailyRevenue;
import com.thinkcode.transportbackend.entity.FinancialEntry;
import com.thinkcode.transportbackend.entity.MaintenanceRecord;
import com.thinkcode.transportbackend.entity.Vehicle;
import com.thinkcode.transportbackend.repository.DailyRevenueRepository;
import com.thinkcode.transportbackend.repository.DebtRepository;
import com.thinkcode.transportbackend.repository.DriverIncidentRepository;
import com.thinkcode.transportbackend.repository.DriverRepository;
import com.thinkcode.transportbackend.repository.FinancialEntryRepository;
import com.thinkcode.transportbackend.repository.MaintenanceRecordRepository;
import com.thinkcode.transportbackend.repository.VehicleRepository;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class ReportingService {

    private final DailyRevenueRepository dailyRevenueRepository;
    private final DebtRepository debtRepository;
    private final MaintenanceRecordRepository maintenanceRecordRepository;
    private final FinancialEntryRepository financialEntryRepository;
    private final AuthenticatedCompanyProvider authenticatedCompanyProvider;
    private final VehicleRepository vehicleRepository;
    private final DriverRepository driverRepository;
    private final DriverIncidentRepository driverIncidentRepository;
    private final DebtService debtService;

    public ReportingService(
            DailyRevenueRepository dailyRevenueRepository,
            DebtRepository debtRepository,
            MaintenanceRecordRepository maintenanceRecordRepository,
            FinancialEntryRepository financialEntryRepository,
            AuthenticatedCompanyProvider authenticatedCompanyProvider,
            VehicleRepository vehicleRepository,
            DriverRepository driverRepository,
            DriverIncidentRepository driverIncidentRepository,
            DebtService debtService
    ) {
        this.dailyRevenueRepository = dailyRevenueRepository;
        this.debtRepository = debtRepository;
        this.maintenanceRecordRepository = maintenanceRecordRepository;
        this.financialEntryRepository = financialEntryRepository;
        this.authenticatedCompanyProvider = authenticatedCompanyProvider;
        this.vehicleRepository = vehicleRepository;
        this.driverRepository = driverRepository;
        this.driverIncidentRepository = driverIncidentRepository;
        this.debtService = debtService;
    }

    public ReportingOverviewResponse overview(LocalDate startDate, LocalDate endDate) {
        validateRange(startDate, endDate);
        UUID companyId = authenticatedCompanyProvider.requireCompanyId();

        List<DailyRevenue> revenues = dailyRevenueRepository.findAllByVehicleCompanyIdAndRevenueDateBetween(companyId, startDate, endDate)
                .stream()
                .filter(r -> r.getRevenueDate().getDayOfWeek() != DayOfWeek.SUNDAY)
                .collect(Collectors.toList());
        List<Debt> debts = debtRepository.findAllByVehicleCompanyIdAndDebtDateBetween(companyId, startDate, endDate)
                .stream()
                .filter(d -> d.getDebtDate().getDayOfWeek() != DayOfWeek.SUNDAY)
                .collect(Collectors.toList());
        List<MaintenanceRecord> maintenances = maintenanceRecordRepository.findAllByVehicleCompanyIdAndMaintenanceDateBetween(companyId, startDate, endDate)
                .stream()
                .filter(m -> m.getMaintenanceDate().getDayOfWeek() != DayOfWeek.SUNDAY)
                .collect(Collectors.toList());
        List<FinancialEntry> entries = financialEntryRepository.findAllByCompanyIdAndEntryDateBetweenOrderByEntryDateAsc(companyId, startDate, endDate)
                .stream()
                .filter(e -> e.getEntryDate().getDayOfWeek() != DayOfWeek.SUNDAY)
                .collect(Collectors.toList());

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

        List<ReportingOverviewResponse.VehicleRanking> vehicleRankings = vehicleRepository.findAllByCompanyId(companyId).stream()
                .map(vehicle -> toVehicleRanking(companyId, vehicle, startDate, endDate))
                .sorted((left, right) -> right.margin().compareTo(left.margin()))
                .toList();

        YearMonth currentMonth = YearMonth.now();
        List<ReportingOverviewResponse.DriverRanking> driverRankings = driverRepository.findAllByCompanyId(companyId).stream()
                .map(driver -> new ReportingOverviewResponse.DriverRanking(
                        driver.getFullName(),
                        driver.getPerformanceScore(),
                        (int) driverIncidentRepository.countByCompanyIdAndDriverIdAndCreatedAtAfter(
                                companyId,
                                driver.getId(),
                                currentMonth.atDay(1).atStartOfDay(java.time.ZoneOffset.UTC).toInstant()
                        ),
                        debtService.openDebtTotalForDriver(driver.getId())
                ))
                .sorted((left, right) -> Integer.compare(right.performanceScore(), left.performanceScore()))
                .toList();

        return new ReportingOverviewResponse(
                totalRevenue,
                totalDebt,
                totalMaintenance,
                totalIncome,
                totalExpense,
                netResult,
                vehicleRankings,
                driverRankings
        );
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
        csv.append("\nvehicle,client,revenue,debt,margin,status\n");
        overview.vehicleRankings().forEach(item -> csv.append(item.matricule()).append(',')
                .append(item.client()).append(',')
                .append(item.revenue()).append(',')
                .append(item.debt()).append(',')
                .append(item.margin()).append(',')
                .append(item.status()).append('\n'));
        return csv.toString();
    }

    public byte[] exportExcel(LocalDate startDate, LocalDate endDate) {
        String html = "<table><tr><th>Metric</th><th>Value</th></tr>"
                + "<tr><td>Total revenue</td><td>" + overview(startDate, endDate).totalRevenue() + "</td></tr>"
                + "<tr><td>Total debt</td><td>" + overview(startDate, endDate).totalDebt() + "</td></tr>"
                + "<tr><td>Total maintenance</td><td>" + overview(startDate, endDate).totalMaintenanceCost() + "</td></tr>"
                + "</table>";
        return html.getBytes();
    }

    public byte[] exportPdf(LocalDate startDate, LocalDate endDate) {
        ReportingOverviewResponse overview = overview(startDate, endDate);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        Document document = new Document();
        try {
            PdfWriter.getInstance(document, output);
            document.open();
            document.add(new Paragraph("Reporting direction", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16)));
            document.add(new Paragraph("Periode: " + startDate + " au " + endDate));
            document.add(new Paragraph("Total recettes: " + overview.totalRevenue()));
            document.add(new Paragraph("Total dettes: " + overview.totalDebt()));
            document.add(new Paragraph("Resultat net: " + overview.netResult()));
            document.add(new Paragraph(" "));
            PdfPTable table = new PdfPTable(5);
            table.addCell("Matricule");
            table.addCell("Client");
            table.addCell("Recette");
            table.addCell("Dette");
            table.addCell("Marge");
            for (ReportingOverviewResponse.VehicleRanking item : overview.vehicleRankings()) {
                table.addCell(item.matricule());
                table.addCell(item.client() == null ? "" : item.client());
                table.addCell(item.revenue().toPlainString());
                table.addCell(item.debt().toPlainString());
                table.addCell(item.margin().toPlainString());
            }
            document.add(table);
        } catch (Exception ex) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to generate reporting PDF");
        } finally {
            document.close();
        }
        return output.toByteArray();
    }

    private ReportingOverviewResponse.VehicleRanking toVehicleRanking(UUID companyId, Vehicle vehicle, LocalDate startDate, LocalDate endDate) {
        BigDecimal revenue = dailyRevenueRepository.findByCompanyIdAndVehicleIdAndDateRange(companyId, vehicle.getId(), startDate, endDate).stream()
                .map(DailyRevenue::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal debt = debtRepository.findByCompanyIdAndVehicleIdAndDateRange(companyId, vehicle.getId(), startDate, endDate).stream()
                .map(Debt::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal maintenance = maintenanceRecordRepository.findByCompanyVehicleAndDateRange(companyId, vehicle.getId(), startDate, endDate).stream()
                .map(MaintenanceRecord::getCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new ReportingOverviewResponse.VehicleRanking(
                vehicle.getMatricule(),
                vehicle.getClient() == null ? null : vehicle.getClient().getName(),
                revenue,
                debt,
                revenue.subtract(debt).subtract(maintenance),
                vehicle.getStatus() == null ? null : vehicle.getStatus().name()
        );
    }

    private void validateRange(LocalDate startDate, LocalDate endDate) {
        if (endDate.isBefore(startDate)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "endDate must be greater than or equal to startDate");
        }
    }
}
