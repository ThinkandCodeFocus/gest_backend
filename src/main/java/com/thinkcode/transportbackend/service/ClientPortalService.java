package com.thinkcode.transportbackend.service;

import com.thinkcode.transportbackend.dto.ClientPortalMonthlyReportResponse;
import com.thinkcode.transportbackend.dto.ClientPortalOverviewResponse;
import com.thinkcode.transportbackend.dto.ClientPortalVehicleResponse;
import com.thinkcode.transportbackend.entity.Client;
import com.thinkcode.transportbackend.entity.DailyRevenue;
import com.thinkcode.transportbackend.entity.Debt;
import com.thinkcode.transportbackend.entity.MaintenanceRecord;
import com.thinkcode.transportbackend.entity.MaintenanceType;
import com.thinkcode.transportbackend.entity.Vehicle;
import com.thinkcode.transportbackend.repository.ClientRepository;
import com.thinkcode.transportbackend.repository.DailyRevenueRepository;
import com.thinkcode.transportbackend.repository.DebtRepository;
import com.thinkcode.transportbackend.repository.MaintenanceRecordRepository;
import com.thinkcode.transportbackend.repository.VehicleRepository;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class ClientPortalService {

    private static final Locale FRENCH = Locale.forLanguageTag("fr-FR");

    private final ClientRepository clientRepository;
    private final VehicleRepository vehicleRepository;
    private final DailyRevenueRepository dailyRevenueRepository;
    private final DebtRepository debtRepository;
    private final MaintenanceRecordRepository maintenanceRecordRepository;
    private final AuthenticatedCompanyProvider authenticatedCompanyProvider;
    private final AuthenticatedUserProvider authenticatedUserProvider;

    public ClientPortalService(
            ClientRepository clientRepository,
            VehicleRepository vehicleRepository,
            DailyRevenueRepository dailyRevenueRepository,
            DebtRepository debtRepository,
            MaintenanceRecordRepository maintenanceRecordRepository,
            AuthenticatedCompanyProvider authenticatedCompanyProvider,
            AuthenticatedUserProvider authenticatedUserProvider
    ) {
        this.clientRepository = clientRepository;
        this.vehicleRepository = vehicleRepository;
        this.dailyRevenueRepository = dailyRevenueRepository;
        this.debtRepository = debtRepository;
        this.maintenanceRecordRepository = maintenanceRecordRepository;
        this.authenticatedCompanyProvider = authenticatedCompanyProvider;
        this.authenticatedUserProvider = authenticatedUserProvider;
    }

    public ClientPortalOverviewResponse getOverview(String month) {
        UUID companyId = authenticatedCompanyProvider.requireCompanyId();
        Client client = requireAuthenticatedClient(companyId);
        YearMonth yearMonth = YearMonth.parse(month);

        List<Vehicle> vehicles = vehicleRepository.findByCompanyIdAndClientId(companyId, client.getId());
        List<UUID> vehicleIds = vehicles.stream().map(Vehicle::getId).toList();

        List<DailyRevenue> revenues = vehicleIds.isEmpty()
                ? Collections.emptyList()
                : dailyRevenueRepository.findByCompanyIdAndVehicleIdInAndDateRange(
                        companyId,
                        vehicleIds,
                        yearMonth.atDay(1),
                        yearMonth.atEndOfMonth()
                );

        List<Debt> debts = vehicleIds.isEmpty()
                ? Collections.emptyList()
                : debtRepository.findByCompanyIdAndVehicleIdInAndDateRange(
                        companyId,
                        vehicleIds,
                        yearMonth.atDay(1),
                        yearMonth.atEndOfMonth()
                );

        BigDecimal totalRevenue = sumRevenues(revenues);
        BigDecimal totalDebt = sumDebts(debts);
        int activeVehicles = (int) vehicles.stream()
                .filter(vehicle -> vehicle.getStatus() != null && "AVAILABLE".equals(vehicle.getStatus().name()))
                .count();

        return new ClientPortalOverviewResponse(
                totalRevenue,
                totalDebt,
                activeVehicles,
                revenues.size(),
                month,
                client.getName()
        );
    }

    public List<ClientPortalVehicleResponse> getVehicles() {
        UUID companyId = authenticatedCompanyProvider.requireCompanyId();
        Client client = requireAuthenticatedClient(companyId);
        YearMonth currentMonth = YearMonth.now();

        return vehicleRepository.findByCompanyIdAndClientId(companyId, client.getId()).stream()
                .map(vehicle -> mapVehicle(companyId, vehicle, currentMonth))
                .toList();
    }

    public ClientPortalMonthlyReportResponse getMonthlyReport(UUID vehicleId, String month) {
        UUID companyId = authenticatedCompanyProvider.requireCompanyId();
        Client client = requireAuthenticatedClient(companyId);

        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Vehicle not found"));
        if (vehicle.getClient() == null || !vehicle.getClient().getId().equals(client.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Vehicle not found or unauthorized");
        }

        YearMonth yearMonth = YearMonth.parse(month);
        LocalDate start = yearMonth.atDay(1);
        LocalDate end = yearMonth.atEndOfMonth();

        List<DailyRevenue> revenues = dailyRevenueRepository.findByCompanyIdAndVehicleIdAndDateRange(companyId, vehicleId, start, end);
        List<Debt> debts = debtRepository.findByCompanyIdAndVehicleIdAndDateRange(companyId, vehicleId, start, end);
        List<MaintenanceRecord> maintenances = maintenanceRecordRepository.findByCompanyVehicleAndDateRange(companyId, vehicleId, start, end);

        Map<LocalDate, DailyRevenue> revenueByDate = revenues.stream()
                .collect(Collectors.toMap(DailyRevenue::getRevenueDate, revenue -> revenue, (left, right) -> right, TreeMap::new));
        Map<LocalDate, List<MaintenanceRecord>> maintenancesByDate = maintenances.stream()
                .collect(Collectors.groupingBy(MaintenanceRecord::getMaintenanceDate, TreeMap::new, Collectors.toList()));

        List<ClientPortalMonthlyReportResponse.DailyVehicleRow> days = new ArrayList<>();
        for (int day = 1; day <= yearMonth.lengthOfMonth(); day++) {
            LocalDate date = yearMonth.atDay(day);
            DailyRevenue revenue = revenueByDate.get(date);
            List<MaintenanceRecord> dayMaintenances = maintenancesByDate.getOrDefault(date, List.of());

            BigDecimal recetteCaisse = revenue == null ? BigDecimal.ZERO : nullSafe(revenue.getAmount());
            BigDecimal recetteChauffeur = revenue == null ? BigDecimal.ZERO : nullSafe(revenue.getDriverShare());
            BigDecimal recetteService = revenue == null ? BigDecimal.ZERO : nullSafe(revenue.getCompanyShare());
            BigDecimal recetteClient = revenue == null
                    ? BigDecimal.ZERO
                    : nullSafe(revenue.getClientShare()).max(recetteCaisse.subtract(recetteChauffeur).subtract(recetteService).max(BigDecimal.ZERO));

            BigDecimal amendes = sumMaintenance(dayMaintenances, MaintenanceType.FINE);
            BigDecimal vidange = sumMaintenance(dayMaintenances, MaintenanceType.OIL_CHANGE);
            BigDecimal pannes = dayMaintenances.stream()
                    .filter(item -> item.getType() == MaintenanceType.BREAKDOWN || item.getType() == MaintenanceType.REPAIR)
                    .map(MaintenanceRecord::getCost)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal autres = sumMaintenance(dayMaintenances, MaintenanceType.OTHER);

            String commentaires = dayMaintenances.stream()
                    .map(MaintenanceRecord::getDescription)
                    .filter(value -> value != null && !value.isBlank())
                    .collect(Collectors.joining(" | "));
            String origineFonds = dayMaintenances.stream()
                    .map(MaintenanceRecord::getProvider)
                    .filter(value -> value != null && !value.isBlank())
                    .distinct()
                    .collect(Collectors.joining(", "));

            days.add(new ClientPortalMonthlyReportResponse.DailyVehicleRow(
                    date.toString(),
                    formatFrenchDayLabel(date),
                    recetteCaisse,
                    recetteChauffeur,
                    recetteService,
                    recetteClient,
                    amendes,
                    vidange,
                    pannes,
                    autres,
                    commentaires,
                    origineFonds
            ));
        }

        BigDecimal recetteCaisse = sumRows(days, ClientPortalMonthlyReportResponse.DailyVehicleRow::recetteCaisse);
        BigDecimal recetteChauffeur = sumRows(days, ClientPortalMonthlyReportResponse.DailyVehicleRow::recetteChauffeur);
        BigDecimal recetteService = sumRows(days, ClientPortalMonthlyReportResponse.DailyVehicleRow::recetteService);
        BigDecimal recetteClient = sumRows(days, ClientPortalMonthlyReportResponse.DailyVehicleRow::recetteClient);
        BigDecimal amendes = sumRows(days, ClientPortalMonthlyReportResponse.DailyVehicleRow::amendes);
        BigDecimal vidange = sumRows(days, ClientPortalMonthlyReportResponse.DailyVehicleRow::vidange);
        BigDecimal pannes = sumRows(days, ClientPortalMonthlyReportResponse.DailyVehicleRow::pannes);
        BigDecimal autres = sumRows(days, ClientPortalMonthlyReportResponse.DailyVehicleRow::autres);
        BigDecimal totalDepenses = amendes.add(vidange).add(pannes).add(autres);

        YearMonth previousMonth = yearMonth.minusMonths(1);
        BigDecimal recetteMoisPrecedent = sumRevenues(
                dailyRevenueRepository.findByCompanyIdAndVehicleIdAndDateRange(
                        companyId,
                        vehicleId,
                        previousMonth.atDay(1),
                        previousMonth.atEndOfMonth()
                )
        );

        BigDecimal detteChauffeur = sumDebts(debts);
        int joursAvecActivite = (int) days.stream()
                .filter(row -> row.recetteCaisse().compareTo(BigDecimal.ZERO) > 0
                        || row.amendes().compareTo(BigDecimal.ZERO) > 0
                        || row.vidange().compareTo(BigDecimal.ZERO) > 0
                        || row.pannes().compareTo(BigDecimal.ZERO) > 0
                        || row.autres().compareTo(BigDecimal.ZERO) > 0)
                .count();

        ClientPortalMonthlyReportResponse.MonthlySummary summary = new ClientPortalMonthlyReportResponse.MonthlySummary(
                days.size(),
                joursAvecActivite,
                recetteCaisse,
                recetteChauffeur,
                recetteService,
                recetteClient,
                amendes,
                vidange,
                pannes,
                autres,
                totalDepenses,
                recetteMoisPrecedent,
                recetteService.subtract(totalDepenses),
                detteChauffeur
        );

        return new ClientPortalMonthlyReportResponse(
                vehicle.getId(),
                vehicle.getMatricule(),
                month,
                client.getName(),
                vehicle.getType() == null ? null : vehicle.getType().name(),
                vehicle.getDriver() == null ? null : vehicle.getDriver().getFullName(),
                client.getEmail(),
                client.getPhoneNumber(),
                nullSafe(vehicle.getAmortization()),
                summary,
                days
        );
    }

    private ClientPortalVehicleResponse mapVehicle(UUID companyId, Vehicle vehicle, YearMonth month) {
        List<DailyRevenue> revenues = dailyRevenueRepository.findByCompanyIdAndVehicleIdAndDateRange(
                companyId,
                vehicle.getId(),
                month.atDay(1),
                month.atEndOfMonth()
        );
        List<Debt> debts = debtRepository.findByCompanyIdAndVehicleIdAndDateRange(
                companyId,
                vehicle.getId(),
                month.atDay(1),
                month.atEndOfMonth()
        );

        return new ClientPortalVehicleResponse(
                vehicle.getId(),
                vehicle.getMatricule(),
                vehicle.getMatricule(),
                vehicle.getType() == null ? "UNKNOWN" : vehicle.getType().name(),
                vehicle.getStatus() == null ? "UNKNOWN" : vehicle.getStatus().name(),
                sumRevenues(revenues),
                sumDebts(debts),
                revenues.size(),
                vehicle.getDriver() == null ? "UNASSIGNED" : vehicle.getDriver().getFullName()
        );
    }

    private BigDecimal sumRows(
            List<ClientPortalMonthlyReportResponse.DailyVehicleRow> rows,
            java.util.function.Function<ClientPortalMonthlyReportResponse.DailyVehicleRow, BigDecimal> extractor
    ) {
        return rows.stream()
                .map(extractor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal sumMaintenance(List<MaintenanceRecord> records, MaintenanceType type) {
        return records.stream()
                .filter(item -> item.getType() == type)
                .map(MaintenanceRecord::getCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal sumRevenues(List<DailyRevenue> revenues) {
        return revenues.stream()
                .map(DailyRevenue::getAmount)
                .map(this::nullSafe)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal sumDebts(List<Debt> debts) {
        return debts.stream()
                .map(Debt::getAmount)
                .map(this::nullSafe)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal nullSafe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String formatFrenchDayLabel(LocalDate date) {
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        String day = dayOfWeek.getDisplayName(TextStyle.FULL, FRENCH);
        String month = date.getMonth().getDisplayName(TextStyle.FULL, FRENCH);
        return day + " " + date.getDayOfMonth() + " " + month;
    }

    private Client requireAuthenticatedClient(UUID companyId) {
        String email = authenticatedUserProvider.requireUser().getEmail();
        return clientRepository.findByCompanyIdAndEmail(companyId, email)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Client not found"));
    }
}
