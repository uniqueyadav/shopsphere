package com.shopsphere.seller.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

public record SellerDeliveryFeeRequest(
		@NotNull @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal deliveryFee,
		@jakarta.validation.constraints.DecimalMin("-90.0") @jakarta.validation.constraints.DecimalMax("90.0") Double pickupLatitude,
		@jakarta.validation.constraints.DecimalMin("-180.0") @jakarta.validation.constraints.DecimalMax("180.0") Double pickupLongitude) {
	public SellerDeliveryFeeRequest(BigDecimal deliveryFee) { this(deliveryFee, null, null); }
}
