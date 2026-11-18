package com.shopsphere.coupon.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;

import com.shopsphere.common.enums.CouponDiscountType;
import com.shopsphere.common.enums.CouponEligibility;
import com.shopsphere.common.enums.CouponScope;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record CouponRequest(
		@NotBlank @Size(max=40) @Pattern(regexp="^[A-Za-z0-9_-]+$") String code,
		@NotBlank @Size(max=120) String name,
		@Size(max=500) String description,
		@Size(max=500) String adminNote,
		@NotNull CouponDiscountType discountType,
		@NotNull @Positive @Digits(integer=10,fraction=2) BigDecimal discountValue,
		@NotNull @PositiveOrZero @Digits(integer=10,fraction=2) BigDecimal minimumOrderAmount,
		@Positive @Digits(integer=10,fraction=2) BigDecimal maximumDiscountAmount,
		@Positive Integer usageLimit,
		@NotNull @Positive Integer perCustomerLimit,
		@NotNull Instant startsAt,
		@NotNull Instant expiresAt,
		boolean active,
		@NotNull CouponEligibility eligibility,
		@NotNull CouponScope scope,
		Set<@NotNull @Positive Long> productIds,
		Set<@NotNull @Positive Long> categoryIds,
		Set<@NotNull @Positive Long> customerIds) { }
