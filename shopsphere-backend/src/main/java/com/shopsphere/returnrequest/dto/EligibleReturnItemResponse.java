package com.shopsphere.returnrequest.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record EligibleReturnItemResponse(Long orderItemId, Long orderId, String productName, String imageUrl, String sku,
		String sellerName, int quantity, BigDecimal amount, Instant deliveredAt, Instant eligibleUntil) { }
