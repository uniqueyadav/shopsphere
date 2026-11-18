package com.shopsphere.category.dto;

public record CategoryStatsResponse(long total, long active, long inactive, long subcategories) {
}
