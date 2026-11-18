package com.shopsphere.coupon.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;

import com.shopsphere.common.enums.CouponDiscountType;
import com.shopsphere.common.enums.CouponEligibility;
import com.shopsphere.common.enums.CouponScope;

public record CouponResponse(Long id, String code, String name, String description, String adminNote, CouponDiscountType discountType, BigDecimal discountValue, BigDecimal minimumOrderAmount, BigDecimal maximumDiscountAmount, Integer usageLimit, Integer perCustomerLimit, long usageCount, Instant startsAt, Instant expiresAt, boolean active, boolean archived, String status, CouponEligibility eligibility, CouponScope scope, Set<Long> productIds, Set<Long> categoryIds, Set<Long> customerIds) { }
