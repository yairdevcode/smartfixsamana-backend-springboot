package com.smartfixsamana.models.entities;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.smartfixsamana.models.enums.SettlementStatus;

import jakarta.persistence.*;

@Entity
@Table(name = "settlements")
public class Settlement implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "total_repair_price", nullable = false)
    private Double totalRepairPrice = 0.0;

    @Column(name = "total_part_cost", nullable = false)
    private Double totalPartCost = 0.0;

    @Column(name = "total_my_share", nullable = false)
    private Double totalMyShare = 0.0;

    @Column(name = "total_store_share", nullable = false)
    private Double totalStoreShare = 0.0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SettlementStatus status;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "settlement")
    @JsonIgnore
    private List<ExternalRepair> repairs = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public Double getTotalRepairPrice() {
        return totalRepairPrice;
    }

    public void setTotalRepairPrice(Double totalRepairPrice) {
        this.totalRepairPrice = totalRepairPrice;
    }

    public Double getTotalPartCost() {
        return totalPartCost;
    }

    public void setTotalPartCost(Double totalPartCost) {
        this.totalPartCost = totalPartCost;
    }

    public Double getTotalMyShare() {
        return totalMyShare;
    }

    public void setTotalMyShare(Double totalMyShare) {
        this.totalMyShare = totalMyShare;
    }

    public Double getTotalStoreShare() {
        return totalStoreShare;
    }

    public void setTotalStoreShare(Double totalStoreShare) {
        this.totalStoreShare = totalStoreShare;
    }

    public SettlementStatus getStatus() {
        return status;
    }

    public void setStatus(SettlementStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public List<ExternalRepair> getRepairs() {
        return repairs;
    }

    public void setRepairs(List<ExternalRepair> repairs) {
        this.repairs = repairs;
    }

    @Serial
    private static final long serialVersionUID = 1L;
}
