package com.shopsphere.payment.controller;

import com.shopsphere.common.response.ApiResponse;
import com.shopsphere.order.dto.OrderResponse;
import com.shopsphere.payment.dto.FailPaymentRequest;
import com.shopsphere.payment.dto.PaymentRequest;
import com.shopsphere.payment.dto.PaymentResponse;
import com.shopsphere.payment.dto.VerifyPaymentRequest;
import com.shopsphere.payment.service.PaymentService;
import com.shopsphere.security.CustomUserDetails;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

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
@RequestMapping("/api/v1/payments")
@PreAuthorize("hasRole('CUSTOMER')")
public class PaymentController {

	private final PaymentService payments;

	public PaymentController(PaymentService payments) {
		this.payments = payments;
	}

	@PostMapping("/create-order")
	public ApiResponse<PaymentResponse> createOrder(
			@AuthenticationPrincipal CustomUserDetails customer,
			@RequestHeader("Idempotency-Key") @NotBlank @Size(max = 100) String idempotencyKey,
			@Valid @RequestBody PaymentRequest request) {
		return ApiResponse.success(
				"Razorpay payment order created.",
				payments.createOrder(customer.getId(), idempotencyKey, request));
	}

	@PostMapping("/verify")
	public ApiResponse<OrderResponse> verify(
			@AuthenticationPrincipal CustomUserDetails customer,
			@Valid @RequestBody VerifyPaymentRequest request) {
		return ApiResponse.success(
				"Payment verified successfully.",
				payments.verifyPayment(customer.getId(), request));
	}

	@PostMapping("/fail")
	public ApiResponse<PaymentResponse> fail(
			@AuthenticationPrincipal CustomUserDetails customer,
			@Valid @RequestBody FailPaymentRequest request) {
		return ApiResponse.success(
				"Payment attempt closed.",
				payments.markAttemptFailed(customer.getId(), request));
	}
}