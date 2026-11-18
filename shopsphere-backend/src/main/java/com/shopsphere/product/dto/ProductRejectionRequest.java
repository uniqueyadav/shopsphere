package com.shopsphere.product.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProductRejectionRequest(
		@NotBlank(message = "A rejection reason is required.")
		@Size(max = 1000, message = "Rejection reason must be 1,000 characters or fewer.")
		String rejectionReason) {
}
