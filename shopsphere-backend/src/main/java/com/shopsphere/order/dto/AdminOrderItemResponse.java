package com.shopsphere.order.dto;

import java.math.BigDecimal;

public record AdminOrderItemResponse(
		Long productId,
		String productName,
		String sku,
		String imageUrl,
		String sellerName,
		int quantity,
		BigDecimal unitPrice,
		BigDecimal lineSubtotal) {
}
