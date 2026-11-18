package com.shopsphere.payment.controller;

import com.shopsphere.common.response.ApiResponse;
import com.shopsphere.payment.service.PaymentService;

import jakarta.validation.constraints.NotBlank;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/payments")
public class PaymentWebhookController {

	private final PaymentService payments;

	public PaymentWebhookController(PaymentService payments) {
		this.payments = payments;
	}

	@PostMapping("/webhook")
	public ApiResponse<Void> webhook(
			@RequestBody String rawPayload,
			@RequestHeader("X-Razorpay-Signature") @NotBlank String signature) {
		payments.processWebhook(rawPayload, signature);
		return ApiResponse.success("Webhook processed.", null);
	}
}
