package com.shopsphere.order.dto;

import java.math.BigDecimal;
import java.util.List;

import com.shopsphere.common.enums.OrderStatus;

public record AdminSellerOrderResponse(
		Long sellerOrderId,
		Long sellerId,
		String sellerName,
		OrderStatus orderStatus,
		BigDecimal subtotal,
		BigDecimal deliveryCharge,
		BigDecimal totalAmount,
		List<AdminOrderItemResponse> items,
		List<OrderStatusHistoryResponse> history) {
}
