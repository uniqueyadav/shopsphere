package com.shopsphere.order.dto;

import java.math.BigDecimal;

public record SellerOrderItemManagementResponse(
		Long productId, String productName, String sku, String imageUrl, int quantity,
		BigDecimal unitPrice, BigDecimal lineSubtotal) {}
