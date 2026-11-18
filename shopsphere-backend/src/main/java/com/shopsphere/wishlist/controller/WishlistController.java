package com.shopsphere.wishlist.controller;

import com.shopsphere.common.response.ApiResponse;
import com.shopsphere.security.CustomUserDetails;
import com.shopsphere.wishlist.dto.WishlistResponse;
import com.shopsphere.wishlist.service.WishlistService;

import jakarta.validation.constraints.Positive;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/wishlist")
@PreAuthorize("hasRole('CUSTOMER')")
public class WishlistController {

	private final WishlistService service;

	public WishlistController(WishlistService service) {
		this.service = service;
	}

	@PostMapping("/items/{productId}")
	public ApiResponse<WishlistResponse> addItem(
			@AuthenticationPrincipal CustomUserDetails customer,
			@PathVariable @Positive Long productId) {
		return ApiResponse.success(
				"Product added to wishlist.",
				service.addItem(customer.getId(), productId));
	}

	@GetMapping
	public ApiResponse<WishlistResponse> getWishlist(
			@AuthenticationPrincipal CustomUserDetails customer) {
		return ApiResponse.success("Wishlist retrieved.", service.getWishlist(customer.getId()));
	}

	@DeleteMapping("/items/{productId}")
	public ApiResponse<WishlistResponse> removeItem(
			@AuthenticationPrincipal CustomUserDetails customer,
			@PathVariable @Positive Long productId) {
		return ApiResponse.success(
				"Product removed from wishlist.",
				service.removeItem(customer.getId(), productId));
	}

	@DeleteMapping("/clear")
	public ApiResponse<WishlistResponse> clearWishlist(
			@AuthenticationPrincipal CustomUserDetails customer) {
		return ApiResponse.success("Wishlist cleared.", service.clearWishlist(customer.getId()));
	}
}