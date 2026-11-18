package com.shopsphere.product;

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
class ProductControllerTests {

	private static final String SELLER_PRODUCTS = "/api/v1/seller/products";
	private static final String ADMIN_PRODUCTS = "/api/v1/admin/products";
	private static final String PUBLIC_PRODUCTS = "/api/v1/products";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ShopUserRepository users;

	@Autowired
	private SellerRepository sellers;

	@Autowired
	private CategoryRepository categories;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private JwtService jwtService;

	@Test
	void sellerCanManageOnlyTheirOwnProductsAndEditsRequireResubmission() throws Exception {
		TestUser owner = createSeller(SellerStatus.APPROVED);
		TestUser anotherSeller = createSeller(SellerStatus.APPROVED);
		long categoryId = createCategory(true);
		String suffix = UUID.randomUUID().toString();
		long productId = createProduct(owner, categoryId, "Own product " + suffix, "SKU-" + suffix);

		mockMvc.perform(get(SELLER_PRODUCTS + "/" + productId)
				.header("Authorization", bearer(owner)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.id").value(productId));
		mockMvc.perform(get(SELLER_PRODUCTS)
				.header("Authorization", bearer(owner))
				.param("search", suffix)
				.param("page", "0")
				.param("size", "1"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.totalElements").value(1))
				.andExpect(jsonPath("$.data.items[0].sellerId").value(owner.sellerId()));

		mockMvc.perform(post(SELLER_PRODUCTS + "/" + productId + "/submit-for-approval")
				.header("Authorization", bearer(owner)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.status").value(ProductStatus.PENDING.name()));

		String updatedName = "Updated product " + suffix;
		mockMvc.perform(put(SELLER_PRODUCTS + "/" + productId)
				.header("Authorization", bearer(owner))
				.contentType(MediaType.APPLICATION_JSON)
				.content(productRequest(updatedName, "SKU-UPDATED-" + suffix, categoryId, "120.00", "90.00", 4)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.productName").value(updatedName))
				.andExpect(jsonPath("$.data.status").value(ProductStatus.DRAFT.name()));

		mockMvc.perform(get(SELLER_PRODUCTS + "/" + productId)
				.header("Authorization", bearer(anotherSeller)))
				.andExpect(status().isNotFound());
		mockMvc.perform(put(SELLER_PRODUCTS + "/" + productId)
				.header("Authorization", bearer(anotherSeller))
				.contentType(MediaType.APPLICATION_JSON)
				.content(productRequest("Tampered", "SKU-TAMPERED-" + suffix, categoryId, "10.00", null, 1)))
				.andExpect(status().isNotFound());
		mockMvc.perform(delete(SELLER_PRODUCTS + "/" + productId)
				.header("Authorization", bearer(anotherSeller)))
				.andExpect(status().isNotFound());

		mockMvc.perform(delete(SELLER_PRODUCTS + "/" + productId)
				.header("Authorization", bearer(owner)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.isActive").value(false));
	}

	@Test
	void onlySuperAdminCanReviewProductsAndMarketplaceShowsOnlyApprovedActiveItems() throws Exception {
		TestUser seller = createSeller(SellerStatus.APPROVED);
		TestUser admin = createUser(Role.SUPER_ADMIN);
		TestUser customer = createUser(Role.CUSTOMER);
		long categoryId = createCategory(true);
		String suffix = UUID.randomUUID().toString();
		long approvedId = createProduct(seller, categoryId, "Public item " + suffix, "SKU-PUBLIC-" + suffix);
		MvcResult sellerProduct = mockMvc.perform(get(SELLER_PRODUCTS + "/" + approvedId)
				.header("Authorization", bearer(seller)))
				.andExpect(status().isOk())
				.andReturn();
		String approvedSlug = JsonPath.read(sellerProduct.getResponse().getContentAsString(), "$.data.slug");

		mockMvc.perform(get(PUBLIC_PRODUCTS + "/" + approvedId))
				.andExpect(status().isNotFound());
		mockMvc.perform(get(PUBLIC_PRODUCTS + "/slug/" + approvedSlug))
				.andExpect(status().isNotFound());
		mockMvc.perform(post(SELLER_PRODUCTS + "/" + approvedId + "/submit-for-approval")
				.header("Authorization", bearer(seller)))
				.andExpect(status().isOk());

		mockMvc.perform(get(ADMIN_PRODUCTS)
				.header("Authorization", bearer(admin))
				.param("search", suffix)
				.param("status", ProductStatus.PENDING.name()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.totalElements").value(1));
		mockMvc.perform(get(ADMIN_PRODUCTS + "/" + approvedId)
				.header("Authorization", bearer(admin)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.status").value(ProductStatus.PENDING.name()));
		mockMvc.perform(get(PUBLIC_PRODUCTS)
				.param("search", suffix))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.totalElements").value(0));

		mockMvc.perform(post(ADMIN_PRODUCTS + "/" + approvedId + "/approve")
				.header("Authorization", bearer(customer)))
				.andExpect(status().isForbidden());
		mockMvc.perform(post(ADMIN_PRODUCTS + "/" + approvedId + "/approve")
				.header("Authorization", bearer(admin)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.status").value(ProductStatus.APPROVED.name()))
				.andExpect(jsonPath("$.data.reviewedById").value(admin.userId()))
				.andExpect(jsonPath("$.data.reviewedAt").isNotEmpty());
		mockMvc.perform(get(PUBLIC_PRODUCTS + "/slug/" + approvedSlug))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.id").value(approvedId))
				.andExpect(jsonPath("$.data.status").value(ProductStatus.APPROVED.name()));

		mockMvc.perform(get(PUBLIC_PRODUCTS)
				.param("search", suffix)
				.param("categoryId", Long.toString(categoryId))
				.param("minPrice", "50")
				.param("maxPrice", "150"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.totalElements").value(1))
				.andExpect(jsonPath("$.data.items[0].id").value(approvedId));
		mockMvc.perform(delete(SELLER_PRODUCTS + "/" + approvedId)
				.header("Authorization", bearer(seller)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.isActive").value(false));
		mockMvc.perform(get(PUBLIC_PRODUCTS + "/" + approvedId))
				.andExpect(status().isNotFound());
		mockMvc.perform(get(PUBLIC_PRODUCTS + "/slug/" + approvedSlug))
				.andExpect(status().isNotFound());

		long rejectedId = createProduct(seller, categoryId, "Rejected item " + suffix, "SKU-REJECT-" + suffix);
		mockMvc.perform(post(SELLER_PRODUCTS + "/" + rejectedId + "/submit-for-approval")
				.header("Authorization", bearer(seller)))
				.andExpect(status().isOk());
		mockMvc.perform(post(ADMIN_PRODUCTS + "/" + rejectedId + "/reject")
				.header("Authorization", bearer(admin))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"rejectionReason\":\"Product photos do not meet the requirements.\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.status").value(ProductStatus.REJECTED.name()))
				.andExpect(jsonPath("$.data.rejectionReason").value("Product photos do not meet the requirements."));
		mockMvc.perform(get(PUBLIC_PRODUCTS + "/" + rejectedId))
				.andExpect(status().isNotFound());
		mockMvc.perform(post(ADMIN_PRODUCTS + "/" + rejectedId + "/reject")
				.header("Authorization", bearer(admin))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"rejectionReason\":\" \"}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void validatesProductDataCategoryStateAndSellerApproval() throws Exception {
		TestUser seller = createSeller(SellerStatus.APPROVED);
		TestUser pendingSeller = createSeller(SellerStatus.PENDING);
		TestUser customer = createUser(Role.CUSTOMER);
		long activeCategory = createCategory(true);
		long inactiveCategory = createCategory(false);
		String suffix = UUID.randomUUID().toString();

		mockMvc.perform(post(SELLER_PRODUCTS)
				.header("Authorization", bearer(seller))
				.contentType(MediaType.APPLICATION_JSON)
				.content(productRequest("Invalid discount " + suffix, "SKU-BAD-" + suffix, activeCategory, "10.00", "10.00", 1)))
				.andExpect(status().isBadRequest());

		mockMvc.perform(post(SELLER_PRODUCTS)
				.header("Authorization", bearer(seller))
				.contentType(MediaType.APPLICATION_JSON)
				.content(productRequest("Inactive category " + suffix, "SKU-INACTIVE-" + suffix, inactiveCategory, "10.00", null, 1)))
				.andExpect(status().isBadRequest());

		mockMvc.perform(post(SELLER_PRODUCTS)
				.header("Authorization", bearer(pendingSeller))
				.contentType(MediaType.APPLICATION_JSON)
				.content(productRequest("Pending seller " + suffix, "SKU-PENDING-" + suffix, activeCategory, "10.00", null, 1)))
				.andExpect(status().isForbidden());

		mockMvc.perform(get(SELLER_PRODUCTS)
				.header("Authorization", bearer(customer)))
				.andExpect(status().isForbidden());
		mockMvc.perform(get(ADMIN_PRODUCTS)
				.header("Authorization", bearer(customer)))
				.andExpect(status().isForbidden());
		mockMvc.perform(get(SELLER_PRODUCTS))
				.andExpect(status().isUnauthorized());
		mockMvc.perform(get(ADMIN_PRODUCTS))
				.andExpect(status().isUnauthorized());

		mockMvc.perform(get(PUBLIC_PRODUCTS)
				.param("minPrice", "20")
				.param("maxPrice", "10"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void publicListingAppliesDiscountAvailabilityAndPriceSortAndReturnsImages() throws Exception {
		TestUser seller = createSeller(SellerStatus.APPROVED);
		TestUser admin = createUser(Role.SUPER_ADMIN);
		long categoryId = createCategory(true);
		String suffix = UUID.randomUUID().toString();
		long discountedOutOfStockId = createProduct(
				seller, categoryId, "Discounted out of stock " + suffix, "SKU-DISCOUNT-" + suffix, "80.00", "60.00", 0);
		long regularInStockId = createProduct(
				seller, categoryId, "Regular in stock " + suffix, "SKU-REGULAR-" + suffix, "30.00", null, 5);

		approveProduct(seller, admin, discountedOutOfStockId);
		approveProduct(seller, admin, regularInStockId);

		mockMvc.perform(get(PUBLIC_PRODUCTS)
				.param("search", suffix)
				.param("discountedOnly", "true"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.totalElements").value(1))
				.andExpect(jsonPath("$.data.items[0].id").value(discountedOutOfStockId))
				.andExpect(jsonPath("$.data.items[0].images").isArray());

		mockMvc.perform(get(PUBLIC_PRODUCTS)
				.param("search", suffix)
				.param("availability", "OUT_OF_STOCK"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.totalElements").value(1))
				.andExpect(jsonPath("$.data.items[0].id").value(discountedOutOfStockId));

		mockMvc.perform(get(PUBLIC_PRODUCTS)
				.param("search", suffix)
				.param("availability", "IN_STOCK")
				.param("sort", "price_asc"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.totalElements").value(1))
				.andExpect(jsonPath("$.data.items[0].id").value(regularInStockId));

		mockMvc.perform(get(PUBLIC_PRODUCTS + "/" + regularInStockId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.images").isArray());
	}

	private long createProduct(
			TestUser seller,
			long categoryId,
			String name,
			String sku) throws Exception {
		return createProduct(seller, categoryId, name, sku, "100.00", "80.00", 10);
	}

	private long createProduct(
			TestUser seller,
			long categoryId,
			String name,
			String sku,
			String price,
			String discountPrice,
			int stock) throws Exception {
		MvcResult result = mockMvc.perform(post(SELLER_PRODUCTS)
				.header("Authorization", bearer(seller))
				.contentType(MediaType.APPLICATION_JSON)
				.content(productRequest(name, sku, categoryId, price, discountPrice, stock)))
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

	private long createCategory(boolean active) {
		String suffix = UUID.randomUUID().toString();
		Category category = new Category(
				"Product test category " + suffix,
				"product-test-category-" + suffix,
				null,
				null,
				active);
		return categories.save(category).getId();
	}

	private TestUser createSeller(SellerStatus status) {
		TestUser user = createUser(Role.SELLER);
		User savedUser = users.findById(user.userId()).orElseThrow();
		Seller seller = new Seller(
				savedUser,
				"Test store " + UUID.randomUUID(),
				"Product test store",
				"Test address");
		seller.setApprovalStatus(status);
		Seller savedSeller = sellers.save(seller);
		return new TestUser(user.token(), user.userId(), savedSeller.getId());
	}

	private TestUser createUser(Role role) {
		User user = new User(
				"Product Test User",
				"product-test-" + UUID.randomUUID() + "@example.test",
				passwordEncoder.encode(UUID.randomUUID().toString()));
		user.setRole(role);
		User saved = users.save(user);
		String token = jwtService.generateAccessToken(new CustomUserDetails(saved));
		return new TestUser(token, saved.getId(), null);
	}

	private String productRequest(
			String name,
			String sku,
			long categoryId,
			String price,
			String discountPrice,
			int stock) {
		String discount = discountPrice == null ? "null" : discountPrice;
		return """
				{"productName":"%s","description":"Product details","brand":"Test brand","sku":"%s",
				 "categoryId":%d,"price":%s,"discountPrice":%s,"stockQuantity":%d}
				""".formatted(name, sku, categoryId, price, discount, stock);
	}

	private String bearer(TestUser user) {
		return "Bearer " + user.token();
	}

	private record TestUser(String token, Long userId, Long sellerId) {
	}
}
