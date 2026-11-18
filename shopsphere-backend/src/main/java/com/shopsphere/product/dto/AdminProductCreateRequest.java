package com.shopsphere.product.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AdminProductCreateRequest(
		@NotNull @Positive Long sellerId,
		@NotNull @Valid ProductRequest product) {
}
