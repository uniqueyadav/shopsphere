package com.shopsphere.order.dto;

import java.time.Instant;

import com.shopsphere.common.enums.OrderStatus;

public record OrderStatusHistoryResponse(
		OrderStatus status,
		Instant changedAt,
		String changedBy,
		String note,
		Double latitude,
		Double longitude) {
}
