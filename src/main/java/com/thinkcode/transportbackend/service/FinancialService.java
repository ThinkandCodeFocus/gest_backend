package com.thinkcode.transportbackend.service;

import com.thinkcode.transportbackend.dto.FinancialEntryRequest;
import com.thinkcode.transportbackend.dto.FinancialSummaryResponse;
import com.thinkcode.transportbackend.entity.FinancialEntry;
import com.thinkcode.transportbackend.entity.FinancialEntryType;
import com.thinkcode.transportbackend.repository.FinancialEntryRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class FinancialService {

    private final FinancialEntryRepository financialEntryRepository;
    private final CompanyResolver companyResolver;
    private final AuthenticatedCompanyProvider authenticatedCompanyProvider;
    private final AuditLogService auditLogService;

    public FinancialService(
            FinancialEntryRepository financialEntryRepository,
            CompanyResolver companyResolver,
            AuthenticatedCompanyProvider authenticatedCompanyProvider,
            AuditLogService auditLogService
    ) {
        this.financialEntryRepository = financialEntryRepository;
        this.companyResolver = companyResolver;
        this.authenticatedCompanyProvider = authenticatedCompanyProvider;
        this.auditLogService = auditLogService;
    }

    public List<FinancialEntry> findEntries(LocalDate startDate, LocalDate endDate) {
        UUID companyId = authenticatedCompanyProvider.requireCompanyId();
        return financialEntryRepository.findAllByCompanyIdAndEntryDateBetweenOrderByEntryDateAsc(companyId, startDate, endDate);
    }

    public FinancialEntry create(FinancialEntryRequest request) {
        UUID companyId = authenticatedCompanyProvider.requireCompanyId();
        if (request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Amount must be greater than zero");
        }

        FinancialEntry entry = new FinancialEntry();
        entry.setCompany(companyResolver.require(companyId));
        entry.setEntryDate(request.entryDate());
        entry.setType(request.type());
        entry.setCategory(request.category());
        entry.setAmount(request.amount());
        entry.setReference(request.reference());
        entry.setNote(request.note());
        FinancialEntry saved = financialEntryRepository.save(entry);
        auditLogService.log("CREATE", "FINANCE", saved.getId().toString(), null, saved.getType() + "|" + saved.getAmount());
        return saved;
    }

    public FinancialEntry update(UUID entryId, FinancialEntryRequest request) {
        UUID companyId = authenticatedCompanyProvider.requireCompanyId();
        FinancialEntry entry = financialEntryRepository.findByIdAndCompanyId(entryId, companyId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Financial entry not found"));
        String before = entry.getType() + "|" + entry.getAmount();

        entry.setEntryDate(request.entryDate());
        entry.setType(request.type());
        entry.setCategory(request.category());
        entry.setAmount(request.amount());
        entry.setReference(request.reference());
        entry.setNote(request.note());
        FinancialEntry saved = financialEntryRepository.save(entry);
        auditLogService.log("UPDATE", "FINANCE", saved.getId().toString(), before, saved.getType() + "|" + saved.getAmount());
        return saved;
    }

    public void delete(UUID entryId) {
        UUID companyId = authenticatedCompanyProvider.requireCompanyId();
        FinancialEntry entry = financialEntryRepository.findByIdAndCompanyId(entryId, companyId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Financial entry not found"));
        auditLogService.log("DELETE", "FINANCE", entry.getId().toString(), entry.getType() + "|" + entry.getAmount(), null);
        financialEntryRepository.delete(entry);
    }

    public FinancialSummaryResponse summary(LocalDate startDate, LocalDate endDate) {
        List<FinancialEntry> entries = findEntries(startDate, endDate);
        BigDecimal totalIncome = entries.stream()
                .filter(entry -> entry.getType() == FinancialEntryType.INCOME)
                .map(FinancialEntry::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalExpense = entries.stream()
                .filter(entry -> entry.getType() == FinancialEntryType.EXPENSE)
                .map(FinancialEntry::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new FinancialSummaryResponse(totalIncome, totalExpense, totalIncome.subtract(totalExpense));
    }
}
