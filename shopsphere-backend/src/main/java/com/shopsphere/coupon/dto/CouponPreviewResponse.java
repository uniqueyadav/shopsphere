package com.shopsphere.coupon.dto;

import java.math.BigDecimal;

public record CouponPreviewResponse(String code, BigDecimal discountAmount) {
}
