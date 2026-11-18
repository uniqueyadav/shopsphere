package com.shopsphere.order.dto;

import java.math.BigDecimal;
import java.time.Instant;

import com.shopsphere.common.enums.PaymentStatus;
import com.shopsphere.payment.entity.Payment;

public record AdminPaymentResponse(
		Long transactionId,
		PaymentStatus status,
		BigDecimal amount,
		String currency,
		String paymentReference,
		String refundReference,
		Instant createdAt,
		Instant updatedAt) {
	public static AdminPaymentResponse from(Payment payment) {
		return new AdminPaymentResponse(
				payment.getId(), payment.getStatus(), payment.getAmount(), payment.getCurrency(),
				payment.getRazorpayPaymentId(), payment.getRazorpayRefundId(),
				payment.getCreatedAt(), payment.getUpdatedAt());
	}
}
