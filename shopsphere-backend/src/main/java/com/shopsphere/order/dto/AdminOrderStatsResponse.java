package com.shopsphere.order.dto;

public record AdminOrderStatsResponse(long total, long delivered, long processing, long cancelled) {
}
