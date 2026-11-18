package com.shopsphere.coupon.dto;

public record CouponStatsResponse(long total, long active, long expired, long inactive, long scheduled) { }
