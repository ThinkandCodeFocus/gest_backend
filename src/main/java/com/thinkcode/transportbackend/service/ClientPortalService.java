package com.thinkcode.transportbackend.service;

import com.thinkcode.transportbackend.dto.ClientPortalMonthlyReportResponse;
import com.thinkcode.transportbackend.dto.ClientPortalOverviewResponse;
import com.thinkcode.transportbackend.dto.ClientPortalVehicleResponse;
import com.thinkcode.transportbackend.entity.Client;
import com.thinkcode.transportbackend.entity.DailyRevenue;
import com.thinkcode.transportbackend.entity.Debt;
import com.thinkcode.transportbackend.entity.Vehicle;
import com.thinkcode.transportbackend.repository.ClientRepository;
import com.thinkcode.transportbackend.repository.DailyRevenueRepository;
import com.thinkcode.transportbackend.repository.DebtRepository;
import com.thinkcode.transportbackend.repository.VehicleRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ClientPortalService {

    private final ClientRepository clientRepository;
    private final VehicleRepository vehicleRepository;
    private final DailyRevenueRepository dailyRevenueRepository;
    private final DebtRepository debtRepository;
    private final AuthenticatedCompanyProvider authenticatedCompanyProvider;
    private final AuthenticatedUserProvider authenticatedUserProvider;

    public ClientPortalService(
            ClientRepository clientRepository,
            VehicleRepository vehicleRepository,
            DailyRevenueRepository dailyRevenueRepository,
            DebtRepository debtRepository,
            AuthenticatedCompanyProvider authenticatedCompanyProvider,
            AuthenticatedUserProvider authenticatedUserProvider
    ) {
        this.clientRepository = clientRepository;
        this.vehicleRepository = vehicleRepository;
        this.dailyRevenueRepository = dailyRevenueRepository;
        this.debtRepository = debtRepository;
        this.authenticatedCompanyProvider = authenticatedCompanyProvider;
        this.authenticatedUserProvider = authenticatedUserProvider;
    }

    /**
     * Get overview: total revenue, debt, active vehicles for a month
     */
    public ClientPortalOverviewResponse getOverview(String month) {
        UUID companyId = authenticatedCompanyProvider.requireCompanyId();
        String email = authenticatedUserProvider.requireUser().getEmail();
        
        Client client = clientRepository.findByCompanyIdAndEmail(companyId, email).orElse(null);
        if (client == null) {
            throw new IllegalArgumentException("Client not found");
        }

        YearMonth yearMonth = YearMonth.parse(month); // YYYY-MM

        // Get client's vehicles
        List<Vehicle> vehicles = vehicleRepository.findByCompanyIdAndClientId(companyId, client.getId());
        List<UUID> vehicleIds = vehicles.stream().map(Vehicle::getId).collect(Collectors.toList());

        // Aggregate revenues for the month
        List<DailyRevenue> revenues = vehicleIds.isEmpty() ? 
            Collections.emptyList() : 
            dailyRevenueRepository.findByCompanyIdAndVehicleIdInAndDateRange(
                companyId,
                vehicleIds,
                yearMonth.atDay(1),
                yearMonth.atEndOfMonth()
            );

        BigDecimal totalRevenue = revenues.stream()
                .map(r -> r.getAmount() != null ? r.getAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Aggregate debts for the month
        List<Debt> debts = vehicleIds.isEmpty() ? 
            Collections.emptyList() : 
            debtRepository.findByCompanyIdAndVehicleIdInAndDateRange(
                companyId,
                vehicleIds,
                yearMonth.atDay(1),
                yearMonth.atEndOfMonth()
            );

        BigDecimal totalDebt = debts.stream()
                .map(d -> d.getAmount() != null ? d.getAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new ClientPortalOverviewResponse(
            totalRevenue,
            totalDebt,
            vehicles.size(),
            revenues.size(),
            month,
            client.getName()
        );
    }

    /**
     * Get list of client's vehicles with KPIs
     */
    public List<ClientPortalVehicleResponse> getVehicles() {
        UUID companyId = authenticatedCompanyProvider.requireCompanyId();
        String email = authenticatedUserProvider.requireUser().getEmail();
        
        Client client = clientRepository.findByCompanyIdAndEmail(companyId, email).orElse(null);
        if (client == null) {
            throw new IllegalArgumentException("Client not found");
        }

        List<Vehicle> vehicles = vehicleRepository.findByCompanyIdAndClientId(companyId, client.getId());

        return vehicles.stream()
                .map(v -> new ClientPortalVehicleResponse(
                    v.getId(),
                    v.getMatricule(),
                    v.getMatricule(), // Same as matricule (immatriculation not separate field)
                    v.getType() != null ? v.getType().toString() : "UNKNOWN",
                    v.getStatus() != null ? v.getStatus().toString() : "UNKNOWN",
                    BigDecimal.ZERO, // Would need aggregation for current month
                    BigDecimal.ZERO,
                    0,
                    v.getDriver() != null ? v.getDriver().getFullName() : "UNASSIGNED"
                ))
                .collect(Collectors.toList());
    }

    /**
     * Get monthly report for a specific vehicle
     */
    public ClientPortalMonthlyReportResponse getMonthlyReport(UUID vehicleId, String month) {
        UUID companyId = authenticatedCompanyProvider.requireCompanyId();
        String email = authenticatedUserProvider.requireUser().getEmail();
        
        Client client = clientRepository.findByCompanyIdAndEmail(companyId, email).orElse(null);
        if (client == null) {
            throw new IllegalArgumentException("Client not found");
        }

        Vehicle vehicle = vehicleRepository.findById(vehicleId).orElse(null);
        if (vehicle == null || !vehicle.getClient().getId().equals(client.getId())) {
            throw new IllegalArgumentException("Vehicle not found or unauthorized");
        }

        YearMonth yearMonth = YearMonth.parse(month);

        // Get revenues for this vehicle/month
        List<DailyRevenue> revenues = dailyRevenueRepository.findByCompanyIdAndVehicleIdAndDateRange(
            companyId,
            vehicleId,
            yearMonth.atDay(1),
            yearMonth.atEndOfMonth()
        );

        // Get debts for this vehicle/month
        List<Debt> debts = debtRepository.findByCompanyIdAndVehicleIdAndDateRange(
            companyId,
            vehicleId,
            yearMonth.atDay(1),
            yearMonth.atEndOfMonth()
        );

        // Aggregate by day
        Map<LocalDate, BigDecimal> dailyRevenues = new TreeMap<>();
        Map<LocalDate, BigDecimal> dailyDebts = new TreeMap<>();

        revenues.forEach(r -> dailyRevenues.put(r.getRevenueDate(), r.getAmount()));
        debts.forEach(d -> dailyDebts.put(d.getDebtDate(), d.getAmount()));

        BigDecimal totalRevenue = revenues.stream()
                .map(r -> r.getAmount() != null ? r.getAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalDebt = debts.stream()
                .map(d -> d.getAmount() != null ? d.getAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Summary by day
        Set<LocalDate> allDates = new HashSet<>();
        allDates.addAll(dailyRevenues.keySet());
        allDates.addAll(dailyDebts.keySet());

        List<ClientPortalMonthlyReportResponse.DailyTripSummary> days = allDates.stream()
                .sorted()
                .map(date -> new ClientPortalMonthlyReportResponse.DailyTripSummary(
                    date,
                    1, // Would need trip count from DailyRevenue
                    dailyRevenues.getOrDefault(date, BigDecimal.ZERO),
                    dailyDebts.getOrDefault(date, BigDecimal.ZERO)
                ))
                .collect(Collectors.toList());

        return new ClientPortalMonthlyReportResponse(
            vehicleId,
            vehicle.getMatricule(),
            month,
            revenues.size(),
            totalRevenue,
            totalRevenue.multiply(BigDecimal.valueOf(0.3)), // 30% company cut
            totalRevenue.multiply(BigDecimal.valueOf(0.7)), // 70% driver earnings
            totalDebt,
            days
        );
    }
}
