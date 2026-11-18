package com.shopsphere.analytics.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record RevenueReportResponse(Instant from,Instant to,BigDecimal paidOrderTotal,long paidOrderCount,List<RevenuePoint> trend,List<RecentTransaction> recentTransactions,List<CategoryRevenue> topCategories) {
	public record RevenuePoint(String label,BigDecimal amount,long orderCount){}
	public record RecentTransaction(Long orderId,String customerName,BigDecimal amount,String paymentStatus,String orderStatus,Instant createdAt){}
	public record CategoryRevenue(Long categoryId,String categoryName,String imageUrl,BigDecimal sales,BigDecimal contributionPercent){}
}
