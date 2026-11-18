package com.shopsphere.product.dto;

public record AdminProductStatsResponse(long total, long approved, long pending, long rejected) {
}
