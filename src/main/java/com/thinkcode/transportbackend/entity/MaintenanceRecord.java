package com.thinkcode.transportbackend.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.thinkcode.transportbackend.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
public class MaintenanceRecord extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(nullable = false)
    @JsonIgnoreProperties({"company"})
    private Vehicle vehicle;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MaintenanceType type;

    @Column(nullable = false)
    private LocalDate maintenanceDate;

    @Column(nullable = false)
    private BigDecimal cost;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private BigDecimal partsCost = BigDecimal.ZERO;

    @Column(nullable = false)
    private BigDecimal laborCost = BigDecimal.ZERO;

    @Column(nullable = false)
    private Integer pieceCount = 1;

    private String provider;

    private String documentUrl;

    @Column(nullable = false)
    private boolean suspectedDuplicate;

    @Column(length = 2000)
    private String fraudReason;

    @Column(length = 6000)
    private String pieceLinesJson;

    @Column(nullable = false)
    private String validationStatus = "En attente";

    private String createdBy;

    private String validatedBy;

    public Vehicle getVehicle() {
        return vehicle;
    }

    public void setVehicle(Vehicle vehicle) {
        this.vehicle = vehicle;
    }

    public MaintenanceType getType() {
        return type;
    }

    public void setType(MaintenanceType type) {
        this.type = type;
    }

    public LocalDate getMaintenanceDate() {
        return maintenanceDate;
    }

    public void setMaintenanceDate(LocalDate maintenanceDate) {
        this.maintenanceDate = maintenanceDate;
    }

    public BigDecimal getCost() {
        return cost;
    }

    public void setCost(BigDecimal cost) {
        this.cost = cost;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getPartsCost() {
        return partsCost;
    }

    public void setPartsCost(BigDecimal partsCost) {
        this.partsCost = partsCost;
    }

    public BigDecimal getLaborCost() {
        return laborCost;
    }

    public void setLaborCost(BigDecimal laborCost) {
        this.laborCost = laborCost;
    }

    public Integer getPieceCount() {
        return pieceCount;
    }

    public void setPieceCount(Integer pieceCount) {
        this.pieceCount = pieceCount;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getDocumentUrl() {
        return documentUrl;
    }

    public void setDocumentUrl(String documentUrl) {
        this.documentUrl = documentUrl;
    }

    public boolean isSuspectedDuplicate() {
        return suspectedDuplicate;
    }

    public void setSuspectedDuplicate(boolean suspectedDuplicate) {
        this.suspectedDuplicate = suspectedDuplicate;
    }

    public String getFraudReason() {
        return fraudReason;
    }

    public void setFraudReason(String fraudReason) {
        this.fraudReason = fraudReason;
    }

    public String getPieceLinesJson() {
        return pieceLinesJson;
    }

    public void setPieceLinesJson(String pieceLinesJson) {
        this.pieceLinesJson = pieceLinesJson;
    }

    public String getValidationStatus() {
        return validationStatus;
    }

    public void setValidationStatus(String validationStatus) {
        this.validationStatus = validationStatus;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getValidatedBy() {
        return validatedBy;
    }

    public void setValidatedBy(String validatedBy) {
        this.validatedBy = validatedBy;
    }
}
