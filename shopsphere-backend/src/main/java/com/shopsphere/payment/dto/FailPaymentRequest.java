package com.shopsphere.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record FailPaymentRequest(
		@NotBlank @Size(max = 100) String razorpayOrderId,
		@NotBlank @Pattern(regexp = "CANCELLED|FAILED") String reason) {
}
