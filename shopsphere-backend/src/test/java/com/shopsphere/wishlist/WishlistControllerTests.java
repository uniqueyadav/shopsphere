package com.shopsphere.wishlist;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class WishlistControllerTests {

	private static final String WISHLIST = "/api/v1/wishlist";
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
	void customerCanAddDuplicateSafelyRemoveAndClearWishlistItems() throws Exception {
		TestUser customer = createUser(Role.CUSTOMER);
		TestUser seller = createSeller();
		TestUser admin = createUser(Role.SUPER_ADMIN);
		long productId = createProduct(seller, createCategory(), "Wishlist item " + UUID.randomUUID(), 5);
		approveProduct(seller, admin, productId);
		String imageUrl = "https://images.example.test/" + UUID.randomUUID() + ".jpg";
		addProductImage(productId, imageUrl);

		MvcResult firstAdd = mockMvc.perform(post(WISHLIST + "/items/" + productId)
				.header("Authorization", bearer(customer)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.items.length()").value(1))
				.andExpect(jsonPath("$.data.items[0].productId").value(productId))
				.andExpect(jsonPath("$.data.items[0].productName").value(org.hamcrest.Matchers.startsWith("Wishlist item")))
				.andExpect(jsonPath("$.data.items[0].sellerName").isNotEmpty())
				.andExpect(jsonPath("$.data.items[0].imageUrl").value(imageUrl))
				.andExpect(jsonPath("$.data.items[0].available").value(true))
				.andExpect(jsonPath("$.data.totalItems").value(1))
				.andReturn();
		Number wishlistId = JsonPath.read(firstAdd.getResponse().getContentAsString(), "$.data.id");

		mockMvc.perform(post(WISHLIST + "/items/" + productId)
				.header("Authorization", bearer(customer)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.items.length()").value(1))
				.andExpect(jsonPath("$.data.id").value(wishlistId.intValue()));

		mockMvc.perform(delete(WISHLIST + "/items/" + productId)
				.header("Authorization", bearer(customer)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.items.length()").value(0))
				.andExpect(jsonPath("$.data.totalItems").value(0));

		mockMvc.perform(post(WISHLIST + "/items/" + productId)
				.header("Authorization", bearer(customer)))
				.andExpect(status().isOk());
		mockMvc.perform(delete(WISHLIST + "/clear")
				.header("Authorization", bearer(customer)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.items.length()").value(0));
	}

	@Test
	void wishlistRequiresCustomerRoleAndItemsArePrivateToTheirOwner() throws Exception {
		TestUser customer = createUser(Role.CUSTOMER);
		TestUser anotherCustomer = createUser(Role.CUSTOMER);
		TestUser seller = createSeller();
		TestUser admin = createUser(Role.SUPER_ADMIN);
		long productId = createProduct(seller, createCategory(), "Private wishlist item " + UUID.randomUUID(), 3);
		approveProduct(seller, admin, productId);

		mockMvc.perform(get(WISHLIST))
				.andExpect(status().isUnauthorized());
		mockMvc.perform(get(WISHLIST)
				.header("Authorization", bearer(seller)))
				.andExpect(status().isForbidden());

		mockMvc.perform(post(WISHLIST + "/items/" + productId)
				.header("Authorization", bearer(customer)))
				.andExpect(status().isOk());
		mockMvc.perform(get(WISHLIST)
				.header("Authorization", bearer(anotherCustomer)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.items.length()").value(0));

		mockMvc.perform(delete(WISHLIST + "/items/" + productId)
				.header("Authorization", bearer(anotherCustomer)))
				.andExpect(status().isNotFound());
	}

	@Test
	void wishlistRejectsNonPublicProductsAndReportsOutOfStockAvailability() throws Exception {
		TestUser customer = createUser(Role.CUSTOMER);
		TestUser seller = createSeller();
		TestUser admin = createUser(Role.SUPER_ADMIN);
		long categoryId = createCategory();
		long draftProductId = createProduct(seller, categoryId, "Draft wishlist item " + UUID.randomUUID(), 2);

		mockMvc.perform(post(WISHLIST + "/items/" + draftProductId)
				.header("Authorization", bearer(customer)))
				.andExpect(status().isNotFound());

		long outOfStockProductId = createProduct(
				seller, categoryId, "Out of stock wishlist item " + UUID.randomUUID(), 0);
		approveProduct(seller, admin, outOfStockProductId);
		mockMvc.perform(post(WISHLIST + "/items/" + outOfStockProductId)
				.header("Authorization", bearer(customer)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.items[0].available").value(false))
				.andExpect(jsonPath("$.data.items[0].stockQuantity").value(0));

		long inactiveProductId = createProduct(
				seller, categoryId, "Inactive wishlist item " + UUID.randomUUID(), 2);
		approveProduct(seller, admin, inactiveProductId);
		mockMvc.perform(delete(SELLER_PRODUCTS + "/" + inactiveProductId)
				.header("Authorization", bearer(seller)))
				.andExpect(status().isOk());
		mockMvc.perform(post(WISHLIST + "/items/" + inactiveProductId)
				.header("Authorization", bearer(customer)))
				.andExpect(status().isNotFound());
	}

	private long createProduct(TestUser seller, long categoryId, String name, int stock) throws Exception {
		String sku = "WISH-" + UUID.randomUUID();
		MvcResult result = mockMvc.perform(post(SELLER_PRODUCTS)
				.header("Authorization", bearer(seller))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"productName":"%s","description":"Wishlist test product","brand":"Test brand","sku":"%s",
						 "categoryId":%d,"price":25.00,"stockQuantity":%d}
						""".formatted(name, sku, categoryId, stock)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.data.status").value(ProductStatus.DRAFT.name()))
				.andReturn();
		Number id = JsonPath.read(result.getResponse().getContentAsString(), "$.data.id");
		return id.longValue();
	}

	private void addProductImage(long productId, String imageUrl) {
		Product product = products.findById(productId).orElseThrow();
		productImages.save(new ProductImage(
				product,
				imageUrl,
				"wishlist-test-" + UUID.randomUUID(),
				0,
				true));
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
				"Wishlist test category " + suffix,
				"wishlist-test-category-" + suffix,
				null,
				null,
				true);
		return categories.save(category).getId();
	}

	private TestUser createSeller() {
		TestUser user = createUser(Role.SELLER);
		User savedUser = users.findById(user.id()).orElseThrow();
		Seller seller = new Seller(
				savedUser,
				"Wishlist test store " + UUID.randomUUID(),
				"Wishlist test store",
				"Test address");
		seller.setApprovalStatus(SellerStatus.APPROVED);
		Seller savedSeller = sellers.save(seller);
		return new TestUser(user.token(), user.id(), savedSeller.getId());
	}

	private TestUser createUser(Role role) {
		User user = new User(
				"Wishlist Test User",
				"wishlist-test-" + UUID.randomUUID() + "@example.test",
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
