package com.shopsphere.order.entity;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.Set;

import com.shopsphere.common.entity.BaseEntity;
import com.shopsphere.common.enums.AddressType;
import com.shopsphere.common.enums.OrderStatus;
import com.shopsphere.common.enums.PaymentMethod;
import com.shopsphere.common.enums.PaymentStatus;
import com.shopsphere.common.enums.RefundStatus;
import com.shopsphere.seller.entity.Seller;
import com.shopsphere.user.entity.Address;
import com.shopsphere.user.entity.User;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
		name = "orders",
		uniqueConstraints = @UniqueConstraint(
				name = "uk_orders_customer_idempotency",
				columnNames = {"customer_id", "idempotency_key"}),
		indexes = @Index(name = "idx_orders_customer_created", columnList = "customer_id,created_at"))
public class Order extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "customer_id", nullable = false, foreignKey = @ForeignKey(name = "fk_orders_customer"))
	private User customer;

	@Column(name = "idempotency_key", nullable = false, length = 100)
	private String idempotencyKey;

	@Column(name = "shipping_address_id", nullable = false)
	private Long shippingAddressId;

	@Column(name = "shipping_full_name", nullable = false, length = 100)
	private String shippingFullName;

	@Column(name = "shipping_mobile_number", nullable = false, length = 20)
	private String shippingMobileNumber;

	@Column(name = "shipping_house_flat_number", nullable = false, length = 100)
	private String shippingHouseFlatNumber;

	@Column(name = "shipping_street_area", nullable = false, length = 255)
	private String shippingStreetArea;

	@Column(name = "shipping_landmark", length = 150)
	private String shippingLandmark;

	@Column(name = "shipping_city", nullable = false, length = 100)
	private String shippingCity;

	@Column(name = "shipping_state", nullable = false, length = 100)
	private String shippingState;

	@Column(name = "shipping_pin_code", nullable = false, length = 20)
	private String shippingPinCode;
	@Column(name = "shipping_latitude") private Double shippingLatitude;
	@Column(name = "shipping_longitude") private Double shippingLongitude;
	@Column(name = "shipping_delivery_instructions", length = 500) private String shippingDeliveryInstructions;
	@Column(name = "shipping_leave_at_door", nullable = false) private boolean shippingLeaveAtDoor;
	@Column(name = "shipping_call_before_delivery", nullable = false) private boolean shippingCallBeforeDelivery;

	@Enumerated(EnumType.STRING)
	@Column(name = "shipping_address_type", nullable = false, length = 10)
	private AddressType shippingAddressType;

	@Enumerated(EnumType.STRING)
	@Column(name = "payment_method", nullable = false, length = 20)
	private PaymentMethod paymentMethod;

	@Enumerated(EnumType.STRING)
	@Column(name = "payment_status", nullable = false, length = 20)
	private PaymentStatus paymentStatus;

	@Enumerated(EnumType.STRING)
	@Column(name = "order_status", nullable = false, length = 20)
	private OrderStatus orderStatus;

	@Enumerated(EnumType.STRING)
	@Column(name = "refund_status", nullable = false, length = 20)
	private RefundStatus refundStatus;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal subtotal;

	@Column(name = "delivery_charges", nullable = false, precision = 12, scale = 2)
	private BigDecimal deliveryCharges;

	@Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
	private BigDecimal totalAmount;
	@Column(name = "discount_amount", nullable = false, precision = 12, scale = 2)
	private BigDecimal discountAmount = BigDecimal.ZERO.setScale(2);
	@Column(name = "coupon_code", length = 40)
	private String couponCode;

	@OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("id ASC")
	private Set<SellerOrder> sellerOrders = new LinkedHashSet<>();

	protected Order() {
	}

	public Order(
			User customer,
			String idempotencyKey,
			Long shippingAddressId,
			Address address,
			PaymentMethod paymentMethod,
			BigDecimal subtotal,
			BigDecimal deliveryCharges,
			BigDecimal totalAmount) {
		this.customer = customer;
		this.idempotencyKey = idempotencyKey;
		this.shippingAddressId = shippingAddressId;
		this.shippingFullName = address.getFullName();
		this.shippingMobileNumber = address.getMobileNumber();
		this.shippingHouseFlatNumber = address.getHouseFlatNumber();
		this.shippingStreetArea = address.getStreetArea();
		this.shippingLandmark = address.getLandmark();
		this.shippingCity = address.getCity();
		this.shippingState = address.getState();
		this.shippingPinCode = address.getPinCode();
		this.shippingLatitude = address.getLatitude();
		this.shippingLongitude = address.getLongitude();
		this.shippingDeliveryInstructions = address.getDeliveryInstructions();
		this.shippingLeaveAtDoor = address.isLeavePackageAtDoor();
		this.shippingCallBeforeDelivery = address.isCallBeforeDelivery();
		this.shippingAddressType = address.getAddressType();
		this.paymentMethod = paymentMethod;
		this.paymentStatus = PaymentStatus.PENDING;
		this.orderStatus = paymentMethod == PaymentMethod.ONLINE
				? OrderStatus.PAYMENT_PENDING
				: OrderStatus.PLACED;
		this.refundStatus = RefundStatus.NOT_REQUESTED;
		this.subtotal = subtotal;
		this.deliveryCharges = deliveryCharges;
		this.totalAmount = totalAmount;
	}

	public User getCustomer() {
		return customer;
	}

	public String getIdempotencyKey() {
		return idempotencyKey;
	}

	public Long getShippingAddressId() {
		return shippingAddressId;
	}

	public String getShippingFullName() {
		return shippingFullName;
	}

	public String getShippingMobileNumber() {
		return shippingMobileNumber;
	}

	public String getShippingHouseFlatNumber() {
		return shippingHouseFlatNumber;
	}

	public String getShippingStreetArea() {
		return shippingStreetArea;
	}

	public String getShippingLandmark() {
		return shippingLandmark;
	}

	public String getShippingCity() {
		return shippingCity;
	}

	public String getShippingState() {
		return shippingState;
	}

	public String getShippingPinCode() {
		return shippingPinCode;
	}
	public Double getShippingLatitude() { return shippingLatitude; }
	public Double getShippingLongitude() { return shippingLongitude; }
	public String getShippingDeliveryInstructions() { return shippingDeliveryInstructions; }
	public boolean isShippingLeaveAtDoor() { return shippingLeaveAtDoor; }
	public boolean isShippingCallBeforeDelivery() { return shippingCallBeforeDelivery; }

	public AddressType getShippingAddressType() {
		return shippingAddressType;
	}

	public PaymentMethod getPaymentMethod() {
		return paymentMethod;
	}

	public PaymentStatus getPaymentStatus() {
		return paymentStatus;
	}

	public RefundStatus getRefundStatus() {
		return refundStatus;
	}

	public void markPaymentSuccessful() {
		if (paymentStatus == PaymentStatus.SUCCESS) {
			return;
		}
		if (paymentMethod != PaymentMethod.ONLINE || paymentStatus != PaymentStatus.PENDING) {
			throw new IllegalStateException("Only a pending online order can be marked paid.");
		}
		paymentStatus = PaymentStatus.SUCCESS;
		if (sellerOrders.isEmpty()) {
			orderStatus = OrderStatus.PLACED;
		} else {
			sellerOrders.forEach(sellerOrder ->
					sellerOrder.transitionTo(OrderStatus.PLACED, "PAYMENT", "Payment verified."));
			refreshOrderStatus();
		}
	}

	public void markPaymentRefunded() {
		if (paymentStatus != PaymentStatus.SUCCESS) {
			throw new IllegalStateException("Only a successful payment can be marked refunded.");
		}
		paymentStatus = PaymentStatus.REFUNDED;
		refundStatus = RefundStatus.REFUNDED;
	}

	public void markRefundPending() {
		if (paymentMethod != PaymentMethod.ONLINE || paymentStatus != PaymentStatus.SUCCESS) {
			throw new IllegalStateException("Only a successful online payment can have a pending refund.");
		}
		refundStatus = RefundStatus.PENDING;
	}

	public void markRefundFailed() {
		if (refundStatus == RefundStatus.PENDING) {
			refundStatus = RefundStatus.FAILED;
		}
	}

	public OrderStatus getOrderStatus() {
		return orderStatus;
	}

	public BigDecimal getSubtotal() {
		return subtotal;
	}

	public BigDecimal getDeliveryCharges() {
		return deliveryCharges;
	}

	public BigDecimal getTotalAmount() {
		return totalAmount;
	}
	public BigDecimal getDiscountAmount() { return discountAmount; }
	public String getCouponCode() { return couponCode; }
	public void applyCoupon(String code, BigDecimal discount) {
		if (couponCode != null || discount == null || discount.signum() < 0 || discount.compareTo(totalAmount) > 0) throw new IllegalArgumentException("Coupon discount is invalid.");
		couponCode = code;
		discountAmount = discount;
		totalAmount = totalAmount.subtract(discount);
	}

	public Set<SellerOrder> getSellerOrders() {
		return sellerOrders;
	}

	public SellerOrder addSellerOrder(
			Seller seller,
			BigDecimal sellerSubtotal,
			BigDecimal deliveryCharge,
			BigDecimal sellerTotal) {
		SellerOrder sellerOrder = new SellerOrder(this, seller, sellerSubtotal, deliveryCharge, sellerTotal);
		sellerOrders.add(sellerOrder);
		return sellerOrder;
	}

	public boolean isCancellationEligible() {
		return !sellerOrders.isEmpty() && sellerOrders.stream().allMatch(SellerOrder::canBeCancelledByCustomer);
	}

	public void cancel(String actor, String note) {
		if (!isCancellationEligible()) {
			throw new IllegalStateException("Order can no longer be cancelled.");
		}
		sellerOrders.forEach(sellerOrder -> sellerOrder.transitionTo(OrderStatus.CANCELLED, actor, note));
		refreshOrderStatus();
	}

	public void refreshOrderStatus() {
		if (sellerOrders.isEmpty()) {
			return;
		}
		Set<OrderStatus> statuses = sellerOrders.stream()
				.map(SellerOrder::getOrderStatus)
				.collect(java.util.stream.Collectors.toSet());
		if (statuses.contains(OrderStatus.PAYMENT_PENDING)) {
			orderStatus = OrderStatus.PAYMENT_PENDING;
			return;
		}
		Set<OrderStatus> activeStatuses = statuses.stream()
				.filter(status -> status != OrderStatus.CANCELLED)
				.collect(java.util.stream.Collectors.toSet());
		if (activeStatuses.isEmpty()) {
			orderStatus = OrderStatus.CANCELLED;
		} else if (activeStatuses.stream().allMatch(status -> status == OrderStatus.DELIVERED)) {
			orderStatus = OrderStatus.DELIVERED;
		} else if (activeStatuses.contains(OrderStatus.OUT_FOR_DELIVERY)) {
			orderStatus = OrderStatus.OUT_FOR_DELIVERY;
		} else if (activeStatuses.contains(OrderStatus.SHIPPED)) {
			orderStatus = OrderStatus.SHIPPED;
		} else if (activeStatuses.contains(OrderStatus.PROCESSING)) {
			orderStatus = OrderStatus.PROCESSING;
		} else if (activeStatuses.contains(OrderStatus.CONFIRMED)) {
			orderStatus = OrderStatus.CONFIRMED;
		} else {
			orderStatus = OrderStatus.PLACED;
		}
	}
}
