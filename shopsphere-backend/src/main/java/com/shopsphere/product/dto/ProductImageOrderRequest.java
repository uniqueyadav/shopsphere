package com.shopsphere.product.dto;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ProductImageOrderRequest(
		@NotEmpty(message = "Image order must include every product image.")
		List<@NotNull @Positive Long> imageIds) {
}
