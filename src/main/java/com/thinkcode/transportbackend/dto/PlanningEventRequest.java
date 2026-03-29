package com.thinkcode.transportbackend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.UUID;

public class PlanningEventRequest {

    @NotNull(message = "Event date is required")
    private LocalDate eventDate;

    @NotBlank(message = "Slot is required")
    private String slot; // MORNING, AFTERNOON, EVENING

    @NotBlank(message = "Type is required")
    private String type; // DRIVER or MAINTENANCE

    @NotBlank(message = "Title is required")
    private String title;

    private UUID ownerId;

    @NotBlank(message = "Priority is required")
    private String priority; // HIGH, MEDIUM, LOW

    public LocalDate getEventDate() {
        return eventDate;
    }

    public void setEventDate(LocalDate eventDate) {
        this.eventDate = eventDate;
    }

    public String getSlot() {
        return slot;
    }

    public void setSlot(String slot) {
        this.slot = slot;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(UUID ownerId) {
        this.ownerId = ownerId;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }
}
