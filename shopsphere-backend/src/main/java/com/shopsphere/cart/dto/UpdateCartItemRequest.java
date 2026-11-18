package com.shopsphere.cart.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record UpdateCartItemRequest(
		@NotNull(message = "Quantity is required.")
		@Positive(message = "Quantity must be at least one.")
		Integer quantity) {
}
