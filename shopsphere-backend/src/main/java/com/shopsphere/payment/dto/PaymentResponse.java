package com.shopsphere.payment.dto;

import com.shopsphere.common.enums.PaymentStatus;

public record PaymentResponse(
		Long paymentTransactionId,
		Long orderId,
		String razorpayOrderId,
		long amountPaise,
		String currency,
		String keyId,
		PaymentStatus status) {
}