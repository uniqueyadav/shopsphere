package com.shopsphere.product;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import com.shopsphere.category.entity.Category;
import com.shopsphere.category.repository.CategoryRepository;
import com.shopsphere.common.enums.Role;
import com.shopsphere.common.enums.SellerStatus;
import com.shopsphere.product.service.ProductImageStorage;
import com.shopsphere.product.service.StoredProductImage;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(properties = "shopsphere.product-image.max-size-bytes=64")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProductImageControllerTests {

	private static final String SELLER_PRODUCTS = "/api/v1/seller/products";
	private static final String PUBLIC_PRODUCTS = "/api/v1/products";
	private static final byte[] PNG_SIGNATURE = new byte[] {
			(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A
	};
	private static final byte[] WEBP_SIGNATURE = new byte[] {
			'R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'E', 'B', 'P'
	};

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

	@MockitoBean
	private ProductImageStorage storage;

	@Test
	void sellerCanUploadReorderChoosePrimaryAndRemoveImages() throws Exception {
		TestUser owner = createSeller();
		TestUser admin = createUser(Role.SUPER_ADMIN);
		long categoryId = createCategory();
		long productId = createProduct(owner, categoryId);
		when(storage.upload(any(byte[].class), anyString()))
				.thenReturn(
						new StoredProductImage("https://images.example.test/first.png", "shopsphere/first"),
						new StoredProductImage("https://images.example.test/second.webp", "shopsphere/second"));

		MvcResult uploadResult = mockMvc.perform(multipart(imagePath(productId))
				.file(new MockMultipartFile("files", "first.png", "image/png", PNG_SIGNATURE))
				.file(new MockMultipartFile("files", "second.webp", "image/webp", WEBP_SIGNATURE))
				.header("Authorization", bearer(owner)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.data.length()").value(2))
				.andExpect(jsonPath("$.data[0].isPrimary").value(true))
				.andExpect(jsonPath("$.data[1].isPrimary").value(false))
				.andExpect(jsonPath("$.data[0].url").value("https://images.example.test/first.png"))
				.andExpect(jsonPath("$.data[0].publicId").doesNotExist())
				.andReturn();
		Number firstIdValue = JsonPath.read(uploadResult.getResponse().getContentAsString(), "$.data[0].id");
		Number secondIdValue = JsonPath.read(uploadResult.getResponse().getContentAsString(), "$.data[1].id");
		long firstId = firstIdValue.longValue();
		long secondId = secondIdValue.longValue();

		mockMvc.perform(put(imagePath(productId) + "/" + secondId + "/primary")
				.header("Authorization", bearer(owner)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data[0].isPrimary").value(false))
				.andExpect(jsonPath("$.data[1].isPrimary").value(true));

		mockMvc.perform(put(imagePath(productId) + "/reorder")
				.header("Authorization", bearer(owner))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"imageIds\":[" + secondId + "," + firstId + "]}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data[0].id").value(secondId))
				.andExpect(jsonPath("$.data[0].isPrimary").value(true));

		mockMvc.perform(delete(imagePath(productId) + "/" + secondId)
				.header("Authorization", bearer(owner)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.length()").value(1))
				.andExpect(jsonPath("$.data[0].id").value(firstId))
				.andExpect(jsonPath("$.data[0].isPrimary").value(true));
		verify(storage).delete("shopsphere/second");

		mockMvc.perform(post(SELLER_PRODUCTS + "/" + productId + "/submit-for-approval")
				.header("Authorization", bearer(owner)))
				.andExpect(status().isOk());
		mockMvc.perform(post("/api/v1/admin/products/" + productId + "/approve")
				.header("Authorization", bearer(admin)))
				.andExpect(status().isOk());
		mockMvc.perform(get(PUBLIC_PRODUCTS + "/" + productId + "/images"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.length()").value(1))
				.andExpect(jsonPath("$.data[0].url").value("https://images.example.test/first.png"));
	}

	@Test
	void imageOwnershipIsRequiredBeforeAnyRemoteMutation() throws Exception {
		TestUser owner = createSeller();
		TestUser otherSeller = createSeller();
		long productId = createProduct(owner, createCategory());
		when(storage.upload(any(byte[].class), anyString()))
				.thenReturn(new StoredProductImage("https://images.example.test/owned.png", "shopsphere/owned"));
		MvcResult result = mockMvc.perform(multipart(imagePath(productId))
				.file(new MockMultipartFile("files", "owned.png", "image/png", PNG_SIGNATURE))
				.header("Authorization", bearer(owner)))
				.andExpect(status().isCreated())
				.andReturn();
		Number imageIdValue = JsonPath.read(result.getResponse().getContentAsString(), "$.data[0].id");
		long imageId = imageIdValue.longValue();

		mockMvc.perform(delete(imagePath(productId) + "/" + imageId)
				.header("Authorization", bearer(otherSeller)))
				.andExpect(status().isNotFound());
		mockMvc.perform(put(imagePath(productId) + "/" + imageId + "/primary")
				.header("Authorization", bearer(otherSeller)))
				.andExpect(status().isNotFound());
		mockMvc.perform(get(imagePath(productId))
				.header("Authorization", bearer(otherSeller)))
				.andExpect(status().isNotFound());
		verify(storage, never()).delete("shopsphere/owned");
	}

	@Test
	void rejectsUnsupportedAndOversizedFilesBeforeCallingCloudinary() throws Exception {
		TestUser owner = createSeller();
		long productId = createProduct(owner, createCategory());
		byte[] oversized = new byte[65];
		System.arraycopy(PNG_SIGNATURE, 0, oversized, 0, PNG_SIGNATURE.length);

		mockMvc.perform(multipart(imagePath(productId))
				.file(new MockMultipartFile("files", "notes.txt", "text/plain", "not an image".getBytes()))
				.header("Authorization", bearer(owner)))
				.andExpect(status().isBadRequest());
		mockMvc.perform(multipart(imagePath(productId))
				.file(new MockMultipartFile("files", "large.png", "image/png", oversized))
				.header("Authorization", bearer(owner)))
				.andExpect(status().isPayloadTooLarge());
		verify(storage, never()).upload(any(byte[].class), anyString());
	}

	private String imagePath(long productId) {
		return SELLER_PRODUCTS + "/" + productId + "/images";
	}

	private long createProduct(TestUser seller, long categoryId) throws Exception {
		String suffix = UUID.randomUUID().toString();
		MvcResult result = mockMvc.perform(post(SELLER_PRODUCTS)
				.header("Authorization", bearer(seller))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"productName":"Image test %s","brand":"Test brand","sku":"IMAGE-%s",
						 "categoryId":%d,"price":10.00,"stockQuantity":1}
						""".formatted(suffix, suffix, categoryId)))
				.andExpect(status().isCreated())
				.andReturn();
		Number id = JsonPath.read(result.getResponse().getContentAsString(), "$.data.id");
		return id.longValue();
	}

	private long createCategory() {
		String suffix = UUID.randomUUID().toString();
		Category category = new Category("Image category " + suffix, "image-category-" + suffix, null, null, true);
		return categories.save(category).getId();
	}

	private TestUser createSeller() {
		TestUser user = createUser(Role.SELLER);
		User savedUser = users.findById(user.userId()).orElseThrow();
		Seller seller = new Seller(savedUser, "Image store " + UUID.randomUUID(), "Image test", "Test address");
		seller.setApprovalStatus(SellerStatus.APPROVED);
		sellers.save(seller);
		return user;
	}

	private TestUser createUser(Role role) {
		User user = new User(
				"Image Test User",
				"image-test-" + UUID.randomUUID() + "@example.test",
				passwordEncoder.encode(UUID.randomUUID().toString()));
		user.setRole(role);
		User saved = users.save(user);
		String token = jwtService.generateAccessToken(new CustomUserDetails(saved));
		return new TestUser(token, saved.getId());
	}

	private String bearer(TestUser user) {
		return "Bearer " + user.token();
	}

	private record TestUser(String token, Long userId) {
	}
}
