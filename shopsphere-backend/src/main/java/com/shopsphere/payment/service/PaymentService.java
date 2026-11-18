package com.shopsphere.payment.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopsphere.common.enums.PaymentMethod;
import com.shopsphere.common.enums.PaymentStatus;
import com.shopsphere.common.enums.OrderStatus;
import com.shopsphere.order.dto.OrderResponse;
import com.shopsphere.order.entity.Order;
import com.shopsphere.order.mapper.OrderMapper;
import com.shopsphere.order.repository.OrderRepository;
import com.shopsphere.payment.dto.FailPaymentRequest;
import com.shopsphere.payment.dto.PaymentRequest;
import com.shopsphere.payment.dto.PaymentResponse;
import com.shopsphere.payment.dto.VerifyPaymentRequest;
import com.shopsphere.payment.entity.Payment;
import com.shopsphere.payment.repository.PaymentRepository;
import com.shopsphere.user.entity.User;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PaymentService {

	private static final String CURRENCY = "INR";
	private static final BigDecimal MAX_PAYMENT_AMOUNT = new BigDecimal("9999999999.99");

	private final PaymentRepository payments;
	private final OrderRepository orders;
	private final RazorpayGateway razorpay;
	private final PaymentVerificationService verification;
	private final ObjectMapper objectMapper;

	public PaymentService(
			PaymentRepository payments,
			OrderRepository orders,
			RazorpayGateway razorpay,
			PaymentVerificationService verification,
			ObjectMapper objectMapper) {
		this.payments = payments;
		this.orders = orders;
		this.razorpay = razorpay;
		this.verification = verification;
		this.objectMapper = objectMapper;
	}

	@Transactional
	public PaymentResponse createOrder(Long customerId, String requestKey, PaymentRequest request) {
		String idempotencyKey = requestKey.trim();
		Order order = orders.findByIdAndCustomer_IdForUpdate(request.orderId(), customerId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order was not found."));
		if (order.getPaymentMethod() != PaymentMethod.ONLINE) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "This order is not configured for online payment.");
		}

		Payment previous = payments.findByOrder_IdAndIdempotencyKey(order.getId(), idempotencyKey).orElse(null);
		if (previous != null) {
			return response(previous);
		}
		if (order.getPaymentStatus() != PaymentStatus.PENDING) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "This order no longer requires payment.");
		}
		Payment activeAttempt = payments
				.findFirstByOrder_IdAndStatusOrderByCreatedAtDesc(order.getId(), PaymentStatus.PENDING)
				.orElse(null);
		if (activeAttempt != null) {
			return response(activeAttempt);
		}

		BigDecimal amount = order.getTotalAmount();
		if (amount == null || amount.signum() <= 0 || amount.compareTo(MAX_PAYMENT_AMOUNT) > 0) {
			throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Order amount is not supported for payment.");
		}
		long amountPaise;
		try {
			amountPaise = amount.setScale(2, RoundingMode.UNNECESSARY).movePointRight(2).longValueExact();
		} catch (ArithmeticException exception) {
			throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Order amount is not a valid INR amount.");
		}

		String keyId = razorpay.requireTestKeyId();
		RazorpayGateway.GatewayOrder gatewayOrder = razorpay.createOrder(
				amountPaise,
				CURRENCY,
				"ss-" + UUID.randomUUID(),
				order.getId());
		User customer = order.getCustomer();
		Payment payment = new Payment(
				order,
				customer,
				idempotencyKey,
				gatewayOrder.id(),
				keyId,
				amount,
				CURRENCY);
		return response(payments.save(payment));
	}

	@Transactional
	public OrderResponse verifyPayment(Long customerId, VerifyPaymentRequest request) {
		Payment payment = payments.findByRazorpayOrderId(request.razorpayOrderId())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment attempt was not found."));
		if (!payment.getCustomer().getId().equals(customerId)) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment attempt was not found.");
		}
		verification.verifyCheckoutSignature(
				request.razorpayOrderId(),
				request.razorpayPaymentId(),
				request.razorpaySignature());
		Order order = orders.findByIdAndCustomer_IdForUpdate(payment.getOrder().getId(), customerId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order was not found."));
		payment = payments.findByRazorpayOrderIdForUpdate(request.razorpayOrderId())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment attempt was not found."));
		if (payment.getStatus() == PaymentStatus.SUCCESS
				&& request.razorpayPaymentId().equals(payment.getRazorpayPaymentId())) {
			return OrderMapper.toResponse(order);
		}
		if (payment.getStatus() == PaymentStatus.REFUNDED
				|| order.getOrderStatus() == OrderStatus.CANCELLED
				|| order.getPaymentStatus() == PaymentStatus.SUCCESS) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "This order has already been paid.");
		}

		RazorpayGateway.GatewayPayment gatewayPayment = razorpay.fetchPayment(request.razorpayPaymentId());
		if (!request.razorpayOrderId().equals(gatewayPayment.orderId())
				|| !"captured".equalsIgnoreCase(gatewayPayment.status())
				|| gatewayPayment.amountPaise() != payment.getAmount().movePointRight(2).longValueExact()
				|| !payment.getCurrency().equals(gatewayPayment.currency())) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Razorpay has not confirmed a captured payment.");
		}

		try {
			payment.markSuccessful(request.razorpayPaymentId());
			order.markPaymentSuccessful();
		} catch (IllegalStateException exception) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Payment could not be applied to this order.");
		}
		payments.save(payment);
		return OrderMapper.toResponse(order);
	}

	@Transactional
	public PaymentResponse markAttemptFailed(Long customerId, FailPaymentRequest request) {
		Payment payment = payments.findByRazorpayOrderIdForUpdate(request.razorpayOrderId())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment attempt was not found."));
		if (!payment.getCustomer().getId().equals(customerId)) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment attempt was not found.");
		}
		payment.markFailed(null, request.reason());
		return response(payment);
	}

	@Transactional
	public void processWebhook(String rawPayload, String signature) {
		verification.verifyWebhookSignature(rawPayload, signature);
		JsonNode root;
		try {
			root = objectMapper.readTree(rawPayload);
		} catch (JsonProcessingException exception) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Razorpay webhook payload is invalid.");
		}

		String event = root.path("event").asText("");
		switch (event) {
			case "payment.captured" -> processCaptured(root.path("payload").path("payment").path("entity"));
			case "payment.failed" -> processFailed(root.path("payload").path("payment").path("entity"));
			case "refund.processed" -> processRefunded(root.path("payload").path("refund").path("entity"));
			case "refund.failed" -> processRefundFailed(root.path("payload").path("refund").path("entity"));
			default -> {
				// Unused but validly signed events need no state change.
			}
		}
	}

	private void processCaptured(JsonNode paymentEntity) {
		String razorpayOrderId = requiredEventValue(paymentEntity, "order_id");
		String paymentId = requiredEventValue(paymentEntity, "id");
		Payment payment = payments.findByRazorpayOrderId(razorpayOrderId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment attempt was not found."));
		Order order = orders.findByIdAndCustomer_IdForUpdate(payment.getOrder().getId(), payment.getCustomer().getId())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order was not found."));
		payment = payments.findByRazorpayOrderIdForUpdate(razorpayOrderId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment attempt was not found."));
		if (!matchesOrderAmount(paymentEntity, payment)) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Captured payment amount does not match the order.");
		}
		if (payment.getStatus() == PaymentStatus.REFUNDED) {
			return;
		}
		if (payment.getStatus() == PaymentStatus.SUCCESS && paymentId.equals(payment.getRazorpayPaymentId())) {
			return;
		}
		try {
			payment.markSuccessful(paymentId);
			order.markPaymentSuccessful();
			payments.save(payment);
		} catch (IllegalStateException exception) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Captured payment could not be applied to this order.");
		}
	}

	private void processFailed(JsonNode paymentEntity) {
		String razorpayOrderId = requiredEventValue(paymentEntity, "order_id");
		String paymentId = requiredEventValue(paymentEntity, "id");
		Payment payment = payments.findByRazorpayOrderIdForUpdate(razorpayOrderId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment attempt was not found."));
		payment.markFailed(paymentId, "FAILED");
	}

	private void processRefunded(JsonNode refundEntity) {
		String paymentId = requiredEventValue(refundEntity, "payment_id");
		String refundId = requiredEventValue(refundEntity, "id");
		Payment payment = payments.findByRazorpayPaymentId(paymentId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment transaction was not found."));
		if (!refundEntity.hasNonNull("amount")
				|| refundEntity.path("amount").asLong(-1) != payment.getAmount().movePointRight(2).longValueExact()) {
			return;
		}
		Order order = orders.findByIdAndCustomer_IdForUpdate(payment.getOrder().getId(), payment.getCustomer().getId())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order was not found."));
		payment = payments.findByRazorpayPaymentIdForUpdate(paymentId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment transaction was not found."));
		if (payment.getStatus() == PaymentStatus.REFUNDED) {
			return;
		}
		if (payment.getRazorpayRefundId() != null && !payment.getRazorpayRefundId().equals(refundId)) {
			return;
		}
		try {
			payment.markRefunded(paymentId, refundId);
			order.markPaymentRefunded();
			payments.save(payment);
		} catch (IllegalStateException exception) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Refund could not be applied to this order.");
		}
	}

	private void processRefundFailed(JsonNode refundEntity) {
		String paymentId = requiredEventValue(refundEntity, "payment_id");
		String refundId = requiredEventValue(refundEntity, "id");
		Payment payment = payments.findByRazorpayPaymentId(paymentId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment transaction was not found."));
		Order order = orders.findByIdAndCustomer_IdForUpdate(payment.getOrder().getId(), payment.getCustomer().getId())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order was not found."));
		payment = payments.findByRazorpayPaymentIdForUpdate(paymentId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment transaction was not found."));
		if (refundId.equals(payment.getRazorpayRefundId())) {
			payment.clearFailedRefund(refundId);
			order.markRefundFailed();
		}
	}

	private String requiredEventValue(JsonNode entity, String field) {
		String value = entity.path(field).asText("");
		if (value.isBlank()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Razorpay event is missing required payment data.");
		}
		return value;
	}

	private boolean matchesOrderAmount(JsonNode paymentEntity, Payment payment) {
		return paymentEntity.hasNonNull("amount")
				&& paymentEntity.path("amount").asLong(-1) == payment.getAmount().movePointRight(2).longValueExact()
				&& payment.getCurrency().equals(paymentEntity.path("currency").asText(""));
	}

	private PaymentResponse response(Payment payment) {
		long amountPaise = payment.getAmount().movePointRight(2).longValueExact();
		return new PaymentResponse(
				payment.getId(),
				payment.getOrder().getId(),
				payment.getRazorpayOrderId(),
				amountPaise,
				payment.getCurrency(),
				payment.getRazorpayKeyId(),
				payment.getStatus());
	}
}