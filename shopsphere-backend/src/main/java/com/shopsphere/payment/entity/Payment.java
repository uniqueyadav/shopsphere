package com.shopsphere.payment.entity;

import java.math.BigDecimal;

import com.shopsphere.common.entity.BaseEntity;
import com.shopsphere.common.enums.PaymentStatus;
import com.shopsphere.order.entity.Order;
import com.shopsphere.user.entity.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
		name = "payment_transactions",
		uniqueConstraints = {
				@UniqueConstraint(
						name = "uk_payment_order_idempotency",
						columnNames = {"order_id", "idempotency_key"}),
				@UniqueConstraint(name = "uk_payment_razorpay_order", columnNames = "razorpay_order_id"),
				@UniqueConstraint(name = "uk_payment_razorpay_payment", columnNames = "razorpay_payment_id")
		},
		indexes = @Index(name = "idx_payment_customer_created", columnList = "customer_id,created_at"))
public class Payment extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "order_id", nullable = false, foreignKey = @ForeignKey(name = "fk_payment_order"))
	private Order order;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "customer_id", nullable = false, foreignKey = @ForeignKey(name = "fk_payment_customer"))
	private User customer;

	@Column(name = "idempotency_key", nullable = false, length = 100)
	private String idempotencyKey;

	@Column(name = "razorpay_order_id", nullable = false, length = 100)
	private String razorpayOrderId;

	@Column(name = "razorpay_payment_id", length = 100)
	private String razorpayPaymentId;

	@Column(name = "razorpay_refund_id", unique = true, length = 100)
	private String razorpayRefundId;

	@Column(name = "razorpay_key_id", nullable = false, length = 100)
	private String razorpayKeyId;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal amount;

	@Column(nullable = false, length = 3)
	private String currency;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private PaymentStatus status;

	@Column(name = "failure_code", length = 30)
	private String failureCode;

	protected Payment() {
	}

	public Payment(
			Order order,
			User customer,
			String idempotencyKey,
			String razorpayOrderId,
			String razorpayKeyId,
			BigDecimal amount,
			String currency) {
		this.order = order;
		this.customer = customer;
		this.idempotencyKey = idempotencyKey;
		this.razorpayOrderId = razorpayOrderId;
		this.razorpayKeyId = razorpayKeyId;
		this.amount = amount;
		this.currency = currency;
		this.status = PaymentStatus.PENDING;
	}

	public Order getOrder() {
		return order;
	}

	public User getCustomer() {
		return customer;
	}

	public String getIdempotencyKey() {
		return idempotencyKey;
	}

	public String getRazorpayOrderId() {
		return razorpayOrderId;
	}

	public String getRazorpayPaymentId() {
		return razorpayPaymentId;
	}

	public String getRazorpayRefundId() {
		return razorpayRefundId;
	}

	public String getRazorpayKeyId() {
		return razorpayKeyId;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public String getCurrency() {
		return currency;
	}

	public PaymentStatus getStatus() {
		return status;
	}

	public String getFailureCode() {
		return failureCode;
	}

	public void markSuccessful(String paymentId) {
		validatePaymentId(paymentId);
		if (status == PaymentStatus.REFUNDED) {
			throw new IllegalStateException("A refunded payment cannot be marked successful.");
		}
		razorpayPaymentId = paymentId;
		status = PaymentStatus.SUCCESS;
		failureCode = null;
	}

	public void markFailed(String paymentId, String reason) {
		if (status == PaymentStatus.SUCCESS || status == PaymentStatus.REFUNDED) {
			return;
		}
		if (paymentId != null && !paymentId.isBlank()) {
			validatePaymentId(paymentId);
			razorpayPaymentId = paymentId;
		}
		status = PaymentStatus.FAILED;
		failureCode = reason;
	}

	public void markRefundInitiated(String refundId) {
		if (status != PaymentStatus.SUCCESS) {
			throw new IllegalStateException("Only a successful payment can be refunded.");
		}
		if (refundId == null || refundId.isBlank()) {
			throw new IllegalArgumentException("Razorpay refund ID is required.");
		}
		if (razorpayRefundId != null && !razorpayRefundId.equals(refundId)) {
			throw new IllegalStateException("A refund has already been initiated for this payment.");
		}
		razorpayRefundId = refundId;
	}

	public void markRefunded(String paymentId, String refundId) {
		validatePaymentId(paymentId);
		if (status != PaymentStatus.SUCCESS) {
			throw new IllegalStateException("Only a successful payment can be marked refunded.");
		}
		if (refundId != null && !refundId.isBlank()) {
			markRefundInitiated(refundId);
		}
		razorpayPaymentId = paymentId;
		status = PaymentStatus.REFUNDED;
	}

	public void clearFailedRefund(String refundId) {
		if (status != PaymentStatus.SUCCESS || !refundId.equals(razorpayRefundId)) {
			return;
		}
		razorpayRefundId = null;
	}

	private void validatePaymentId(String paymentId) {
		if (paymentId == null || paymentId.isBlank()) {
			throw new IllegalArgumentException("Razorpay payment ID is required.");
		}
		if (razorpayPaymentId != null && !razorpayPaymentId.equals(paymentId)) {
			throw new IllegalStateException("The payment attempt is already linked to a different Razorpay payment.");
		}
	}
}