package com.thinkcode.transportbackend.controller;

import com.thinkcode.transportbackend.dto.PlanningEventRequest;
import com.thinkcode.transportbackend.dto.PlanningEventResponse;
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
@RequestMapping("/planning")
public class PlanningEventController {

    private final PlanningEventService planningEventService;

    public PlanningEventController(PlanningEventService planningEventService) {
        this.planningEventService = planningEventService;
    }

    @GetMapping("/events")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATIONS_MANAGER', 'ASSISTANT')")
    public ResponseEntity<List<PlanningEventResponse>> getEvents(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String type) {
        List<PlanningEvent> events = type == null
                ? planningEventService.getEventsByDateRange(startDate, endDate)
                : planningEventService.getEventsByType(type, startDate, endDate);
        return ResponseEntity.ok(events.stream().map(this::toResponse).toList());
    }

    @PostMapping("/events")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATIONS_MANAGER', 'ASSISTANT')")
    public ResponseEntity<PlanningEventResponse> createEvent(@Valid @RequestBody PlanningEventRequest request) {
        PlanningEvent event = planningEventService.createEvent(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(event));
    }

    @PutMapping("/events/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATIONS_MANAGER', 'ASSISTANT')")
    public ResponseEntity<PlanningEventResponse> updateEvent(@PathVariable UUID id, @Valid @RequestBody PlanningEventRequest request) {
        PlanningEvent event = planningEventService.updateEvent(id, request);
        return ResponseEntity.ok(toResponse(event));
    }

    @DeleteMapping("/events/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATIONS_MANAGER')")
    public ResponseEntity<Void> deleteEvent(@PathVariable UUID id) {
        planningEventService.deleteEvent(id);
        return ResponseEntity.noContent().build();
    }

    private PlanningEventResponse toResponse(PlanningEvent event) {
        return new PlanningEventResponse(
                event.getId(),
                event.getEventDate(),
                event.getSlot(),
                event.getType(),
                event.getTitle(),
                event.getOwner() == null ? null : event.getOwner().getId(),
                event.getOwner() == null ? null : event.getOwner().getFullName(),
                event.getPriority()
        );
    }
}
