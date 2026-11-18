package com.shopsphere.payout.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PayoutStatusRequest(@NotBlank @Size(max=30) String action,
		@Size(max=120) String transferReference, @Size(max=500) String remarks) {}
