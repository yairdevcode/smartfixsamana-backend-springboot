package com.smartfixsamana.models.entities;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;

import com.smartfixsamana.models.enums.ExternalRepairStatus;

import jakarta.persistence.*;

@Entity
@Table(name = "external_repairs")
public class ExternalRepair implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "client_name", nullable = false)
    private String clientName;

    @Column(name = "phone_brand", nullable = false)
    private String phoneBrand;

    @Column(nullable = false)
    private String solution;

    @Column(name = "repair_price", nullable = false)
    private Double repairPrice;

    @Column(name = "part_cost")
    private Double partCost = 0.0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExternalRepairStatus status;

    @Column(nullable = false)
    private LocalDate date;

    private String notes;

    @ManyToOne
    @JoinColumn(name = "settlement_id")
    private Settlement settlement;

    // Computed getters
    public Double getNetProfit() {
        double cost = partCost != null ? partCost : 0.0;
        return repairPrice - cost;
    }

    public Double getMyShare() {
        double cost = partCost != null ? partCost : 0.0;
        return (getNetProfit() * 0.60) + cost;
    }

    public Double getStoreShare() {
        return getNetProfit() * 0.40;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public String getPhoneBrand() {
        return phoneBrand;
    }

    public void setPhoneBrand(String phoneBrand) {
        this.phoneBrand = phoneBrand;
    }

    public String getSolution() {
        return solution;
    }

    public void setSolution(String solution) {
        this.solution = solution;
    }

    public Double getRepairPrice() {
        return repairPrice;
    }

    public void setRepairPrice(Double repairPrice) {
        this.repairPrice = repairPrice;
    }

    public Double getPartCost() {
        return partCost;
    }

    public void setPartCost(Double partCost) {
        this.partCost = partCost;
    }

    public ExternalRepairStatus getStatus() {
        return status;
    }

    public void setStatus(ExternalRepairStatus status) {
        this.status = status;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Settlement getSettlement() {
        return settlement;
    }

    public void setSettlement(Settlement settlement) {
        this.settlement = settlement;
    }

    @Serial
    private static final long serialVersionUID = 1L;
}
