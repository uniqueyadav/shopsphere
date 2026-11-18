package com.shopsphere.coupon.dto;

import java.math.BigDecimal;

public record CouponAnalyticsResponse(long successfulUses, BigDecimal totalDiscountAmount, long associatedOrders) { }
