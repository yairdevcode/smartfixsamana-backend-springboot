package com.smartfixsamana.models.entities;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

import com.smartfixsamana.models.enums.MovementType;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Entity
@Table(name = "inventory_movements")
public class InventoryMovement implements Serializable {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne
	@JoinColumn(name = "part_catalog_id")
	@NotNull
	private PartCatalog partCatalog;

	@ManyToOne
	@JoinColumn(name = "repair_id")
	private Repair repair;

	@Enumerated(EnumType.STRING)
	@NotNull
	private MovementType movementType;

	@NotNull
	@Positive
	private Integer quantity;

	private String reason;

	@Column(name = "created_at")
	private LocalDateTime createdAt;

	private String notes;

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

	public void setPartCatalog(PartCatalog partCatalog) {
		this.partCatalog = partCatalog;
	}

	public Repair getRepair() {
		return repair;
	}

	public void setRepair(Repair repair) {
		this.repair = repair;
	}


	public void setMovementType(MovementType movementType) {
		this.movementType = movementType;
	}

	public Integer getQuantity() {
		return quantity;
	}

	public void setQuantity(Integer quantity) {
		this.quantity = quantity;
	}


	public void setReason(String reason) {
		this.reason = reason;
	}


	public void setNotes(String notes) {
		this.notes = notes;
	}

	@Serial
	private static final long serialVersionUID = 1L;
}
