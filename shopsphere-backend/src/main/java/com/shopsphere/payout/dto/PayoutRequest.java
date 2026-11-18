package com.shopsphere.payout.dto;

import java.math.BigDecimal;
import com.shopsphere.payout.entity.PayoutMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PayoutRequest(
		@NotNull @DecimalMin("0.01") @Digits(integer=10,fraction=2) BigDecimal amount,
		@NotNull PayoutMethod method,
		@Size(max=120) String accountHolder,
		@Size(max=34) String accountNumber,
		@Size(max=11) String ifsc,
		@Size(max=256) String upiId) {}
