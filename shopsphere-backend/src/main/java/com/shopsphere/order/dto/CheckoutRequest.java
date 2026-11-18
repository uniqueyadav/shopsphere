package com.shopsphere.order.dto;

import com.shopsphere.common.enums.PaymentMethod;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CheckoutRequest(
		@NotNull @Positive Long addressId,
		@NotNull PaymentMethod paymentMethod,
		@Size(max=40) @Pattern(regexp="^[A-Za-z0-9_-]+$") String couponCode) {
	public CheckoutRequest(Long addressId, PaymentMethod paymentMethod) { this(addressId, paymentMethod, null); }
}
