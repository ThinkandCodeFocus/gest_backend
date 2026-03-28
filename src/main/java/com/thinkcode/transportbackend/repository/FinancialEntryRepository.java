package com.thinkcode.transportbackend.repository;

import com.thinkcode.transportbackend.entity.FinancialEntry;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FinancialEntryRepository extends JpaRepository<FinancialEntry, UUID> {

    List<FinancialEntry> findAllByCompanyIdAndEntryDateBetweenOrderByEntryDateAsc(UUID companyId, LocalDate startDate, LocalDate endDate);

    Optional<FinancialEntry> findByIdAndCompanyId(UUID entryId, UUID companyId);
}
