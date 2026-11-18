package com.shopsphere.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record OrderCancellationRequest(
		@NotBlank(message = "A cancellation reason is required.")
		@Size(max = 500, message = "Cancellation reason must be 500 characters or fewer.")
		String reason) {
}
