package com.shopsphere.order.dto;

public record CheckoutResult(OrderResponse order, boolean replayed) {
}
