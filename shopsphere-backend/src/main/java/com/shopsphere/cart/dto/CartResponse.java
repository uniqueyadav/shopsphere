package com.shopsphere.cart.dto;

import java.math.BigDecimal;
import java.util.List;

import com.shopsphere.cart.entity.Cart;

public record CartResponse(
		Long id,
		List<CartItemResponse> items,
		BigDecimal subtotal,
		int totalQuantity) {

	public static CartResponse from(Cart cart, List<CartItemResponse> items) {
		BigDecimal subtotal = items.stream()
				.map(CartItemResponse::lineSubtotal)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		int totalQuantity = items.stream()
				.mapToInt(CartItemResponse::quantity)
				.sum();
		return new CartResponse(cart.getId(), items, subtotal, totalQuantity);
	}
}