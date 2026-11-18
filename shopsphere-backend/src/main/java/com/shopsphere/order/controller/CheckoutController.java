package com.shopsphere.order.controller;

import com.shopsphere.common.response.ApiResponse;
import com.shopsphere.order.dto.CheckoutRequest;
import com.shopsphere.order.dto.CheckoutResult;
import com.shopsphere.order.dto.OrderResponse;
import com.shopsphere.order.service.CheckoutService;
import com.shopsphere.security.CustomUserDetails;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/orders")
@PreAuthorize("hasRole('CUSTOMER')")
public class CheckoutController {

	private final CheckoutService checkoutService;

	public CheckoutController(CheckoutService checkoutService) {
		this.checkoutService = checkoutService;
	}

	@PostMapping("/checkout")
	public ResponseEntity<ApiResponse<OrderResponse>> checkout(
			@AuthenticationPrincipal CustomUserDetails customer,
			@RequestHeader("Idempotency-Key") @NotBlank @Size(max = 100) String idempotencyKey,
			@Valid @RequestBody CheckoutRequest request) {
		CheckoutResult result = checkoutService.checkout(customer.getId(), idempotencyKey, request);
		HttpStatus status = result.replayed() ? HttpStatus.OK : HttpStatus.CREATED;
		String message = result.replayed() ? "Existing order returned." : "Order placed successfully.";
		return ResponseEntity.status(status).body(ApiResponse.success(message, result.order()));
	}
}