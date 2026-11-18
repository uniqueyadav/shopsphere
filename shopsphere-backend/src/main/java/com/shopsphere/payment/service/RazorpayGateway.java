package com.shopsphere.payment.service;

import org.json.JSONObject;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.shopsphere.payment.config.RazorpayProperties;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class RazorpayGateway {

	private final RazorpayProperties properties;

	public RazorpayGateway(RazorpayProperties properties) {
		this.properties = properties;
	}

	public String requireTestKeyId() {
		String keyId = properties.getKeyId();
		if (keyId == null || !keyId.startsWith("rzp_test_") || blank(properties.getKeySecret())) {
			throw new ResponseStatusException(
					HttpStatus.SERVICE_UNAVAILABLE,
					"Razorpay test credentials are not configured.");
		}
		return keyId;
	}

	public GatewayOrder createOrder(long amountPaise, String currency, String receipt, Long shopOrderId) {
		requireTestKeyId();
		JSONObject options = new JSONObject()
				.put("amount", amountPaise)
				.put("currency", currency)
				.put("receipt", receipt)
				.put("notes", new JSONObject().put("shopsphere_order_id", shopOrderId.toString()));
		try {
			com.razorpay.Order order = client().orders.create(options);
			return new GatewayOrder(order.get("id").toString());
		} catch (RazorpayException | org.json.JSONException exception) {
			throw providerUnavailable(exception);
		}
	}

	public GatewayPayment fetchPayment(String paymentId) {
		requireTestKeyId();
		try {
			com.razorpay.Payment payment = client().payments.fetch(paymentId);
			return new GatewayPayment(
					payment.get("order_id").toString(),
					payment.get("status").toString(),
					Long.parseLong(payment.get("amount").toString()),
					payment.get("currency").toString());
		} catch (RazorpayException | org.json.JSONException exception) {
			throw providerUnavailable(exception);
		}
	}

	public GatewayRefund refundPayment(String paymentId, long amountPaise, String orderReference) {
		requireTestKeyId();
		JSONObject options = new JSONObject()
				.put("amount", amountPaise)
				.put("notes", new JSONObject().put("shopsphere_order_reference", orderReference));
		try {
			com.razorpay.Refund refund = client().payments.refund(paymentId, options);
			return new GatewayRefund(
					refund.get("id").toString(),
					refund.get("status").toString());
		} catch (RazorpayException | org.json.JSONException exception) {
			throw providerUnavailable(exception);
		}
	}

	private RazorpayClient client() throws RazorpayException {
		return new RazorpayClient(properties.getKeyId(), properties.getKeySecret());
	}

	private ResponseStatusException providerUnavailable(Exception cause) {
		return new ResponseStatusException(
				HttpStatus.BAD_GATEWAY,
				"Razorpay could not process the payment request.",
				cause);
	}

	private boolean blank(String value) {
		return value == null || value.isBlank();
	}

	public record GatewayOrder(String id) {
	}

	public record GatewayPayment(String orderId, String status, long amountPaise, String currency) {
	}

	public record GatewayRefund(String id, String status) {
	}
}
