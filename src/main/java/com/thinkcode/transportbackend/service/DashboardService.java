package com.thinkcode.transportbackend.service;

import com.thinkcode.transportbackend.entity.Debt;
import com.thinkcode.transportbackend.entity.DebtStatus;
import com.thinkcode.transportbackend.entity.VehicleStatus;
import com.thinkcode.transportbackend.repository.DailyRevenueRepository;
import com.thinkcode.transportbackend.repository.DebtRepository;
import com.thinkcode.transportbackend.repository.MaintenanceRecordRepository;
import com.thinkcode.transportbackend.repository.VehicleRepository;
import com.thinkcode.transportbackend.dto.DashboardResponse;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class DashboardService {

    private final VehicleRepository vehicleRepository;
    private final DailyRevenueRepository dailyRevenueRepository;
    private final DebtRepository debtRepository;
    private final MaintenanceRecordRepository maintenanceRecordRepository;

    public DashboardService(
            VehicleRepository vehicleRepository,
            DailyRevenueRepository dailyRevenueRepository,
            DebtRepository debtRepository,
            MaintenanceRecordRepository maintenanceRecordRepository
    ) {
        this.vehicleRepository = vehicleRepository;
        this.dailyRevenueRepository = dailyRevenueRepository;
        this.debtRepository = debtRepository;
        this.maintenanceRecordRepository = maintenanceRecordRepository;
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
}

