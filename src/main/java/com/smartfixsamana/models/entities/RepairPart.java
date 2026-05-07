package com.smartfixsamana.models.entities;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Entity
@Table(name = "repair_parts")
public class RepairPart implements Serializable {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne
	@JoinColumn(name = "repair_id")
	@NotNull
	private Repair repair;

	@ManyToOne
	@JoinColumn(name = "part_catalog_id")
	@NotNull
	private PartCatalog partCatalog;

	@Positive
	private Integer quantity = 1;

	@Column(name = "price_charged")
	private Double priceCharged;

	@Column(name = "created_at")
	private LocalDateTime createdAt;

	@PrePersist
	protected void onCreate() {
		createdAt = LocalDateTime.now();
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Repair getRepair() {
		return repair;
	}

	public void setRepair(Repair repair) {
		this.repair = repair;
	}

	public PartCatalog getPartCatalog() {
		return partCatalog;
	}

	public void setPartCatalog(PartCatalog partCatalog) {
		this.partCatalog = partCatalog;
	}

	public Integer getQuantity() {
		return quantity;
	}

	public void setQuantity(Integer quantity) {
		this.quantity = quantity;
	}

	public Double getPriceCharged() {
		return priceCharged;
	}

	public void setPriceCharged(Double priceCharged) {
		this.priceCharged = priceCharged;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}

	@Serial
	private static final long serialVersionUID = 1L;

}
