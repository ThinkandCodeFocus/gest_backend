package com.thinkcode.transportbackend.service;

import com.thinkcode.transportbackend.dto.DashboardAssistantResponse;
import com.thinkcode.transportbackend.dto.DashboardDirectionResponse;
import com.thinkcode.transportbackend.dto.DashboardOperationsResponse;
import com.thinkcode.transportbackend.dto.DashboardResponse;
import com.thinkcode.transportbackend.entity.Debt;
import com.thinkcode.transportbackend.entity.DebtStatus;
import com.thinkcode.transportbackend.entity.VehicleStatus;
import com.thinkcode.transportbackend.repository.DailyRevenueRepository;
import com.thinkcode.transportbackend.repository.DebtRepository;
import com.thinkcode.transportbackend.repository.DriverAbsenceRepository;
import com.thinkcode.transportbackend.repository.DriverRepository;
import com.thinkcode.transportbackend.repository.MaintenanceRecordRepository;
import com.thinkcode.transportbackend.repository.MessageRepository;
import com.thinkcode.transportbackend.repository.NotificationRepository;
import com.thinkcode.transportbackend.repository.PlanningEventRepository;
import com.thinkcode.transportbackend.repository.VehicleRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class DashboardService {

    private final VehicleRepository vehicleRepository;
    private final DailyRevenueRepository dailyRevenueRepository;
    private final DebtRepository debtRepository;
    private final MaintenanceRecordRepository maintenanceRecordRepository;
    private final DriverRepository driverRepository;
    private final NotificationRepository notificationRepository;
    private final MessageRepository messageRepository;
    private final PlanningEventRepository planningEventRepository;
    private final DriverAbsenceRepository driverAbsenceRepository;
    private final AuthenticatedUserProvider authenticatedUserProvider;

    public DashboardService(
            VehicleRepository vehicleRepository,
            DailyRevenueRepository dailyRevenueRepository,
            DebtRepository debtRepository,
            MaintenanceRecordRepository maintenanceRecordRepository,
            DriverRepository driverRepository,
            NotificationRepository notificationRepository,
            MessageRepository messageRepository,
            PlanningEventRepository planningEventRepository,
            DriverAbsenceRepository driverAbsenceRepository,
            AuthenticatedUserProvider authenticatedUserProvider
    ) {
        this.vehicleRepository = vehicleRepository;
        this.dailyRevenueRepository = dailyRevenueRepository;
        this.debtRepository = debtRepository;
        this.maintenanceRecordRepository = maintenanceRecordRepository;
        this.driverRepository = driverRepository;
        this.notificationRepository = notificationRepository;
        this.messageRepository = messageRepository;
        this.planningEventRepository = planningEventRepository;
        this.driverAbsenceRepository = driverAbsenceRepository;
        this.authenticatedUserProvider = authenticatedUserProvider;
    }

    public DashboardResponse getMetrics(UUID companyId) {
        long totalVehicles = vehicleRepository.findAllByCompanyId(companyId).size();
        long activeVehicles = vehicleRepository.findAllByCompanyId(companyId).stream()
                .filter(vehicle -> vehicle.getStatus() == VehicleStatus.ASSIGNED || vehicle.getStatus() == VehicleStatus.AVAILABLE)
                .count();
        BigDecimal totalRevenue = dailyRevenueRepository.findAllByVehicleCompanyId(companyId).stream()
                .map(revenue -> revenue.getAmount() == null ? BigDecimal.ZERO : revenue.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal openDebt = debtRepository.findAllByVehicleCompanyId(companyId).stream()
                .filter(debt -> debt.getStatus() == DebtStatus.OPEN || debt.getStatus() == DebtStatus.PARTIALLY_PAID)
                .map(Debt::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal maintenanceCost = maintenanceRecordRepository.findAllByVehicleCompanyId(companyId).stream()
                .map(record -> record.getCost() == null ? BigDecimal.ZERO : record.getCost())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new DashboardResponse(totalVehicles, activeVehicles, totalRevenue, openDebt, maintenanceCost);
    }

    public DashboardDirectionResponse getDirectionDashboard(UUID companyId) {
        BigDecimal totalRevenue = dailyRevenueRepository.findAllByVehicleCompanyId(companyId).stream()
                .map(item -> item.getAmount() == null ? BigDecimal.ZERO : item.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalDebt = debtRepository.findAllByVehicleCompanyId(companyId).stream()
                .filter(item -> item.getStatus() == DebtStatus.OPEN || item.getStatus() == DebtStatus.PARTIALLY_PAID)
                .map(Debt::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalMaintenance = maintenanceRecordRepository.findAllByVehicleCompanyId(companyId).stream()
                .map(item -> item.getCost() == null ? BigDecimal.ZERO : item.getCost())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalProfit = totalRevenue.subtract(totalMaintenance).subtract(totalDebt);
        BigDecimal roi = totalMaintenance.compareTo(BigDecimal.ZERO) == 0
                ? totalProfit
                : totalProfit.divide(totalMaintenance, 2, java.math.RoundingMode.HALF_UP);

        Map<String, BigDecimal> revenueByVehicle = vehicleRepository.findAllByCompanyId(companyId).stream()
                .collect(java.util.stream.Collectors.toMap(
                        vehicle -> vehicle.getMatricule(),
                        vehicle -> dailyRevenueRepository.findByCompanyIdAndVehicleIdAndDateRange(
                                companyId,
                                vehicle.getId(),
                                LocalDate.now().withDayOfMonth(1),
                                LocalDate.now()
                        ).stream().map(revenue -> revenue.getAmount() == null ? BigDecimal.ZERO : revenue.getAmount()).reduce(BigDecimal.ZERO, BigDecimal::add)
                ));

        Map<String, Integer> alertsCount = Map.of(
                "critical", maintenanceRecordRepository.findAllByVehicleCompanyId(companyId).stream().mapToInt(item -> item.isSuspectedDuplicate() ? 1 : 0).sum(),
                "warning", debtRepository.findAllByVehicleCompanyId(companyId).stream().mapToInt(item -> item.getStatus() == DebtStatus.OPEN ? 1 : 0).sum(),
                "info", (int) notificationRepository.countByCompanyIdAndRead(companyId, false)
        );

        return new DashboardDirectionResponse(
                totalProfit,
                totalMaintenance,
                roi,
                (int) vehicleRepository.findAllByCompanyId(companyId).stream().filter(vehicle -> vehicle.getStatus() == VehicleStatus.AVAILABLE || vehicle.getStatus() == VehicleStatus.ASSIGNED).count(),
                driverRepository.findAllByCompanyId(companyId).size(),
                revenueByVehicle,
                alertsCount
        );
    }

    public DashboardOperationsResponse getOperationsDashboard(UUID companyId) {
        YearMonth month = YearMonth.now();
        long unreadNotifications = notificationRepository.countByCompanyIdAndRead(companyId, false);
        List<String> criticalAlerts = debtRepository.findAllByVehicleCompanyId(companyId).stream()
                .filter(item -> item.getStatus() == DebtStatus.OPEN)
                .limit(3)
                .map(item -> "Dette en cours sur " + item.getVehicle().getMatricule())
                .toList();
        long absences = driverAbsenceRepository.findAllByCompanyIdAndStartDateBetweenOrderByStartDateAsc(
                companyId,
                month.atDay(1),
                month.atEndOfMonth()
        ).size();

        return new DashboardOperationsResponse(
                planningEventRepository.findByCompanyAndDateRange(companyId, LocalDate.now(), LocalDate.now().plusDays(1)).size(),
                maintenanceRecordRepository.findAllByVehicleCompanyId(companyId).stream().mapToInt(item -> item.isSuspectedDuplicate() ? 1 : 0).sum(),
                (int) debtRepository.findAllByVehicleCompanyId(companyId).stream().filter(item -> item.getStatus() == DebtStatus.OPEN).count(),
                debtRepository.findAllByVehicleCompanyId(companyId).stream()
                        .filter(item -> item.getStatus() == DebtStatus.OPEN || item.getStatus() == DebtStatus.PARTIALLY_PAID)
                        .map(Debt::getAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add),
                absences > 0 ? concatAlerts(criticalAlerts, "Absences du mois: " + absences) : criticalAlerts,
                unreadNotifications
        );
    }

    public DashboardAssistantResponse getAssistantDashboard(UUID companyId) {
        UUID userId = authenticatedUserProvider.requireUser().getId();
        List<String> upcomingSchedules = planningEventRepository.findByCompanyAndDateRange(
                companyId,
                LocalDate.now(),
                LocalDate.now().plusDays(7)
        ).stream().limit(5).map(item -> item.getEventDate() + " - " + item.getTitle()).toList();

        return new DashboardAssistantResponse(
                planningEventRepository.findByCompanyAndDateRange(companyId, LocalDate.now(), LocalDate.now().plusDays(7)).size(),
                dailyRevenueRepository.findAllByVehicleCompanyIdAndRevenueDateBetween(companyId, LocalDate.now(), LocalDate.now()).size(),
                maintenanceRecordRepository.findAllByVehicleCompanyId(companyId).stream().mapToInt(item -> item.isSuspectedDuplicate() ? 1 : 0).sum(),
                upcomingSchedules,
                messageRepository.countUnreadMessages(companyId, userId)
        );
    }

    private List<String> concatAlerts(List<String> alerts, String extra) {
        java.util.ArrayList<String> combined = new java.util.ArrayList<>(alerts);
        combined.add(extra);
        return combined;
    }
}
