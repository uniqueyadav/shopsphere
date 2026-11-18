package com.shopsphere.analytics.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record SellerDashboardResponse(
		String storeName,
		String sellerName,
		String period,
		Instant generatedAt,
		Metric sales,
		Metric orders,
		Metric earnings,
		Double storeRating,
		long reviewCount,
		long productCount,
		long lowStockProductCount,
		List<TrendPoint> trend,
		List<TopProduct> topProducts,
		List<RecentOrder> recentOrders) {

	public record Metric(BigDecimal value, BigDecimal growthPercent) {}
	public record TrendPoint(String date, BigDecimal sales, long orders) {}
	public record TopProduct(Long id, String name, String imageUrl, long unitsSold, BigDecimal sales) {}
	public record RecentOrder(Long sellerOrderId, Long orderId, String customerName, BigDecimal amount, String status, Instant createdAt) {}
}
