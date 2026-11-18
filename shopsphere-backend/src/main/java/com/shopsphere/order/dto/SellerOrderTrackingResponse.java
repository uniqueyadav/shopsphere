package com.shopsphere.order.dto;

import java.util.List;

import com.shopsphere.common.enums.OrderStatus;

public record SellerOrderTrackingResponse(
		Long sellerOrderId,
		Long sellerId,
		String sellerName,
		OrderStatus status,
		List<OrderStatusHistoryResponse> history) {
}
