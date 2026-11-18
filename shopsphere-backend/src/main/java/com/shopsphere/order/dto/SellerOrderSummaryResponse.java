package com.shopsphere.order.dto;

public record SellerOrderSummaryResponse(long totalOrders, long newOrders, long processing, long shipped, long delivered, long cancelled) {}
