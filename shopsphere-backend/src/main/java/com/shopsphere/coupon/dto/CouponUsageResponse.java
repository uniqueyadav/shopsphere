package com.shopsphere.coupon.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record CouponUsageResponse(Long usageId, String customerName, String customerEmail, Long orderId, BigDecimal discountAmount, String orderStatus, Instant usedAt) { }
