package com.shopsphere.refund.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.shopsphere.common.enums.PaymentMethod;
import com.shopsphere.common.enums.RefundStatus;

public record AdminRefundResponse(
		Long paymentTransactionId,
		Long orderId,
		String refundReference,
		String customerName,
		String customerEmail,
		String customerPhone,
		PaymentMethod paymentMethod,
		String transactionId,
		BigDecimal orderAmount,
		BigDecimal refundAmount,
		String currency,
		RefundStatus status,
		String reason,
		Instant orderDate,
		Instant lastUpdated,
		List<RefundItem> items) {
	public record RefundItem(String productName, String sku, int quantity) { }
}
