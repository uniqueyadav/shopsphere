package com.shopsphere.order.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.shopsphere.common.enums.OrderStatus;
import com.shopsphere.common.enums.PaymentMethod;
import com.shopsphere.common.enums.PaymentStatus;
import com.shopsphere.common.enums.RefundStatus;

public record AdminOrderResponse(
		Long id,
		Long customerId,
		String customerName,
		String customerEmail,
		String customerPhone,
		OrderStatus orderStatus,
		PaymentMethod paymentMethod,
		PaymentStatus paymentStatus,
		RefundStatus refundStatus,
		BigDecimal subtotal,
		BigDecimal deliveryCharges,
		BigDecimal totalAmount,
		ShippingAddressResponse shippingAddress,
		List<AdminSellerOrderResponse> sellerOrders,
		AdminPaymentResponse payment,
		List<AdminOrderTimelineEvent> timeline,
		boolean cancellationEligible,
		Instant createdAt) {
}
