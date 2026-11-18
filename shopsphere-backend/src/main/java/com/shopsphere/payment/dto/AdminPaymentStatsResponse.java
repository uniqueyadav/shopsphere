package com.shopsphere.payment.dto;

import java.math.BigDecimal;

public record AdminPaymentStatsResponse(
		long totalCount, BigDecimal totalAmount,
		long successfulCount, BigDecimal successfulAmount,
		long pendingCount, BigDecimal pendingAmount,
		long failedCount, BigDecimal failedAmount,
		long refundedCount, BigDecimal refundedAmount) {
}
