package com.shopsphere.analytics.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record DashboardResponse(
		String period,
		Instant generatedAt,
		Metric revenue,
		Metric orders,
		Metric customers,
		Metric activeSellers,
		List<TrendPoint> trend,
		long totalOrders,
		List<StatusCount> orderStatuses,
		List<RecentOrder> recentOrders,
		List<TopProduct> topProducts) {

	public record Metric(BigDecimal value, BigDecimal growthPercent, List<BigDecimal> sparkline) {}
	public record TrendPoint(String label, BigDecimal revenue, long orders) {}
	public record StatusCount(String status, long count) {}
	public record RecentOrder(Long id, String customerName, Instant createdAt, BigDecimal amount, String status) {}
	public record TopProduct(Long id, String name, String imageUrl, long unitsSold, BigDecimal sales) {}
}
