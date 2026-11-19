package com.shopsphere.order.entity;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.shopsphere.common.entity.BaseEntity;
import com.shopsphere.common.enums.OrderStatus;
import com.shopsphere.product.entity.Product;
import com.shopsphere.seller.entity.Seller;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
		name = "seller_orders",
		uniqueConstraints = @UniqueConstraint(
				name = "uk_seller_orders_order_seller",
				columnNames = {"order_id", "seller_id"}))
public class SellerOrder extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "order_id", nullable = false, foreignKey = @ForeignKey(name = "fk_seller_orders_order"))
	private Order order;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "seller_id", nullable = false, foreignKey = @ForeignKey(name = "fk_seller_orders_seller"))
	private Seller seller;

	@Column(name = "seller_name", nullable = false, length = 150)
	private String sellerName;

	@Enumerated(EnumType.STRING)
	@Column(name = "order_status", nullable = false, length = 20)
	private OrderStatus orderStatus;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal subtotal;

	@Column(name = "delivery_charge", nullable = false, precision = 12, scale = 2)
	private BigDecimal deliveryCharge;

	@Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
	private BigDecimal totalAmount;

	@OneToMany(mappedBy = "sellerOrder", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("id ASC")
	private List<OrderItem> items = new ArrayList<>();

	@OneToMany(mappedBy = "sellerOrder", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("changedAt ASC, id ASC")
	private Set<OrderStatusHistory> statusHistory = new LinkedHashSet<>();

	protected SellerOrder() {
	}

	public SellerOrder(
			Order order,
			Seller seller,
			BigDecimal subtotal,
			BigDecimal deliveryCharge,
			BigDecimal totalAmount) {
		this.order = order;
		this.seller = seller;
		this.sellerName = seller.getStoreName();
		this.orderStatus = order.getOrderStatus();
		this.subtotal = subtotal;
		this.deliveryCharge = deliveryCharge;
		this.totalAmount = totalAmount;
		recordStatusChange("SYSTEM", orderStatus == OrderStatus.PAYMENT_PENDING
				? "Awaiting payment."
				: "Order placed.", null, null);
	}

	public Order getOrder() {
		return order;
	}

	public Seller getSeller() {
		return seller;
	}

	public String getSellerName() {
		return sellerName;
	}

	public OrderStatus getOrderStatus() {
		return orderStatus;
	}

	public BigDecimal getSubtotal() {
		return subtotal;
	}

	public BigDecimal getDeliveryCharge() {
		return deliveryCharge;
	}

	public BigDecimal getTotalAmount() {
		return totalAmount;
	}

	public List<OrderItem> getItems() {
		return items;
	}

	public Set<OrderStatusHistory> getStatusHistory() {
		return statusHistory;
	}

	public OrderItem addItem(Product product, int quantity, BigDecimal unitPrice) {
		OrderItem item = new OrderItem(this, product, product.getProductName(), product.getSku(), quantity, unitPrice);
		items.add(item);
		return item;
	}

	public boolean transitionTo(OrderStatus nextStatus, String changedBy, String note) {
		return transitionTo(nextStatus, changedBy, note, null, null);
	}

	public boolean transitionTo(OrderStatus nextStatus, String changedBy, String note, Double latitude, Double longitude) {
		if (nextStatus == orderStatus) {
			return false;
		}
		if (!canTransitionTo(nextStatus)) {
			throw new IllegalStateException("Order status transition is not allowed.");
		}
		orderStatus = nextStatus;
		recordStatusChange(changedBy, note, latitude, longitude);
		return true;
	}

	boolean canBeCancelledByCustomer() {
		return orderStatus == OrderStatus.PLACED || orderStatus == OrderStatus.CONFIRMED;
	}

	private boolean canTransitionTo(OrderStatus nextStatus) {
		return switch (orderStatus) {
			case PAYMENT_PENDING -> nextStatus == OrderStatus.PLACED;
			case PLACED -> nextStatus == OrderStatus.CONFIRMED || nextStatus == OrderStatus.CANCELLED;
			case CONFIRMED -> nextStatus == OrderStatus.PROCESSING || nextStatus == OrderStatus.CANCELLED;
			case PROCESSING -> nextStatus == OrderStatus.SHIPPED;
			case SHIPPED -> nextStatus == OrderStatus.OUT_FOR_DELIVERY;
			case OUT_FOR_DELIVERY -> nextStatus == OrderStatus.DELIVERED;
			case DELIVERED, CANCELLED -> false;
		};
	}

	private void recordStatusChange(String changedBy, String note, Double latitude, Double longitude) {
		statusHistory.add(new OrderStatusHistory(this, orderStatus, changedBy, note, latitude, longitude));
	}
}
