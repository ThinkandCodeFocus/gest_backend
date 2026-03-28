package com.thinkcode.transportbackend.service;

import com.thinkcode.transportbackend.entity.ActivityStatus;
import com.thinkcode.transportbackend.entity.DailyRevenue;
import com.thinkcode.transportbackend.entity.Vehicle;
import com.thinkcode.transportbackend.repository.DailyRevenueRepository;
import com.thinkcode.transportbackend.dto.DailyRevenueRequest;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DailyRevenueService {

    private final DailyRevenueRepository dailyRevenueRepository;
    private final VehicleService vehicleService;
    private final DebtService debtService;
    private final AuthenticatedCompanyProvider authenticatedCompanyProvider;

    public DailyRevenueService(
            DailyRevenueRepository dailyRevenueRepository,
            VehicleService vehicleService,
            DebtService debtService,
            AuthenticatedCompanyProvider authenticatedCompanyProvider
    ) {
        this.dailyRevenueRepository = dailyRevenueRepository;
        this.vehicleService = vehicleService;
        this.debtService = debtService;
        this.authenticatedCompanyProvider = authenticatedCompanyProvider;
    }

    public List<DailyRevenue> findAll(UUID companyId) {
        return dailyRevenueRepository.findAllByVehicleCompanyId(companyId);
    }

    public DailyRevenue findByIdForCompany(UUID revenueId, UUID companyId) {
        return dailyRevenueRepository.findByIdAndVehicleCompanyId(revenueId, companyId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Daily revenue not found"));
    }

    @Transactional
    public DailyRevenue create(DailyRevenueRequest request) {
        UUID authenticatedCompanyId = authenticatedCompanyProvider.requireCompanyId();
        Vehicle vehicle = vehicleService.findByIdForCompany(
                request.vehicleId(),
            authenticatedCompanyId
        );
        if (dailyRevenueRepository.existsByVehicleIdAndRevenueDate(vehicle.getId(), request.revenueDate())) {
            throw new ApiException(HttpStatus.CONFLICT, "Revenue already exists for this vehicle and date");
        }

        DailyRevenue revenue = new DailyRevenue();
        revenue.setVehicle(vehicle);
        revenue.setRevenueDate(request.revenueDate());
        revenue.setAmount(request.amount());
        revenue.setActivityStatus(request.activityStatus());
        revenue.setNote(request.note());

        if (request.activityStatus() == ActivityStatus.ACTIVE) {
            BigDecimal driverShare = request.amount().multiply(new BigDecimal("0.30")).setScale(2, RoundingMode.HALF_UP);
            BigDecimal companyShare = request.amount().subtract(driverShare);
            revenue.setDriverShare(driverShare);
            revenue.setCompanyShare(companyShare);

            BigDecimal shortfall = vehicle.getDailyTarget().subtract(request.amount());
            if (shortfall.compareTo(BigDecimal.ZERO) > 0) {
                debtService.createAutomaticDebt(
                        vehicle,
                        vehicle.getDriver(),
                        shortfall,
                        request.revenueDate(),
                        "Objectif journalier non atteint le " + request.revenueDate()
                );
            }
        } else {
            revenue.setDriverShare(BigDecimal.ZERO);
            revenue.setCompanyShare(request.amount());
        }

        return dailyRevenueRepository.save(revenue);
    }

    @Transactional
    public DailyRevenue update(UUID revenueId, DailyRevenueRequest request) {
        UUID authenticatedCompanyId = authenticatedCompanyProvider.requireCompanyId();
        DailyRevenue revenue = findByIdForCompany(revenueId, authenticatedCompanyId);
        Vehicle vehicle = vehicleService.findByIdForCompany(request.vehicleId(), authenticatedCompanyId);

        boolean vehicleOrDateChanged = !vehicle.getId().equals(revenue.getVehicle().getId())
                || !request.revenueDate().equals(revenue.getRevenueDate());

        if (vehicleOrDateChanged
                && dailyRevenueRepository.existsByVehicleIdAndRevenueDate(vehicle.getId(), request.revenueDate())) {
            throw new ApiException(HttpStatus.CONFLICT, "Revenue already exists for this vehicle and date");
        }

        revenue.setVehicle(vehicle);
        revenue.setRevenueDate(request.revenueDate());
        revenue.setAmount(request.amount());
        revenue.setActivityStatus(request.activityStatus());
        revenue.setNote(request.note());

        if (request.activityStatus() == ActivityStatus.ACTIVE) {
            BigDecimal driverShare = request.amount().multiply(new BigDecimal("0.30")).setScale(2, RoundingMode.HALF_UP);
            BigDecimal companyShare = request.amount().subtract(driverShare);
            revenue.setDriverShare(driverShare);
            revenue.setCompanyShare(companyShare);
        } else {
            revenue.setDriverShare(BigDecimal.ZERO);
            revenue.setCompanyShare(request.amount());
        }

        return dailyRevenueRepository.save(revenue);
    }

    public void delete(UUID revenueId) {
        UUID authenticatedCompanyId = authenticatedCompanyProvider.requireCompanyId();
        DailyRevenue revenue = findByIdForCompany(revenueId, authenticatedCompanyId);
        dailyRevenueRepository.delete(revenue);
    }
}

