package com.shopsphere.order.dto;

import java.time.Instant;

import com.shopsphere.common.enums.OrderStatus;

public record AdminOrderTimelineEvent(
		String label,
		OrderStatus status,
		Instant timestamp,
		String source,
		String note) {
}
