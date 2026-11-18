package com.shopsphere.order.dto;

import com.shopsphere.common.enums.OrderStatus;

import jakarta.validation.constraints.NotNull;

public record OrderStatusRequest(@NotNull OrderStatus status) {
}