package com.shopsphere.analytics.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record SellerEarningsResponse(
		Instant from,
		Instant to,
		BigDecimal grossSales,
		BigDecimal refundedAmount,
		BigDecimal sales,
		BigDecimal platformCommission,
		BigDecimal earnings,
		BigDecimal pendingBalance,
		BigDecimal totalPayouts,
		boolean earningsEstimated,
		boolean payoutTrackingAvailable,
		List<TrendPoint> trend,
		List<CategorySales> categories,
		List<Transaction> transactions) {

	public record TrendPoint(String date, BigDecimal sales, BigDecimal earnings) {}
	public record CategorySales(Long categoryId, String categoryName, BigDecimal sales) {}
	public record Transaction(Long sellerOrderId, Long orderId, Instant date, String description,
			BigDecimal amount, String status) {}
}
