package com.shopsphere.payment.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.shopsphere.payment.config.RazorpayProperties;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PaymentVerificationService {

	private final RazorpayProperties properties;

	public PaymentVerificationService(RazorpayProperties properties) {
		this.properties = properties;
	}

	public void verifyCheckoutSignature(String razorpayOrderId, String razorpayPaymentId, String signature) {
		String secret = requiredSecret(properties.getKeySecret(), "Razorpay API secret is not configured.");
		String payload = razorpayOrderId + "|" + razorpayPaymentId;
		if (!matchesHmac(payload, signature, secret)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Razorpay payment signature is invalid.");
		}
	}

	public void verifyWebhookSignature(String payload, String signature) {
		String secret = requiredSecret(properties.getWebhookSecret(), "Razorpay webhook secret is not configured.");
		if (!matchesHmac(payload, signature, secret)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Razorpay webhook signature is invalid.");
		}
	}

	private String requiredSecret(String secret, String message) {
		if (secret == null || secret.isBlank()) {
			throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, message);
		}
		return secret;
	}

	private boolean matchesHmac(String payload, String signature, String secret) {
		if (signature == null || signature.isBlank()) {
			return false;
		}
		try {
			Mac mac = Mac.getInstance("HmacSHA256");
			mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
			byte[] expected = HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)))
					.getBytes(StandardCharsets.US_ASCII);
			byte[] supplied = signature.toLowerCase(java.util.Locale.ROOT).getBytes(StandardCharsets.US_ASCII);
			return MessageDigest.isEqual(expected, supplied);
		} catch (java.security.GeneralSecurityException exception) {
			throw new IllegalStateException("HMAC-SHA256 is unavailable.", exception);
		}
	}
}