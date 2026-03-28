package com.thinkcode.transportbackend.controller;

import com.thinkcode.transportbackend.dto.FinancialEntryRequest;
import com.thinkcode.transportbackend.dto.FinancialSummaryResponse;
import com.thinkcode.transportbackend.entity.FinancialEntry;
import com.thinkcode.transportbackend.service.FinancialService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/finance")
public class FinancialController {

    private final FinancialService financialService;

    public FinancialController(FinancialService financialService) {
        this.financialService = financialService;
    }

    @GetMapping("/entries")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATIONS_MANAGER', 'ASSISTANT')")
    public List<FinancialEntry> findEntries(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        return financialService.findEntries(startDate, endDate);
    }

    @PostMapping("/entries")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATIONS_MANAGER')")
    public FinancialEntry create(@Valid @RequestBody FinancialEntryRequest request) {
        return financialService.create(request);
    }

    @PutMapping("/entries/{entryId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATIONS_MANAGER')")
    public FinancialEntry update(@PathVariable UUID entryId, @Valid @RequestBody FinancialEntryRequest request) {
        return financialService.update(entryId, request);
    }

    @DeleteMapping("/entries/{entryId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATIONS_MANAGER')")
    public void delete(@PathVariable UUID entryId) {
        financialService.delete(entryId);
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATIONS_MANAGER', 'ASSISTANT')")
    public FinancialSummaryResponse summary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        return financialService.summary(startDate, endDate);
    }
}
