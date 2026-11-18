package com.shopsphere.coupon.dto;

import java.util.List;

public record CouponPageResponse(List<CouponResponse> items, int page, int size, long totalElements, int totalPages) { }
