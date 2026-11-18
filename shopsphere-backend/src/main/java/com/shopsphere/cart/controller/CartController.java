package com.shopsphere.cart.controller;

import com.shopsphere.cart.dto.AddToCartRequest;
import com.shopsphere.cart.dto.CartResponse;
import com.shopsphere.cart.dto.UpdateCartItemRequest;
import com.shopsphere.cart.service.CartService;
import com.shopsphere.common.response.ApiResponse;
import com.shopsphere.security.CustomUserDetails;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/cart")
@PreAuthorize("hasRole('CUSTOMER')")
public class CartController {

	private final CartService service;

	public CartController(CartService service) {
		this.service = service;
	}

	@PostMapping("/items")
	public ApiResponse<CartResponse> addItem(
			@AuthenticationPrincipal CustomUserDetails customer,
			@Valid @RequestBody AddToCartRequest request) {
		return ApiResponse.success("Product added to cart.", service.addItem(customer.getId(), request));
	}

	@GetMapping
	public ApiResponse<CartResponse> getCart(@AuthenticationPrincipal CustomUserDetails customer) {
		return ApiResponse.success("Cart retrieved.", service.getCart(customer.getId()));
	}

	@PutMapping("/items/{itemId}")
	public ApiResponse<CartResponse> updateItem(
			@AuthenticationPrincipal CustomUserDetails customer,
			@PathVariable @Positive Long itemId,
			@Valid @RequestBody UpdateCartItemRequest request) {
		return ApiResponse.success(
				"Cart item quantity updated.",
				service.updateItem(customer.getId(), itemId, request));
	}

	@DeleteMapping("/items/{itemId}")
	public ApiResponse<CartResponse> removeItem(
			@AuthenticationPrincipal CustomUserDetails customer,
			@PathVariable @Positive Long itemId) {
		return ApiResponse.success("Product removed from cart.", service.removeItem(customer.getId(), itemId));
	}

	@DeleteMapping("/clear")
	public ApiResponse<CartResponse> clearCart(@AuthenticationPrincipal CustomUserDetails customer) {
		return ApiResponse.success("Cart cleared.", service.clearCart(customer.getId()));
	}
}