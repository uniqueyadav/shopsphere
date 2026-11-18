package com.shopsphere.payment.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.shopsphere.common.enums.PaymentMethod;
import com.shopsphere.common.enums.PaymentStatus;
import com.shopsphere.common.enums.RefundStatus;

public record AdminPaymentResponse(
		Long id,
		Long orderId,
		String customerName,
		String customerEmail,
		String customerPhone,
		PaymentMethod paymentMethod,
		BigDecimal amount,
		String currency,
		PaymentStatus status,
		String transactionId,
		String gatewayOrderId,
		String refundReference,
		String failureReason,
		RefundStatus refundStatus,
		BigDecimal refundAmount,
		BigDecimal subtotal,
		BigDecimal deliveryCharges,
		BigDecimal totalAmount,
		List<SellerOrderSummary> sellerOrders,
		Instant createdAt,
		Instant updatedAt) {
	public record SellerOrderSummary(Long id, String sellerName, BigDecimal subtotal, BigDecimal deliveryCharge, BigDecimal totalAmount) { }
}
