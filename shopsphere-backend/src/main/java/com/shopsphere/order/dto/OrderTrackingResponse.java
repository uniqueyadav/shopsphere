package com.shopsphere.order.dto;

import java.time.Instant;
import java.util.List;

import com.shopsphere.common.enums.OrderStatus;
import com.shopsphere.common.enums.PaymentStatus;
import com.shopsphere.common.enums.RefundStatus;

public record OrderTrackingResponse(
		Long orderId,
		OrderStatus status,
		PaymentStatus paymentStatus,
		RefundStatus refundStatus,
		Instant createdAt,
		List<SellerOrderTrackingResponse> sellerOrders) {
}
