package com.smartfixsamana.models.entities;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.*;

@Entity
@Table(name = "repairs")
public class Repair implements Serializable{

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne()
	@JoinColumn(name = "customer_id")
	private Customer customer;

	@ManyToOne()
	@JoinColumn(name = "phone_id")
	private Phone phone;

	@Column(name = "fault")
	private String fault;

	private String state;

	@Column(name = "date")
	private LocalDate date;

	@OneToMany(mappedBy = "repair", cascade = CascadeType.ALL, orphanRemoval = true)
	@JsonIgnore
	private List<RepairPart> partsUsed = new ArrayList<>();

	@Column(name = "total_parts_cost")
	private Double totalPartsCost = 0.0;

	@Column(name = "labor_cost")
	private Double laborCost = 0.0;

	@Column(name = "total_cost")
	private Double totalCost = 0.0;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Customer getCustomer() {
		return customer;
	}

	public void setCustomer(Customer customer) {
		this.customer = customer;
	}

	public Phone getPhone() {
		return phone;
	}

	public void setPhone(Phone phone) {
		this.phone = phone;
	}

	public String getFault() {
		return fault;
	}

	public void setFault(String fault) {
		this.fault = fault;
	}

	public String getState() {
		return state;
	}

	public void setState(String state) {
		this.state = state;
	}

	public LocalDate getDate() {
		return date;
	}

	public void setDate(LocalDate date) {
		this.date = date;
	}

	public List<RepairPart> getPartsUsed() {
		return partsUsed;
	}

	public void setPartsUsed(List<RepairPart> partsUsed) {
		this.partsUsed = partsUsed;
	}

	public Double getTotalPartsCost() {
		return totalPartsCost;
	}

	public void setTotalPartsCost(Double totalPartsCost) {
		this.totalPartsCost = totalPartsCost;
	}

	public Double getLaborCost() {
		return laborCost;
	}

	public void setLaborCost(Double laborCost) {
		this.laborCost = laborCost;
	}

	public Double getTotalCost() {
		return totalCost;
	}

	public void setTotalCost(Double totalCost) {
		this.totalCost = totalCost;
	}

	public void recalculateTotalCost() {
		this.totalCost = (this.totalPartsCost != null ? this.totalPartsCost : 0.0)
			+ (this.laborCost != null ? this.laborCost : 0.0);
	}

	@Serial
    private static final long serialVersionUID = 1L;

}
