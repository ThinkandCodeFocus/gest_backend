package com.thinkcode.transportbackend.service;

import com.thinkcode.transportbackend.entity.ActivityStatus;
import com.thinkcode.transportbackend.entity.DailyRevenue;
import com.thinkcode.transportbackend.entity.Vehicle;
import com.thinkcode.transportbackend.repository.DailyRevenueRepository;
import com.thinkcode.transportbackend.dto.DailyRevenueRequest;
import com.thinkcode.transportbackend.dto.DailyRevenueResponse;
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

    public List<DailyRevenueResponse> findAllResponses(UUID companyId) {
        return findAll(companyId).stream()
                .sorted((left, right) -> right.getRevenueDate().compareTo(left.getRevenueDate()))
                .map(this::toResponse)
                .toList();
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
            BigDecimal driverShare = defaultShare(
                    request.driverShare(),
                    request.amount().multiply(new BigDecimal("0.30")).setScale(2, RoundingMode.HALF_UP)
            );
            BigDecimal companyShare = defaultShare(
                    request.companyShare(),
                    request.amount().subtract(driverShare)
            );
            BigDecimal clientShare = defaultShare(
                    request.clientShare(),
                    request.amount().subtract(driverShare).subtract(companyShare).max(BigDecimal.ZERO)
            );
            revenue.setDriverShare(driverShare);
            revenue.setCompanyShare(companyShare);
            revenue.setClientShare(clientShare);

            BigDecimal shortfall = defaultShare(request.generatedDebt(), vehicle.getDailyTarget().subtract(request.amount()));
            if (shortfall.compareTo(BigDecimal.ZERO) > 0) {
                revenue.setGeneratedDebt(shortfall);
                debtService.createAutomaticDebt(
                        vehicle,
                        vehicle.getDriver(),
                        shortfall,
                        request.revenueDate(),
                        "Objectif journalier non atteint le " + request.revenueDate()
                );
            } else {
                revenue.setGeneratedDebt(BigDecimal.ZERO);
            }
        } else {
            revenue.setDriverShare(defaultShare(request.driverShare(), BigDecimal.ZERO));
            revenue.setCompanyShare(defaultShare(request.companyShare(), BigDecimal.ZERO));
            revenue.setClientShare(defaultShare(request.clientShare(), BigDecimal.ZERO));
            revenue.setGeneratedDebt(defaultShare(request.generatedDebt(), BigDecimal.ZERO));
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
            BigDecimal driverShare = defaultShare(
                    request.driverShare(),
                    request.amount().multiply(new BigDecimal("0.30")).setScale(2, RoundingMode.HALF_UP)
            );
            BigDecimal companyShare = defaultShare(
                    request.companyShare(),
                    request.amount().subtract(driverShare)
            );
            BigDecimal clientShare = defaultShare(
                    request.clientShare(),
                    request.amount().subtract(driverShare).subtract(companyShare).max(BigDecimal.ZERO)
            );
            revenue.setDriverShare(driverShare);
            revenue.setCompanyShare(companyShare);
            revenue.setClientShare(clientShare);
            BigDecimal shortfall = defaultShare(request.generatedDebt(), vehicle.getDailyTarget().subtract(request.amount()));
            revenue.setGeneratedDebt(shortfall.compareTo(BigDecimal.ZERO) > 0 ? shortfall : BigDecimal.ZERO);
        } else {
            revenue.setDriverShare(defaultShare(request.driverShare(), BigDecimal.ZERO));
            revenue.setCompanyShare(defaultShare(request.companyShare(), BigDecimal.ZERO));
            revenue.setClientShare(defaultShare(request.clientShare(), BigDecimal.ZERO));
            revenue.setGeneratedDebt(defaultShare(request.generatedDebt(), BigDecimal.ZERO));
        }

        return dailyRevenueRepository.save(revenue);
    }

    public void delete(UUID revenueId) {
        UUID authenticatedCompanyId = authenticatedCompanyProvider.requireCompanyId();
        DailyRevenue revenue = findByIdForCompany(revenueId, authenticatedCompanyId);
        dailyRevenueRepository.delete(revenue);
    }

    public DailyRevenueResponse toResponse(DailyRevenue revenue) {
        return new DailyRevenueResponse(
                revenue.getId(),
                revenue.getRevenueDate(),
                revenue.getAmount(),
                revenue.getActivityStatus() == null ? null : revenue.getActivityStatus().name(),
                revenue.getDriverShare(),
                revenue.getCompanyShare(),
                revenue.getClientShare(),
                revenue.getGeneratedDebt(),
                revenue.getNote(),
                revenue.getVehicle() == null
                        ? null
                        : new DailyRevenueResponse.VehicleSummary(
                                revenue.getVehicle().getId(),
                                revenue.getVehicle().getMatricule()
                        )
        );
    }

    private BigDecimal defaultShare(BigDecimal explicitValue, BigDecimal fallback) {
        return explicitValue == null ? fallback : explicitValue.max(BigDecimal.ZERO);
    }
}

