package com.shopsphere.order.dto;

public record CancellationResponse(
		OrderResponse order,
		boolean refundPending) {
}
