package com.thinkcode.transportbackend.controller;

import com.thinkcode.transportbackend.dto.DriverAbsenceRequest;
import com.thinkcode.transportbackend.entity.DriverAbsence;
import com.thinkcode.transportbackend.service.HrService;
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
@RequestMapping("/hr")
public class HrController {

    private final HrService hrService;

    public HrController(HrService hrService) {
        this.hrService = hrService;
    }

    @GetMapping("/absences")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATIONS_MANAGER', 'ASSISTANT')")
    public List<DriverAbsence> findAbsences(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        return hrService.findAbsences(startDate, endDate);
    }

    @PostMapping("/absences")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATIONS_MANAGER')")
    public DriverAbsence createAbsence(@Valid @RequestBody DriverAbsenceRequest request) {
        return hrService.createAbsence(request);
    }

    @PutMapping("/absences/{absenceId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATIONS_MANAGER')")
    public DriverAbsence updateAbsence(@PathVariable UUID absenceId, @Valid @RequestBody DriverAbsenceRequest request) {
        return hrService.updateAbsence(absenceId, request);
    }

    @DeleteMapping("/absences/{absenceId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATIONS_MANAGER')")
    public void deleteAbsence(@PathVariable UUID absenceId) {
        hrService.deleteAbsence(absenceId);
    }
}
