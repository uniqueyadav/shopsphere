package com.shopsphere.category;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import com.shopsphere.common.enums.Role;
import com.shopsphere.security.CustomUserDetails;
import com.shopsphere.security.JwtService;
import com.shopsphere.user.ShopUserRepository;
import com.shopsphere.user.entity.User;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.jayway.jsonpath.JsonPath;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CategoryControllerTests {

	private static final String ADMIN_CATEGORIES = "/api/v1/admin/categories";
	private static final String PUBLIC_CATEGORIES = "/api/v1/categories";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ShopUserRepository users;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private JwtService jwtService;

	@Test
	void superAdminCanCreateReadUpdateAndSafelyDeleteCategories() throws Exception {
		TestUser admin = createUser(Role.SUPER_ADMIN);
		String suffix = UUID.randomUUID().toString();
		String name = "Category " + suffix;
		long id = createCategory(admin, name, null, true);

		mockMvc.perform(get(ADMIN_CATEGORIES + "/" + id)
				.header("Authorization", "Bearer " + admin.token()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.name").value(name))
				.andExpect(jsonPath("$.data.slug").value("category-" + suffix))
				.andExpect(jsonPath("$.data.isActive").value(true))
				.andExpect(jsonPath("$.data.createdAt").isNotEmpty())
				.andExpect(jsonPath("$.data.updatedAt").isNotEmpty());

		mockMvc.perform(get(PUBLIC_CATEGORIES + "/" + id))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.id").value(id))
				.andExpect(jsonPath("$.data.isActive").value(true));

		String updatedName = "Updated " + suffix;
		mockMvc.perform(put(ADMIN_CATEGORIES + "/" + id)
				.header("Authorization", "Bearer " + admin.token())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"name":"%s","slug":"updated-%s","description":"Updated details","imageUrl":"","isActive":true}
						""".formatted(updatedName, suffix)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.name").value(updatedName))
				.andExpect(jsonPath("$.data.slug").value("updated-" + suffix))
				.andExpect(jsonPath("$.data.description").value("Updated details"))
				.andExpect(jsonPath("$.data.imageUrl").value(org.hamcrest.Matchers.nullValue()));

		mockMvc.perform(get(ADMIN_CATEGORIES)
				.header("Authorization", "Bearer " + admin.token())
				.param("search", suffix)
				.param("page", "0")
				.param("size", "1"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.items.length()").value(1))
				.andExpect(jsonPath("$.data.totalElements").value(1));

		mockMvc.perform(delete(ADMIN_CATEGORIES + "/" + id)
				.header("Authorization", "Bearer " + admin.token()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.isActive").value(false));

		mockMvc.perform(get(PUBLIC_CATEGORIES + "/" + id))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.success").value(false));
		mockMvc.perform(get(ADMIN_CATEGORIES + "/" + id)
				.header("Authorization", "Bearer " + admin.token()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.isActive").value(false));
	}

	@Test
	void publicCategoryBrowseSupportsSearchPaginationAndOnlyReturnsActiveCategories() throws Exception {
		TestUser admin = createUser(Role.SUPER_ADMIN);
		String search = "browse-" + UUID.randomUUID().toString().substring(0, 8);
		createCategory(admin, search + " first", null, true);
		createCategory(admin, search + " second", null, true);
		createCategory(admin, search + " hidden", null, false);

		mockMvc.perform(get(PUBLIC_CATEGORIES)
				.param("search", search.toUpperCase())
				.param("page", "0")
				.param("size", "1"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.items.length()").value(1))
				.andExpect(jsonPath("$.data.totalElements").value(2))
				.andExpect(jsonPath("$.data.totalPages").value(2))
				.andExpect(jsonPath("$.data.items[0].isActive").value(true));

		mockMvc.perform(get(ADMIN_CATEGORIES)
				.header("Authorization", "Bearer " + admin.token())
				.param("search", search)
				.param("size", "10"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.totalElements").value(3));
	}

	@Test
	void rejectsDuplicateNamesSlugsAndInvalidCategoryData() throws Exception {
		TestUser admin = createUser(Role.SUPER_ADMIN);
		String suffix = UUID.randomUUID().toString();
		String name = "Unique " + suffix;
		createCategory(admin, name, "unique-" + suffix, true);

		mockMvc.perform(post(ADMIN_CATEGORIES)
				.header("Authorization", "Bearer " + admin.token())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"name":"%s","slug":"another-%s"}
						""".formatted(name, suffix)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.success").value(false));

		mockMvc.perform(post(ADMIN_CATEGORIES)
				.header("Authorization", "Bearer " + admin.token())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"name":"Another %s","slug":"unique-%s"}
						""".formatted(suffix, suffix)))
				.andExpect(status().isConflict());

		mockMvc.perform(post(ADMIN_CATEGORIES)
				.header("Authorization", "Bearer " + admin.token())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"name":" ","slug":"invalid_slug","imageUrl":"file:///local/image.png"}
						"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.success").value(false));

		mockMvc.perform(get(ADMIN_CATEGORIES)
				.header("Authorization", "Bearer " + admin.token())
				.param("page", "-1"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void onlySuperAdminsCanManageCategoriesAndBrowsingIsPublic() throws Exception {
		TestUser customer = createUser(Role.CUSTOMER);

		mockMvc.perform(get(PUBLIC_CATEGORIES))
				.andExpect(status().isOk());
		mockMvc.perform(get(ADMIN_CATEGORIES))
				.andExpect(status().isUnauthorized());
		mockMvc.perform(get(ADMIN_CATEGORIES)
				.header("Authorization", "Bearer " + customer.token()))
				.andExpect(status().isForbidden());
		mockMvc.perform(post(ADMIN_CATEGORIES)
				.header("Authorization", "Bearer " + customer.token())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"name":"Unauthorized category"}
						"""))
				.andExpect(status().isForbidden());
	}

	private long createCategory(TestUser admin, String name, String slug, boolean active) throws Exception {
		String slugValue = slug == null ? "" : slug;
		MvcResult result = mockMvc.perform(post(ADMIN_CATEGORIES)
				.header("Authorization", "Bearer " + admin.token())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"name":"%s","slug":"%s","description":"Test category","isActive":%s}
						""".formatted(name, slugValue, active)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.success").value(true))
				.andReturn();
		Number id = JsonPath.read(result.getResponse().getContentAsString(), "$.data.id");
		return id.longValue();
	}

	private TestUser createUser(Role role) {
		User user = new User(
				"Category Test Admin",
				"category-test-" + UUID.randomUUID() + "@example.test",
				passwordEncoder.encode(UUID.randomUUID().toString()));
		user.setRole(role);
		User saved = users.save(user);
		String token = jwtService.generateAccessToken(new CustomUserDetails(saved));
		return new TestUser(token);
	}

	private record TestUser(String token) {
	}
}
