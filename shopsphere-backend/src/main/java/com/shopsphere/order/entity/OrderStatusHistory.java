package com.shopsphere.order.entity;

import java.time.Instant;

import com.shopsphere.common.entity.BaseEntity;
import com.shopsphere.common.enums.OrderStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Index;

@Entity
@Table(
		name = "order_status_history",
		indexes = @Index(name = "idx_order_status_history_seller_order", columnList = "seller_order_id,changed_at"))
public class OrderStatusHistory extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(
			name = "seller_order_id",
			nullable = false,
			foreignKey = @ForeignKey(name = "fk_order_status_history_seller_order"))
	private SellerOrder sellerOrder;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private OrderStatus status;

	@Column(name = "changed_at", nullable = false)
	private Instant changedAt;

	@Column(name = "changed_by", nullable = false, length = 20)
	private String changedBy;

	@Column(length = 255)
	private String note;
	@Column(name = "location_latitude") private Double locationLatitude;
	@Column(name = "location_longitude") private Double locationLongitude;

	protected OrderStatusHistory() {
	}

	OrderStatusHistory(SellerOrder sellerOrder, OrderStatus status, String changedBy, String note, Double latitude, Double longitude) {
		this.sellerOrder = sellerOrder;
		this.status = status;
		this.changedAt = Instant.now();
		this.changedBy = changedBy;
		this.note = note;
		this.locationLatitude = latitude;
		this.locationLongitude = longitude;
	}

	public OrderStatus getStatus() {
		return status;
	}

	public Instant getChangedAt() {
		return changedAt;
	}

	public String getChangedBy() {
		return changedBy;
	}

	public String getNote() {
		return note;
	}
	public Double getLocationLatitude() { return locationLatitude; }
	public Double getLocationLongitude() { return locationLongitude; }
}
