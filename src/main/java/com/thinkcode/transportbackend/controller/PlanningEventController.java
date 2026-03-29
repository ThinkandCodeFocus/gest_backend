package com.thinkcode.transportbackend.controller;

import com.thinkcode.transportbackend.dto.PlanningEventRequest;
import com.thinkcode.transportbackend.entity.PlanningEvent;
import com.thinkcode.transportbackend.service.PlanningEventService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/planning")
public class PlanningEventController {

    private final PlanningEventService planningEventService;

    public PlanningEventController(PlanningEventService planningEventService) {
        this.planningEventService = planningEventService;
    }

    @GetMapping("/events")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPS_MANAGER', 'ASSISTANT')")
    public ResponseEntity<List<PlanningEvent>> getEvents(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String type) {
        List<PlanningEvent> events = type == null
                ? planningEventService.getEventsByDateRange(startDate, endDate)
                : planningEventService.getEventsByType(type, startDate, endDate);
        return ResponseEntity.ok(events);
    }

    @PostMapping("/events")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPS_MANAGER')")
    public ResponseEntity<PlanningEvent> createEvent(@Valid @RequestBody PlanningEventRequest request) {
        PlanningEvent event = planningEventService.createEvent(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(event);
    }

    @PutMapping("/events/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPS_MANAGER')")
    public ResponseEntity<PlanningEvent> updateEvent(@PathVariable UUID id, @Valid @RequestBody PlanningEventRequest request) {
        PlanningEvent event = planningEventService.updateEvent(id, request);
        return ResponseEntity.ok(event);
    }

    @DeleteMapping("/events/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPS_MANAGER')")
    public ResponseEntity<Void> deleteEvent(@PathVariable UUID id) {
        planningEventService.deleteEvent(id);
        return ResponseEntity.noContent().build();
    }
}
