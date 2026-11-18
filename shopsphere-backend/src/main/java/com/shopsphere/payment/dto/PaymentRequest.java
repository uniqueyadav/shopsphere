package com.shopsphere.payment.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record PaymentRequest(@NotNull @Positive Long orderId) {
}