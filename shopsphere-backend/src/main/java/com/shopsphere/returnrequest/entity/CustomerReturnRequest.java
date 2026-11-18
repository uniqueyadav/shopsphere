package com.shopsphere.returnrequest.entity;

import java.math.BigDecimal;
import java.time.Instant;

import com.shopsphere.common.entity.BaseEntity;
import com.shopsphere.common.enums.ReturnRequestStatus;
import com.shopsphere.common.enums.ReturnRequestType;
import com.shopsphere.order.entity.OrderItem;
import com.shopsphere.user.entity.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "customer_return_requests")
public class CustomerReturnRequest extends BaseEntity {
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "customer_id", nullable = false, foreignKey = @ForeignKey(name = "fk_return_requests_customer"))
	private User customer;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "order_item_id", nullable = false, foreignKey = @ForeignKey(name = "fk_return_requests_order_item"))
	private OrderItem orderItem;

	@Enumerated(EnumType.STRING)
	@Column(name = "request_type", nullable = false, length = 20)
	private ReturnRequestType requestType;

	@Column(nullable = false, length = 60)
	private String reason;

	@Column(length = 1000)
	private String description;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private ReturnRequestStatus status;

	@Column(name = "refund_amount", nullable = false, precision = 12, scale = 2)
	private BigDecimal refundAmount;

	@Column(name = "expected_pickup_at")
	private Instant expectedPickupAt;

	@Column(name = "resolution_note", length = 500)
	private String resolutionNote;

	protected CustomerReturnRequest() { }

	public CustomerReturnRequest(User customer, OrderItem orderItem, ReturnRequestType requestType, String reason, String description) {
		this.customer = customer;
		this.orderItem = orderItem;
		this.requestType = requestType;
		this.reason = reason;
		this.description = description;
		this.status = ReturnRequestStatus.REQUESTED;
		this.refundAmount = orderItem.getLineSubtotal();
	}

	public User getCustomer() { return customer; }
	public OrderItem getOrderItem() { return orderItem; }
	public ReturnRequestType getRequestType() { return requestType; }
	public String getReason() { return reason; }
	public String getDescription() { return description; }
	public ReturnRequestStatus getStatus() { return status; }
	public BigDecimal getRefundAmount() { return refundAmount; }
	public Instant getExpectedPickupAt() { return expectedPickupAt; }
	public String getResolutionNote() { return resolutionNote; }

	public void transition(ReturnRequestStatus next, String note, Instant pickupAt) {
		this.status = next;
		this.resolutionNote = note;
		if (pickupAt != null) this.expectedPickupAt = pickupAt;
	}
}
