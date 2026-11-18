package com.shopsphere.order.dto;

import java.math.BigDecimal;

public record OrderItemResponse(
		Long productId,
		String productName,
		String imageUrl,
		String sku,
		int quantity,
		BigDecimal unitPrice,
		BigDecimal lineSubtotal) {
}
