package com.thinkcode.transportbackend.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "planning_events")
public class PlanningEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(nullable = false)
    private LocalDate eventDate;

    @Column(nullable = false)  
    private String slot; // MORNING, AFTERNOON, EVENING

    @Column(nullable = false)
    private String type; // DRIVER or MAINTENANCE

    @Column(nullable = false)
    private String title;

    @ManyToOne
    @JoinColumn(name = "owner_id")
    private UserAccount owner;

    @Column(nullable = false)
    private String priority; // HIGH, MEDIUM, LOW

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Company getCompany() {
        return company;
    }

    public void setCompany(Company company) {
        this.company = company;
    }

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

    public UserAccount getOwner() {
        return owner;
    }

    public void setOwner(UserAccount owner) {
        this.owner = owner;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }
}
