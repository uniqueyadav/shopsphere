package com.shopsphere.cart;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import com.shopsphere.category.entity.Category;
import com.shopsphere.category.repository.CategoryRepository;
import com.shopsphere.common.enums.ProductStatus;
import com.shopsphere.common.enums.Role;
import com.shopsphere.common.enums.SellerStatus;
import com.shopsphere.product.entity.Product;
import com.shopsphere.product.entity.ProductImage;
import com.shopsphere.product.repository.ProductImageRepository;
import com.shopsphere.product.repository.ProductRepository;
import com.shopsphere.security.CustomUserDetails;
import com.shopsphere.security.JwtService;
import com.shopsphere.seller.entity.Seller;
import com.shopsphere.seller.repository.SellerRepository;
import com.shopsphere.user.ShopUserRepository;
import com.shopsphere.user.entity.User;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CartControllerTests {

	private static final String CART = "/api/v1/cart";
	private static final String SELLER_PRODUCTS = "/api/v1/seller/products";
	private static final String ADMIN_PRODUCTS = "/api/v1/admin/products";

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
	private ProductImageRepository productImages;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private JwtService jwtService;

	@Test
	void customerCanAddMergeUpdateRemoveAndClearCartItemsAtServerPrice() throws Exception {
		TestUser customer = createUser(Role.CUSTOMER);
		TestUser seller = createSeller();
		TestUser admin = createUser(Role.SUPER_ADMIN);
		long categoryId = createCategory();
		long productId = createProduct(seller, categoryId, "Cart item " + UUID.randomUUID(), "100.00", "80.00", 8);
		approveProduct(seller, admin, productId);
		String imageUrl = "https://images.example.test/" + UUID.randomUUID() + ".jpg";
		addProductImage(productId, imageUrl);

		MvcResult firstAdd = mockMvc.perform(post(CART + "/items")
				.header("Authorization", bearer(customer))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"productId\":%d,\"quantity\":2,\"price\":0.01}".formatted(productId)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.items.length()").value(1))
				.andExpect(jsonPath("$.data.items[0].quantity").value(2))
				.andExpect(jsonPath("$.data.items[0].sellerName").isNotEmpty())
				.andExpect(jsonPath("$.data.items[0].imageUrl").value(imageUrl))
				.andExpect(jsonPath("$.data.items[0].unitPrice").value(80.0))
				.andExpect(jsonPath("$.data.items[0].lineSubtotal").value(160.0))
				.andExpect(jsonPath("$.data.subtotal").value(160.0))
				.andExpect(jsonPath("$.data.totalQuantity").value(2))
				.andReturn();
		Number itemId = JsonPath.read(firstAdd.getResponse().getContentAsString(), "$.data.items[0].id");
		Number cartId = JsonPath.read(firstAdd.getResponse().getContentAsString(), "$.data.id");

		mockMvc.perform(post(CART + "/items")
				.header("Authorization", bearer(customer))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"productId\":%d,\"quantity\":3}".formatted(productId)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.id").value(cartId.intValue()))
				.andExpect(jsonPath("$.data.items.length()").value(1))
				.andExpect(jsonPath("$.data.items[0].quantity").value(5))
				.andExpect(jsonPath("$.data.subtotal").value(400.0));

		mockMvc.perform(put(CART + "/items/" + itemId)
				.header("Authorization", bearer(customer))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"quantity\":4}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.items[0].quantity").value(4))
				.andExpect(jsonPath("$.data.subtotal").value(320.0));

		mockMvc.perform(delete(CART + "/items/" + itemId)
				.header("Authorization", bearer(customer)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.items.length()").value(0))
				.andExpect(jsonPath("$.data.subtotal").value(0));

		mockMvc.perform(post(CART + "/items")
				.header("Authorization", bearer(customer))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"productId\":%d,\"quantity\":1}".formatted(productId)))
				.andExpect(status().isOk());
		mockMvc.perform(delete(CART + "/clear")
				.header("Authorization", bearer(customer)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.items.length()").value(0))
				.andExpect(jsonPath("$.data.subtotal").value(0));

		mockMvc.perform(get(CART)
				.header("Authorization", bearer(customer)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.id").value(cartId.intValue()))
				.andExpect(jsonPath("$.data.totalQuantity").value(0));
	}

	@Test
	void addingOrUpdatingRequiresApprovedActiveProductAndAvailableStock() throws Exception {
		TestUser customer = createUser(Role.CUSTOMER);
		TestUser seller = createSeller();
		TestUser admin = createUser(Role.SUPER_ADMIN);
		long categoryId = createCategory();

		long draftProductId = createProduct(seller, categoryId, "Draft item " + UUID.randomUUID(), "20.00", null, 4);
		mockMvc.perform(post(CART + "/items")
				.header("Authorization", bearer(customer))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"productId\":%d,\"quantity\":1}".formatted(draftProductId)))
				.andExpect(status().isNotFound());

		long limitedProductId = createProduct(
				seller, categoryId, "Limited item " + UUID.randomUUID(), "30.00", null, 3);
		approveProduct(seller, admin, limitedProductId);
		MvcResult added = mockMvc.perform(post(CART + "/items")
				.header("Authorization", bearer(customer))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"productId\":%d,\"quantity\":2}".formatted(limitedProductId)))
				.andExpect(status().isOk())
				.andReturn();
		Number itemId = JsonPath.read(added.getResponse().getContentAsString(), "$.data.items[0].id");

		mockMvc.perform(post(CART + "/items")
				.header("Authorization", bearer(customer))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"productId\":%d,\"quantity\":2}".formatted(limitedProductId)))
				.andExpect(status().isConflict());
		mockMvc.perform(put(CART + "/items/" + itemId)
				.header("Authorization", bearer(customer))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"quantity\":4}"))
				.andExpect(status().isConflict());

		long outOfStockProductId = createProduct(
				seller, categoryId, "Out of stock " + UUID.randomUUID(), "15.00", null, 0);
		approveProduct(seller, admin, outOfStockProductId);
		mockMvc.perform(post(CART + "/items")
				.header("Authorization", bearer(customer))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"productId\":%d,\"quantity\":1}".formatted(outOfStockProductId)))
				.andExpect(status().isConflict());

		long inactiveProductId = createProduct(
				seller, categoryId, "Inactive item " + UUID.randomUUID(), "25.00", null, 2);
		approveProduct(seller, admin, inactiveProductId);
		mockMvc.perform(delete(SELLER_PRODUCTS + "/" + inactiveProductId)
				.header("Authorization", bearer(seller)))
				.andExpect(status().isOk());
		mockMvc.perform(post(CART + "/items")
				.header("Authorization", bearer(customer))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"productId\":%d,\"quantity\":1}".formatted(inactiveProductId)))
				.andExpect(status().isNotFound());
	}

	@Test
	void cartRequiresCustomerAuthenticationAndItemsArePrivateToTheirOwner() throws Exception {
		TestUser customer = createUser(Role.CUSTOMER);
		TestUser anotherCustomer = createUser(Role.CUSTOMER);
		TestUser seller = createSeller();
		TestUser admin = createUser(Role.SUPER_ADMIN);
		long productId = createProduct(
				seller, createCategory(), "Private cart item " + UUID.randomUUID(), "10.00", null, 3);
		approveProduct(seller, admin, productId);

		mockMvc.perform(get(CART))
				.andExpect(status().isUnauthorized());
		mockMvc.perform(get(CART)
				.header("Authorization", bearer(seller)))
				.andExpect(status().isForbidden());

		MvcResult added = mockMvc.perform(post(CART + "/items")
				.header("Authorization", bearer(customer))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"productId\":%d,\"quantity\":1}".formatted(productId)))
				.andExpect(status().isOk())
				.andReturn();
		Number itemId = JsonPath.read(added.getResponse().getContentAsString(), "$.data.items[0].id");

		mockMvc.perform(put(CART + "/items/" + itemId)
				.header("Authorization", bearer(anotherCustomer))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"quantity\":2}"))
				.andExpect(status().isNotFound());
		mockMvc.perform(delete(CART + "/items/" + itemId)
				.header("Authorization", bearer(anotherCustomer)))
				.andExpect(status().isNotFound());
		mockMvc.perform(get(CART)
				.header("Authorization", bearer(anotherCustomer)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.items.length()").value(0));

		mockMvc.perform(post(CART + "/items")
				.header("Authorization", bearer(customer))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"productId\":%d,\"quantity\":0}".formatted(productId)))
				.andExpect(status().isBadRequest());
	}

	private long createProduct(
			TestUser seller,
			long categoryId,
			String name,
			String price,
			String discountPrice,
			int stock) throws Exception {
		String sku = "CART-" + UUID.randomUUID();
		String discount = discountPrice == null ? "null" : discountPrice;
		MvcResult result = mockMvc.perform(post(SELLER_PRODUCTS)
				.header("Authorization", bearer(seller))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"productName":"%s","description":"Cart test product","brand":"Test brand","sku":"%s",
						 "categoryId":%d,"price":%s,"discountPrice":%s,"stockQuantity":%d}
						""".formatted(name, sku, categoryId, price, discount, stock)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.data.status").value(ProductStatus.DRAFT.name()))
				.andReturn();
		Number id = JsonPath.read(result.getResponse().getContentAsString(), "$.data.id");
		return id.longValue();
	}

	private void approveProduct(TestUser seller, TestUser admin, long productId) throws Exception {
		mockMvc.perform(post(SELLER_PRODUCTS + "/" + productId + "/submit-for-approval")
				.header("Authorization", bearer(seller)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.status").value(ProductStatus.PENDING.name()));
		mockMvc.perform(post(ADMIN_PRODUCTS + "/" + productId + "/approve")
				.header("Authorization", bearer(admin)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.status").value(ProductStatus.APPROVED.name()));
	}

	private long createCategory() {
		String suffix = UUID.randomUUID().toString();
		Category category = new Category(
				"Cart test category " + suffix,
				"cart-test-category-" + suffix,
				null,
				null,
				true);
		return categories.save(category).getId();
	}

	private void addProductImage(long productId, String imageUrl) {
		Product product = products.findById(productId).orElseThrow();
		productImages.save(new ProductImage(
				product,
				imageUrl,
				"cart-test-" + UUID.randomUUID(),
				0,
				true));
	}

	private TestUser createSeller() {
		TestUser user = createUser(Role.SELLER);
		User savedUser = users.findById(user.id()).orElseThrow();
		Seller seller = new Seller(savedUser, "Cart test store " + UUID.randomUUID(), "Cart test store", "Test address");
		seller.setApprovalStatus(SellerStatus.APPROVED);
		Seller savedSeller = sellers.save(seller);
		return new TestUser(user.token(), user.id(), savedSeller.getId());
	}

	private TestUser createUser(Role role) {
		User user = new User(
				"Cart Test User",
				"cart-test-" + UUID.randomUUID() + "@example.test",
				passwordEncoder.encode(UUID.randomUUID().toString()));
		user.setRole(role);
		User saved = users.save(user);
		String token = jwtService.generateAccessToken(new CustomUserDetails(saved));
		return new TestUser(token, saved.getId(), null);
	}

	private String bearer(TestUser user) {
		return "Bearer " + user.token();
	}

	private record TestUser(String token, Long id, Long sellerId) {
	}
}
