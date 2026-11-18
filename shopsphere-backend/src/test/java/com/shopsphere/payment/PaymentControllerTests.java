package com.shopsphere.payment;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.UUID;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.jayway.jsonpath.JsonPath;
import com.shopsphere.common.enums.AddressType;
import com.shopsphere.common.enums.OrderStatus;
import com.shopsphere.common.enums.PaymentMethod;
import com.shopsphere.common.enums.PaymentStatus;
import com.shopsphere.common.enums.Role;
import com.shopsphere.common.enums.RefundStatus;
import com.shopsphere.order.entity.Order;
import com.shopsphere.order.repository.OrderRepository;
import com.shopsphere.payment.entity.Payment;
import com.shopsphere.payment.repository.PaymentRepository;
import com.shopsphere.payment.service.RazorpayGateway;
import com.shopsphere.security.CustomUserDetails;
import com.shopsphere.security.JwtService;
import com.shopsphere.user.ShopUserRepository;
import com.shopsphere.user.entity.Address;
import com.shopsphere.user.entity.User;
import com.shopsphere.user.repository.AddressRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PaymentControllerTests {

	private static final String CREATE_ORDER = "/api/v1/payments/create-order";
	private static final String VERIFY = "/api/v1/payments/verify";
	private static final String FAIL = "/api/v1/payments/fail";
	private static final String WEBHOOK = "/api/v1/payments/webhook";
	private static final String API_SECRET = "shopsphere-test-secret";
	private static final String WEBHOOK_SECRET = "shopsphere-test-webhook-secret";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ShopUserRepository users;

	@Autowired
	private AddressRepository addresses;

	@Autowired
	private OrderRepository orders;

	@Autowired
	private PaymentRepository payments;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private JwtService jwtService;

	@MockitoBean
	private RazorpayGateway razorpay;

	@BeforeEach
	void configureGateway() {
		reset(razorpay);
		when(razorpay.requireTestKeyId()).thenReturn("rzp_test_shopsphere_test_key");
		when(razorpay.createOrder(anyLong(), eq("INR"), anyString(), anyLong()))
				.thenAnswer(invocation -> new RazorpayGateway.GatewayOrder("order_test_" + UUID.randomUUID()));
	}

	@Test
	void createsServerPricedPaymentOrderAndVerifiesCapturedPaymentIdempotently() throws Exception {
		TestCustomer customer = createCustomer();
		Order order = createOnlineOrder(customer.user());
		String idempotencyKey = "payment-create-" + UUID.randomUUID();
		MvcResult created = createPaymentAttempt(customer, order, idempotencyKey);
		String razorpayOrderId = JsonPath.read(created.getResponse().getContentAsString(), "$.data.razorpayOrderId");
		MvcResult replay = createPaymentAttempt(customer, order, idempotencyKey);
		org.junit.jupiter.api.Assertions.assertEquals(
				razorpayOrderId,
				JsonPath.read(replay.getResponse().getContentAsString(), "$.data.razorpayOrderId"));
		org.junit.jupiter.api.Assertions.assertEquals(12500,
				JsonPath.<Number>read(created.getResponse().getContentAsString(), "$.data.amountPaise").longValue());
		when(razorpay.fetchPayment("pay_test_success"))
				.thenReturn(new RazorpayGateway.GatewayPayment(razorpayOrderId, "captured", 12500, "INR"));

		String verifyPayload = verifyPayload(razorpayOrderId, "pay_test_success", checkoutSignature(razorpayOrderId, "pay_test_success"));
		mockMvc.perform(post(VERIFY)
				.header("Authorization", bearer(customer))
				.contentType(MediaType.APPLICATION_JSON)
				.content(verifyPayload))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.paymentStatus").value(PaymentStatus.SUCCESS.name()))
				.andExpect(jsonPath("$.data.orderStatus").value(OrderStatus.PLACED.name()));

		Payment savedPayment = payments.findByRazorpayOrderId(razorpayOrderId).orElseThrow();
		org.junit.jupiter.api.Assertions.assertEquals(PaymentStatus.SUCCESS, savedPayment.getStatus());
		org.junit.jupiter.api.Assertions.assertEquals("pay_test_success", savedPayment.getRazorpayPaymentId());
		org.junit.jupiter.api.Assertions.assertEquals(
				PaymentStatus.SUCCESS,
				orders.findById(order.getId()).orElseThrow().getPaymentStatus());

		mockMvc.perform(post(VERIFY)
				.header("Authorization", bearer(customer))
				.contentType(MediaType.APPLICATION_JSON)
				.content(verifyPayload))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.paymentStatus").value(PaymentStatus.SUCCESS.name()));
	}

	@Test
	void rejectsInvalidSignatureAndDoesNotMarkOrderPaid() throws Exception {
		TestCustomer customer = createCustomer();
		Order order = createOnlineOrder(customer.user());
		MvcResult created = createPaymentAttempt(customer, order, "payment-create-" + UUID.randomUUID());
		String razorpayOrderId = JsonPath.read(created.getResponse().getContentAsString(), "$.data.razorpayOrderId");

		mockMvc.perform(post(VERIFY)
				.header("Authorization", bearer(customer))
				.contentType(MediaType.APPLICATION_JSON)
				.content(verifyPayload(razorpayOrderId, "pay_test_fake", "invalid-signature")))
				.andExpect(status().isBadRequest());

		org.junit.jupiter.api.Assertions.assertEquals(
				PaymentStatus.PENDING,
				orders.findById(order.getId()).orElseThrow().getPaymentStatus());
		org.junit.jupiter.api.Assertions.assertEquals(
				PaymentStatus.PENDING,
				payments.findByRazorpayOrderId(razorpayOrderId).orElseThrow().getStatus());
	}

	@Test
	void rejectsProviderPaymentForDifferentAmount() throws Exception {
		TestCustomer customer = createCustomer();
		Order order = createOnlineOrder(customer.user());
		MvcResult created = createPaymentAttempt(customer, order, "payment-create-" + UUID.randomUUID());
		String razorpayOrderId = JsonPath.read(created.getResponse().getContentAsString(), "$.data.razorpayOrderId");
		when(razorpay.fetchPayment("pay_test_wrong_amount"))
				.thenReturn(new RazorpayGateway.GatewayPayment(razorpayOrderId, "captured", 100, "INR"));

		mockMvc.perform(post(VERIFY)
				.header("Authorization", bearer(customer))
				.contentType(MediaType.APPLICATION_JSON)
				.content(verifyPayload(
						razorpayOrderId,
						"pay_test_wrong_amount",
						checkoutSignature(razorpayOrderId, "pay_test_wrong_amount"))))
				.andExpect(status().isConflict());
		org.junit.jupiter.api.Assertions.assertEquals(
				PaymentStatus.PENDING,
				orders.findById(order.getId()).orElseThrow().getPaymentStatus());
	}

	@Test
	void handlesCancelledAttemptAndAllowsSafeRetry() throws Exception {
		TestCustomer customer = createCustomer();
		Order order = createOnlineOrder(customer.user());
		String firstKey = "payment-create-" + UUID.randomUUID();
		MvcResult created = createPaymentAttempt(customer, order, firstKey);
		String firstRazorpayOrderId = JsonPath.read(created.getResponse().getContentAsString(), "$.data.razorpayOrderId");

		mockMvc.perform(post(FAIL)
				.header("Authorization", bearer(customer))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"razorpayOrderId":"%s","reason":"CANCELLED"}
						""".formatted(firstRazorpayOrderId)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.status").value(PaymentStatus.FAILED.name()));
		org.junit.jupiter.api.Assertions.assertEquals(
				PaymentStatus.FAILED,
				payments.findByRazorpayOrderId(firstRazorpayOrderId).orElseThrow().getStatus());
		org.junit.jupiter.api.Assertions.assertEquals(
				PaymentStatus.PENDING,
				orders.findById(order.getId()).orElseThrow().getPaymentStatus());

		MvcResult retry = createPaymentAttempt(customer, order, "payment-retry-" + UUID.randomUUID());
		String retryRazorpayOrderId = JsonPath.read(retry.getResponse().getContentAsString(), "$.data.razorpayOrderId");
		org.junit.jupiter.api.Assertions.assertNotEquals(firstRazorpayOrderId, retryRazorpayOrderId);
		org.junit.jupiter.api.Assertions.assertEquals(2, payments.countByOrder_Id(order.getId()));
	}

	@Test
	void verifiesWebhookSignatureAndIgnoresDuplicateCapturedEvents() throws Exception {
		TestCustomer customer = createCustomer();
		Order order = createOnlineOrder(customer.user());
		MvcResult created = createPaymentAttempt(customer, order, "payment-create-" + UUID.randomUUID());
		String razorpayOrderId = JsonPath.read(created.getResponse().getContentAsString(), "$.data.razorpayOrderId");
		String payload = """
				{"event":"payment.captured","payload":{"payment":{"entity":{
				  "id":"pay_test_webhook","order_id":"%s","status":"captured","amount":12500,"currency":"INR"
				}}}}
				""".formatted(razorpayOrderId);

		mockMvc.perform(post(WEBHOOK)
				.header("X-Razorpay-Signature", webhookSignature(payload))
				.contentType(MediaType.APPLICATION_JSON)
				.content(payload))
				.andExpect(status().isOk());
		mockMvc.perform(post(WEBHOOK)
				.header("X-Razorpay-Signature", webhookSignature(payload))
				.contentType(MediaType.APPLICATION_JSON)
				.content(payload))
				.andExpect(status().isOk());

		String partialRefund = """
				{"event":"refund.processed","payload":{"refund":{"entity":{
				  "id":"rfnd_test_partial","payment_id":"pay_test_webhook","amount":3000
				}}}}
				""";
		mockMvc.perform(post(WEBHOOK)
				.header("X-Razorpay-Signature", webhookSignature(partialRefund))
				.contentType(MediaType.APPLICATION_JSON)
				.content(partialRefund))
				.andExpect(status().isOk());
		org.junit.jupiter.api.Assertions.assertEquals(
				PaymentStatus.SUCCESS,
				payments.findByRazorpayOrderId(razorpayOrderId).orElseThrow().getStatus());

		String refund = """
				{"event":"refund.processed","payload":{"refund":{"entity":{
				  "id":"rfnd_test_full","payment_id":"pay_test_webhook","amount":12500
				}}}}
				""";
		mockMvc.perform(post(WEBHOOK)
				.header("X-Razorpay-Signature", webhookSignature(refund))
				.contentType(MediaType.APPLICATION_JSON)
				.content(refund))
				.andExpect(status().isOk());

		org.junit.jupiter.api.Assertions.assertEquals(
				PaymentStatus.REFUNDED,
				payments.findByRazorpayOrderId(razorpayOrderId).orElseThrow().getStatus());
		org.junit.jupiter.api.Assertions.assertEquals(
				PaymentStatus.REFUNDED,
				orders.findById(order.getId()).orElseThrow().getPaymentStatus());
	}

	@Test
	void recordsProviderFailureWebhookWithoutMarkingOrderPaid() throws Exception {
		TestCustomer customer = createCustomer();
		Order order = createOnlineOrder(customer.user());
		MvcResult created = createPaymentAttempt(customer, order, "payment-create-" + UUID.randomUUID());
		String razorpayOrderId = JsonPath.read(created.getResponse().getContentAsString(), "$.data.razorpayOrderId");
		String payload = """
				{"event":"payment.failed","payload":{"payment":{"entity":{
				  "id":"pay_test_failed","order_id":"%s","status":"failed"
				}}}}
				""".formatted(razorpayOrderId);

		mockMvc.perform(post(WEBHOOK)
				.header("X-Razorpay-Signature", webhookSignature(payload))
				.contentType(MediaType.APPLICATION_JSON)
				.content(payload))
				.andExpect(status().isOk());

		org.junit.jupiter.api.Assertions.assertEquals(
				PaymentStatus.FAILED,
				payments.findByRazorpayOrderId(razorpayOrderId).orElseThrow().getStatus());
		org.junit.jupiter.api.Assertions.assertEquals(
				PaymentStatus.PENDING,
				orders.findById(order.getId()).orElseThrow().getPaymentStatus());
	}

	@Test
	void clearsFailedRefundAttemptWithoutChangingSuccessfulPayment() throws Exception {
		TestCustomer customer = createCustomer();
		Order order = createOnlineOrder(customer.user());
		MvcResult created = createPaymentAttempt(customer, order, "payment-create-" + UUID.randomUUID());
		String razorpayOrderId = JsonPath.read(created.getResponse().getContentAsString(), "$.data.razorpayOrderId");
		String captured = """
				{"event":"payment.captured","payload":{"payment":{"entity":{
				  "id":"pay_test_refund_failure","order_id":"%s","status":"captured","amount":12500,"currency":"INR"
				}}}}
				""".formatted(razorpayOrderId);
		mockMvc.perform(post(WEBHOOK)
				.header("X-Razorpay-Signature", webhookSignature(captured))
				.contentType(MediaType.APPLICATION_JSON)
				.content(captured))
				.andExpect(status().isOk());

		Payment payment = payments.findByRazorpayOrderId(razorpayOrderId).orElseThrow();
		payment.markRefundInitiated("rfnd_test_to_fail");
		payments.saveAndFlush(payment);
		Order paidOrder = orders.findById(order.getId()).orElseThrow();
		paidOrder.markRefundPending();
		orders.saveAndFlush(paidOrder);
		String failedRefund = """
				{"event":"refund.failed","payload":{"refund":{"entity":{
				  "id":"rfnd_test_to_fail","payment_id":"pay_test_refund_failure","amount":12500
				}}}}
				""";
		mockMvc.perform(post(WEBHOOK)
				.header("X-Razorpay-Signature", webhookSignature(failedRefund))
				.contentType(MediaType.APPLICATION_JSON)
				.content(failedRefund))
				.andExpect(status().isOk());

		Payment updatedPayment = payments.findByRazorpayOrderId(razorpayOrderId).orElseThrow();
		org.junit.jupiter.api.Assertions.assertEquals(PaymentStatus.SUCCESS, updatedPayment.getStatus());
		org.junit.jupiter.api.Assertions.assertNull(updatedPayment.getRazorpayRefundId());
		org.junit.jupiter.api.Assertions.assertEquals(
				PaymentStatus.SUCCESS,
				orders.findById(order.getId()).orElseThrow().getPaymentStatus());
		org.junit.jupiter.api.Assertions.assertEquals(
				RefundStatus.FAILED,
				orders.findById(order.getId()).orElseThrow().getRefundStatus());
	}

	@Test
	void rejectsUnsignedWebhookAndCustomerOnlyPaymentAccess() throws Exception {
		TestCustomer customer = createCustomer();
		TestCustomer anotherCustomer = createCustomer();
		Order order = createOnlineOrder(customer.user());
		String payload = "{\"event\":\"payment.captured\"}";

		mockMvc.perform(post(WEBHOOK)
				.header("X-Razorpay-Signature", "invalid")
				.contentType(MediaType.APPLICATION_JSON)
				.content(payload))
				.andExpect(status().isBadRequest());
		mockMvc.perform(post(CREATE_ORDER)
				.header("Authorization", bearer(customer))
				.header("Idempotency-Key", "missing-order"))
				.andExpect(status().isBadRequest());
		mockMvc.perform(post(CREATE_ORDER)
				.header("Idempotency-Key", "unauthenticated")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"orderId\":" + order.getId() + "}"))
				.andExpect(status().isUnauthorized());
		MvcResult created = createPaymentAttempt(customer, order, "payment-create-" + UUID.randomUUID());
		String razorpayOrderId = JsonPath.read(created.getResponse().getContentAsString(), "$.data.razorpayOrderId");
		mockMvc.perform(post(VERIFY)
				.header("Authorization", bearer(anotherCustomer))
				.contentType(MediaType.APPLICATION_JSON)
				.content(verifyPayload(razorpayOrderId, "pay_test_cross_customer", "irrelevant")))
				.andExpect(status().isNotFound());
	}

	private MvcResult createPaymentAttempt(TestCustomer customer, Order order, String idempotencyKey) throws Exception {
		return mockMvc.perform(post(CREATE_ORDER)
				.header("Authorization", bearer(customer))
				.header("Idempotency-Key", idempotencyKey)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"orderId\":" + order.getId() + ",\"amount\":1}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.amountPaise").value(12500))
				.andExpect(jsonPath("$.data.keyId").value("rzp_test_shopsphere_test_key"))
				.andReturn();
	}

	private Order createOnlineOrder(User customer) {
		Address address = addresses.save(new Address(
				customer,
				"Payment Test Customer",
				"9876543210",
				"22A",
				"Test Market Road",
				null,
				"Jaipur",
				"Rajasthan",
				"302001",
				AddressType.HOME,
				true));
		return orders.saveAndFlush(new Order(
				customer,
				"checkout-" + UUID.randomUUID(),
				address.getId(),
				address,
				PaymentMethod.ONLINE,
				new java.math.BigDecimal("100.00"),
				new java.math.BigDecimal("25.00"),
				new java.math.BigDecimal("125.00")));
	}

	private TestCustomer createCustomer() {
		String suffix = UUID.randomUUID().toString();
		User user = new User(
				"Payment Test Customer",
				"payment-" + suffix + "@example.test",
				passwordEncoder.encode(suffix));
		user.setRole(Role.CUSTOMER);
		User saved = users.save(user);
		return new TestCustomer(saved, jwtService.generateAccessToken(new CustomUserDetails(saved)));
	}

	private String checkoutSignature(String razorpayOrderId, String razorpayPaymentId) throws Exception {
		return hmac(razorpayOrderId + "|" + razorpayPaymentId, API_SECRET);
	}

	private String webhookSignature(String payload) throws Exception {
		return hmac(payload, WEBHOOK_SECRET);
	}

	private String hmac(String payload, String secret) throws Exception {
		Mac mac = Mac.getInstance("HmacSHA256");
		mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
		return HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
	}

	private String verifyPayload(String orderId, String paymentId, String signature) {
		return """
				{"razorpayOrderId":"%s","razorpayPaymentId":"%s","razorpaySignature":"%s"}
				""".formatted(orderId, paymentId, signature);
	}

	private String bearer(TestCustomer customer) {
		return "Bearer " + customer.token();
	}

	private record TestCustomer(User user, String token) {
	}
}
