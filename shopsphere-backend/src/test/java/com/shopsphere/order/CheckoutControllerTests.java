package com.shopsphere.order;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.jayway.jsonpath.JsonPath;
import com.shopsphere.cart.entity.Cart;
import com.shopsphere.cart.repository.CartRepository;
import com.shopsphere.category.entity.Category;
import com.shopsphere.category.repository.CategoryRepository;
import com.shopsphere.common.enums.AddressType;
import com.shopsphere.common.enums.OrderStatus;
import com.shopsphere.common.enums.PaymentMethod;
import com.shopsphere.common.enums.PaymentStatus;
import com.shopsphere.common.enums.Role;
import com.shopsphere.common.enums.SellerStatus;
import com.shopsphere.order.entity.Order;
import com.shopsphere.order.entity.SellerOrder;
import com.shopsphere.order.repository.OrderRepository;
import com.shopsphere.order.repository.SellerOrderRepository;
import com.shopsphere.payment.entity.Payment;
import com.shopsphere.payment.repository.PaymentRepository;
import com.shopsphere.payment.service.RazorpayGateway;
import com.shopsphere.product.entity.Product;
import com.shopsphere.product.repository.ProductRepository;
import com.shopsphere.security.CustomUserDetails;
import com.shopsphere.security.JwtService;
import com.shopsphere.seller.entity.Seller;
import com.shopsphere.seller.repository.SellerRepository;
import com.shopsphere.user.ShopUserRepository;
import com.shopsphere.user.entity.Address;
import com.shopsphere.user.entity.User;
import com.shopsphere.user.repository.AddressRepository;

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
class CheckoutControllerTests {

	private static final String CHECKOUT = "/api/v1/orders/checkout";
	private static final String MY_ORDERS = "/api/v1/orders/my-orders";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ShopUserRepository users;

	@Autowired
	private SellerRepository sellers;

	@Autowired
	private CategoryRepository categories;

	@Autowired
	private ProductRepository products;

	@Autowired
	private CartRepository carts;

	@Autowired
	private AddressRepository addresses;

	@Autowired
	private OrderRepository orders;

	@Autowired
	private SellerOrderRepository sellerOrders;

	@Autowired
	private PaymentRepository payments;

	@MockitoBean
	private RazorpayGateway razorpay;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private JwtService jwtService;

	@Test
	void checksOutProductsFromMultipleSellersAndHonorsIdempotencyAndOwnership() throws Exception {
		TestUser customer = createUser(Role.CUSTOMER);
		TestUser otherCustomer = createUser(Role.CUSTOMER);
		TestUser admin = createUser(Role.SUPER_ADMIN);
		Address shipping = createAddress(customer.user(), "Primary recipient");
		Address privateAddress = createAddress(otherCustomer.user(), "Other recipient");
		SellerProduct first = createSellerProduct(admin.user(), "Ceramics", "105.50", "80.25", 5, "12.50");
		SellerProduct second = createSellerProduct(admin.user(), "Textiles", "50.00", null, 3, "7.75");
		addToCart(customer.user(), first.product(), 2, second.product(), 1);
		String key = UUID.randomUUID().toString();

		mockMvc.perform(get("/api/v1/cart")
				.header("Authorization", bearer(customer)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.items[0].sellerId").exists())
				.andExpect(jsonPath("$.data.items[0].sellerDeliveryFee").exists());
		mockMvc.perform(post(CHECKOUT)
				.header("Authorization", bearer(customer))
				.header("Idempotency-Key", UUID.randomUUID().toString())
				.contentType(MediaType.APPLICATION_JSON)
				.content(checkoutPayload(privateAddress.getId(), PaymentMethod.COD)))
				.andExpect(status().isNotFound());

		MvcResult result = mockMvc.perform(post(CHECKOUT)
				.header("Authorization", bearer(customer))
				.header("Idempotency-Key", key)
				.contentType(MediaType.APPLICATION_JSON)
				.content(checkoutPayload(shipping.getId(), PaymentMethod.COD)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.orderStatus").value(OrderStatus.PLACED.name()))
				.andExpect(jsonPath("$.data.paymentMethod").value(PaymentMethod.COD.name()))
				.andExpect(jsonPath("$.data.paymentStatus").value(PaymentStatus.PENDING.name()))
				.andExpect(jsonPath("$.data.subtotal").value(210.50))
				.andExpect(jsonPath("$.data.deliveryCharges").value(20.25))
				.andExpect(jsonPath("$.data.totalAmount").value(230.75))
				.andExpect(jsonPath("$.data.shippingAddress.fullName").value("Primary recipient"))
				.andExpect(jsonPath("$.data.shippingAddress.pinCode").value("302001"))
				.andExpect(jsonPath("$.data.sellerOrders.length()").value(2))
				.andExpect(jsonPath("$.data.sellerOrders[0].subtotal").value(160.50))
				.andExpect(jsonPath("$.data.sellerOrders[0].deliveryCharge").value(12.50))
				.andExpect(jsonPath("$.data.sellerOrders[0].items[0].unitPrice").value(80.25))
				.andExpect(jsonPath("$.data.sellerOrders[0].items[0].quantity").value(2))
				.andExpect(jsonPath("$.data.sellerOrders[1].subtotal").value(50))
				.andExpect(jsonPath("$.data.sellerOrders[1].deliveryCharge").value(7.75))
				.andExpect(jsonPath("$.data.sellerOrders[0].items.length()").value(1))
				.andExpect(jsonPath("$.data.sellerOrders[1].items.length()").value(1))
				.andReturn();

		Number orderIdValue = JsonPath.read(result.getResponse().getContentAsString(), "$.data.id");
		Long orderId = orderIdValue.longValue();
		Address differentAddress = createAddress(customer.user(), "Alternate recipient");
		Order savedOrder = orders.findByIdAndCustomer_Id(orderId, customer.user().getId()).orElseThrow();
		org.junit.jupiter.api.Assertions.assertEquals(key, savedOrder.getIdempotencyKey());
		org.junit.jupiter.api.Assertions.assertEquals(2, savedOrder.getSellerOrders().size());
		org.junit.jupiter.api.Assertions.assertEquals(3, products.findById(first.product().getId()).orElseThrow().getStockQuantity());
		org.junit.jupiter.api.Assertions.assertEquals(2, products.findById(second.product().getId()).orElseThrow().getStockQuantity());
		org.junit.jupiter.api.Assertions.assertTrue(carts.findByCustomer_Id(customer.user().getId()).orElseThrow().getItems().isEmpty());

		mockMvc.perform(post(CHECKOUT)
				.header("Authorization", bearer(customer))
				.header("Idempotency-Key", key)
				.contentType(MediaType.APPLICATION_JSON)
				.content(checkoutPayload(shipping.getId(), PaymentMethod.COD)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.id").value(orderId));
		org.junit.jupiter.api.Assertions.assertEquals(3, products.findById(first.product().getId()).orElseThrow().getStockQuantity());
		mockMvc.perform(post(CHECKOUT)
				.header("Authorization", bearer(customer))
				.header("Idempotency-Key", key)
				.contentType(MediaType.APPLICATION_JSON)
				.content(checkoutPayload(differentAddress.getId(), PaymentMethod.COD)))
				.andExpect(status().isConflict());

		mockMvc.perform(get(MY_ORDERS)
				.header("Authorization", bearer(customer)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.length()").value(1))
				.andExpect(jsonPath("$.data[0].id").value(orderId));
		mockMvc.perform(get("/api/v1/orders/" + orderId)
				.header("Authorization", bearer(customer)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.id").value(orderId));
		mockMvc.perform(get("/api/v1/orders/" + orderId)
				.header("Authorization", bearer(otherCustomer)))
				.andExpect(status().isNotFound());
		mockMvc.perform(get(MY_ORDERS)
				.header("Authorization", bearer(otherCustomer)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.length()").value(0));
		mockMvc.perform(get(MY_ORDERS))
				.andExpect(status().isUnauthorized());
		mockMvc.perform(get(MY_ORDERS)
				.header("Authorization", bearer(first.sellerUser())))
				.andExpect(status().isForbidden());
	}

	@Test
	void rejectsInsufficientStockWithoutCreatingOrderOrClearingCart() throws Exception {
		TestUser customer = createUser(Role.CUSTOMER);
		TestUser admin = createUser(Role.SUPER_ADMIN);
		Address shipping = createAddress(customer.user(), "Stock recipient");
		SellerProduct product = createSellerProduct(admin.user(), "Limited item", "25.00", null, 1, "0.00");
		addToCart(customer.user(), product.product(), 2, null, 0);

		mockMvc.perform(post(CHECKOUT)
				.header("Authorization", bearer(customer))
				.header("Idempotency-Key", UUID.randomUUID().toString())
				.contentType(MediaType.APPLICATION_JSON)
				.content(checkoutPayload(shipping.getId(), PaymentMethod.COD)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.success").value(false));

		org.junit.jupiter.api.Assertions.assertEquals(1, products.findById(product.product().getId()).orElseThrow().getStockQuantity());
		org.junit.jupiter.api.Assertions.assertTrue(orders.findByCustomer_IdOrderByCreatedAtDescIdDesc(customer.user().getId()).isEmpty());
		org.junit.jupiter.api.Assertions.assertEquals(
				1,
				carts.findByCustomer_Id(customer.user().getId()).orElseThrow().getItems().size());
	}

	@Test
	void rejectsUnapprovedProductsWithoutCreatingOrderOrClearingCart() throws Exception {
		TestUser customer = createUser(Role.CUSTOMER);
		TestUser admin = createUser(Role.SUPER_ADMIN);
		Address shipping = createAddress(customer.user(), "Unapproved recipient");
		SellerProduct product = createSellerProduct(admin.user(), "Unapproved item", "25.00", null, 2, "0.00");
		product.product().reject(admin.user(), Instant.now(), "Approval pending.");
		products.save(product.product());
		addToCart(customer.user(), product.product(), 1, null, 0);

		mockMvc.perform(post(CHECKOUT)
				.header("Authorization", bearer(customer))
				.header("Idempotency-Key", UUID.randomUUID().toString())
				.contentType(MediaType.APPLICATION_JSON)
				.content(checkoutPayload(shipping.getId(), PaymentMethod.COD)))
				.andExpect(status().isConflict());

		org.junit.jupiter.api.Assertions.assertEquals(2, products.findById(product.product().getId()).orElseThrow().getStockQuantity());
		org.junit.jupiter.api.Assertions.assertTrue(orders.findByCustomer_IdOrderByCreatedAtDescIdDesc(customer.user().getId()).isEmpty());
		org.junit.jupiter.api.Assertions.assertEquals(
				1,
				carts.findByCustomer_Id(customer.user().getId()).orElseThrow().getItems().size());
	}

	@Test
	void onlineCheckoutCreatesPaymentPendingOrderAndReservesStock() throws Exception {
		TestUser customer = createUser(Role.CUSTOMER);
		TestUser admin = createUser(Role.SUPER_ADMIN);
		Address shipping = createAddress(customer.user(), "Online payment recipient");
		SellerProduct product = createSellerProduct(admin.user(), "Online payment item", "40.00", null, 4, "0.00");
		addToCart(customer.user(), product.product(), 2, null, 0);

		MvcResult result = mockMvc.perform(post(CHECKOUT)
				.header("Authorization", bearer(customer))
				.header("Idempotency-Key", UUID.randomUUID().toString())
				.contentType(MediaType.APPLICATION_JSON)
				.content(checkoutPayload(shipping.getId(), PaymentMethod.ONLINE)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.data.paymentMethod").value(PaymentMethod.ONLINE.name()))
				.andExpect(jsonPath("$.data.paymentStatus").value(PaymentStatus.PENDING.name()))
				.andExpect(jsonPath("$.data.orderStatus").value(OrderStatus.PAYMENT_PENDING.name()))
				.andExpect(jsonPath("$.data.sellerOrders[0].orderStatus").value(OrderStatus.PAYMENT_PENDING.name()))
				.andReturn();

		Number orderId = JsonPath.read(result.getResponse().getContentAsString(), "$.data.id");
		org.junit.jupiter.api.Assertions.assertEquals(
				2,
				products.findById(product.product().getId()).orElseThrow().getStockQuantity());
		org.junit.jupiter.api.Assertions.assertTrue(carts.findByCustomer_Id(customer.user().getId())
				.orElseThrow().getItems().isEmpty());
		org.junit.jupiter.api.Assertions.assertEquals(
				PaymentStatus.PENDING,
				orders.findById(orderId.longValue()).orElseThrow().getPaymentStatus());
	}

	@Test
	void tracksSellerStatusHistoryAndPreventsSellerOrderAccessAcrossStores() throws Exception {
		TestUser customer = createUser(Role.CUSTOMER);
		TestUser admin = createUser(Role.SUPER_ADMIN);
		Address shipping = createAddress(customer.user(), "Tracking recipient");
		SellerProduct product = createSellerProduct(admin.user(), "Trackable item", "40.00", null, 4, "0.00");
		SellerProduct unrelatedProduct = createSellerProduct(admin.user(), "Other seller item", "20.00", null, 2, "0.00");
		addToCart(customer.user(), product.product(), 1, null, 0);

		MvcResult checkout = mockMvc.perform(post(CHECKOUT)
				.header("Authorization", bearer(customer))
				.header("Idempotency-Key", UUID.randomUUID().toString())
				.contentType(MediaType.APPLICATION_JSON)
				.content(checkoutPayload(shipping.getId(), PaymentMethod.COD)))
				.andExpect(status().isCreated())
				.andReturn();
		Long orderId = JsonPath.<Number>read(checkout.getResponse().getContentAsString(), "$.data.id").longValue();
		SellerOrder sellerOrder = sellerOrders.findByOrder_IdOrderByIdAsc(orderId).get(0);

		mockMvc.perform(get("/api/v1/orders/" + orderId + "/tracking")
				.header("Authorization", bearer(customer)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.sellerOrders[0].history.length()").value(1))
				.andExpect(jsonPath("$.data.sellerOrders[0].history[0].status").value(OrderStatus.PLACED.name()));
		mockMvc.perform(get("/api/v1/orders/" + orderId + "/tracking")
				.header("Authorization", bearer(createUser(Role.CUSTOMER))))
				.andExpect(status().isNotFound());

		mockMvc.perform(patch("/api/v1/seller/orders/" + sellerOrder.getId() + "/status")
				.header("Authorization", bearer(product.sellerUser()))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"status\":\"CONFIRMED\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.status").value(OrderStatus.CONFIRMED.name()));
		mockMvc.perform(patch("/api/v1/seller/orders/" + sellerOrder.getId() + "/status")
				.header("Authorization", bearer(unrelatedProduct.sellerUser()))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"status\":\"PROCESSING\"}"))
				.andExpect(status().isNotFound());
		mockMvc.perform(get("/api/v1/orders/" + orderId + "/tracking")
				.header("Authorization", bearer(customer)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.sellerOrders[0].history.length()").value(2))
				.andExpect(jsonPath("$.data.sellerOrders[0].history[1].status").value(OrderStatus.CONFIRMED.name()));
		mockMvc.perform(patch("/api/v1/seller/orders/" + sellerOrder.getId() + "/status")
				.header("Authorization", bearer(product.sellerUser()))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"status\":\"PROCESSING\"}"))
				.andExpect(status().isOk());
		mockMvc.perform(post("/api/v1/orders/" + orderId + "/cancel")
				.header("Authorization", bearer(customer)))
				.andExpect(status().isConflict());
	}

	@Test
	void cancelsEligibleOrderOnceAndRestoresStock() throws Exception {
		TestUser customer = createUser(Role.CUSTOMER);
		TestUser admin = createUser(Role.SUPER_ADMIN);
		Address shipping = createAddress(customer.user(), "Cancellation recipient");
		SellerProduct product = createSellerProduct(admin.user(), "Cancellable item", "40.00", null, 4, "0.00");
		addToCart(customer.user(), product.product(), 2, null, 0);

		MvcResult checkout = mockMvc.perform(post(CHECKOUT)
				.header("Authorization", bearer(customer))
				.header("Idempotency-Key", UUID.randomUUID().toString())
				.contentType(MediaType.APPLICATION_JSON)
				.content(checkoutPayload(shipping.getId(), PaymentMethod.COD)))
				.andExpect(status().isCreated())
				.andReturn();
		Long orderId = JsonPath.<Number>read(checkout.getResponse().getContentAsString(), "$.data.id").longValue();
		org.junit.jupiter.api.Assertions.assertEquals(2, products.findById(product.product().getId()).orElseThrow().getStockQuantity());

		mockMvc.perform(post("/api/v1/orders/" + orderId + "/cancel")
				.header("Authorization", bearer(customer)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.refundPending").value(false))
				.andExpect(jsonPath("$.data.order.orderStatus").value(OrderStatus.CANCELLED.name()));
		org.junit.jupiter.api.Assertions.assertEquals(4, products.findById(product.product().getId()).orElseThrow().getStockQuantity());

		mockMvc.perform(post("/api/v1/orders/" + orderId + "/cancel")
				.header("Authorization", bearer(customer)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.order.orderStatus").value(OrderStatus.CANCELLED.name()));
		org.junit.jupiter.api.Assertions.assertEquals(4, products.findById(product.product().getId()).orElseThrow().getStockQuantity());
	}

	@Test
	void prepaidCancellationWaitsForRazorpayRefundConfirmation() throws Exception {
		TestUser customer = createUser(Role.CUSTOMER);
		TestUser admin = createUser(Role.SUPER_ADMIN);
		Address shipping = createAddress(customer.user(), "Prepaid recipient");
		SellerProduct product = createSellerProduct(admin.user(), "Prepaid item", "40.00", null, 3, "0.00");
		addToCart(customer.user(), product.product(), 1, null, 0);

		MvcResult checkout = mockMvc.perform(post(CHECKOUT)
				.header("Authorization", bearer(customer))
				.header("Idempotency-Key", UUID.randomUUID().toString())
				.contentType(MediaType.APPLICATION_JSON)
				.content(checkoutPayload(shipping.getId(), PaymentMethod.ONLINE)))
				.andExpect(status().isCreated())
				.andReturn();
		Long orderId = JsonPath.<Number>read(checkout.getResponse().getContentAsString(), "$.data.id").longValue();
		Order order = orders.findByIdAndCustomer_Id(orderId, customer.user().getId()).orElseThrow();
		order.markPaymentSuccessful();
		orders.save(order);

		String paymentId = "pay_test_" + UUID.randomUUID();
		Payment payment = new Payment(
				order,
				customer.user(),
				"payment-" + UUID.randomUUID(),
				"order_test_" + UUID.randomUUID(),
				"rzp_test_shopsphere",
				order.getTotalAmount(),
				"INR");
		payment.markSuccessful(paymentId);
		payments.saveAndFlush(payment);
		when(razorpay.refundPayment(anyString(), anyLong(), anyString()))
				.thenReturn(new RazorpayGateway.GatewayRefund("rfnd_test_pending", "pending"));

		mockMvc.perform(post("/api/v1/orders/" + orderId + "/cancel")
				.header("Authorization", bearer(customer)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.refundPending").value(true))
				.andExpect(jsonPath("$.data.order.orderStatus").value(OrderStatus.CANCELLED.name()))
				.andExpect(jsonPath("$.data.order.paymentStatus").value(PaymentStatus.SUCCESS.name()))
				.andExpect(jsonPath("$.data.order.refundStatus").value("PENDING"));
		org.junit.jupiter.api.Assertions.assertEquals(
				PaymentStatus.SUCCESS,
				payments.findByRazorpayPaymentId(paymentId).orElseThrow().getStatus());
		org.junit.jupiter.api.Assertions.assertEquals(
				"rfnd_test_pending",
				payments.findByRazorpayPaymentId(paymentId).orElseThrow().getRazorpayRefundId());

		mockMvc.perform(post("/api/v1/orders/" + orderId + "/cancel")
				.header("Authorization", bearer(customer)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.refundPending").value(true));
		verify(razorpay, times(1)).refundPayment(anyString(), anyLong(), anyString());
	}

	private void addToCart(User customer, Product firstProduct, int firstQuantity, Product secondProduct, int secondQuantity) {
		Cart cart = new Cart(customer);
		cart.addItem(firstProduct, firstQuantity);
		if (secondProduct != null) cart.addItem(secondProduct, secondQuantity);
		carts.save(cart);
	}

	private Address createAddress(User customer, String fullName) {
		Address address = new Address(
				customer,
				fullName,
				"9876543210",
				"12B",
				"Market Road",
				"Near Central Park",
				"Jaipur",
				"Rajasthan",
				"302001",
				AddressType.HOME,
				true);
		return addresses.save(address);
	}

	private SellerProduct createSellerProduct(
			User admin,
			String name,
			String price,
			String discountPrice,
			int stock,
			String deliveryFee) {
		TestUser sellerUser = createUser(Role.SELLER);
		Seller seller = new Seller(sellerUser.user(), "Store " + UUID.randomUUID(), "Checkout test store", "Business address");
		seller.setApprovalStatus(SellerStatus.APPROVED);
		seller.updateDeliveryFee(new BigDecimal(deliveryFee));
		seller = sellers.save(seller);

		String suffix = UUID.randomUUID().toString();
		Category category = categories.save(new Category(
				"Checkout category " + suffix,
				"checkout-category-" + suffix,
				null,
				null,
				true));
		Product product = new Product(
				seller,
				category,
				name + " " + suffix,
				"checkout-" + suffix,
				"Checkout test product",
				"ShopSphere",
				"SKU-" + suffix,
				new BigDecimal(price),
				discountPrice == null ? null : new BigDecimal(discountPrice),
				stock);
		product.approve(admin, Instant.now());
		product = products.save(product);
		return new SellerProduct(sellerUser, seller, product);
	}

	private TestUser createUser(Role role) {
		String suffix = UUID.randomUUID().toString();
		User user = new User(
				"Checkout Test User",
				"checkout-" + suffix + "@example.test",
				passwordEncoder.encode(suffix));
		user.setRole(role);
		User saved = users.save(user);
		String token = jwtService.generateAccessToken(new CustomUserDetails(saved));
		return new TestUser(saved, token);
	}

	private String checkoutPayload(Long addressId, PaymentMethod paymentMethod) {
		return """
				{"addressId":%d,"paymentMethod":"%s"}
				""".formatted(addressId, paymentMethod.name());
	}

	private String bearer(TestUser user) {
		return "Bearer " + user.token();
	}

	private record TestUser(User user, String token) {
	}

	private record SellerProduct(TestUser sellerUser, Seller seller, Product product) {
	}
}
