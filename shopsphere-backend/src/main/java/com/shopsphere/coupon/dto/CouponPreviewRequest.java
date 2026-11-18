package com.shopsphere.coupon.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CouponPreviewRequest(
		@NotBlank @Size(max = 40) String code) {
}
