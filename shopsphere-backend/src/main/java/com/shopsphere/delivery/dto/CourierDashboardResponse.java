package com.shopsphere.delivery.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record CourierDashboardResponse(
        long totalDeliveries,
        long successfulDeliveries,
        long inTransit,
        long failedDeliveries,
        List<TrendDay> trend,
        List<RecentDelivery> recentDeliveries) {
    public record TrendDay(LocalDate date, long total, long successful, long failed) {}
    public record RecentDelivery(Long sellerOrderId, Long orderId, String customerName,
            String fulfillmentPartner, String status, String city, Instant createdAt) {}
}
