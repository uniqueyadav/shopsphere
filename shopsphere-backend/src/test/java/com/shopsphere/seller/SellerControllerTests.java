package com.shopsphere.seller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import com.jayway.jsonpath.JsonPath;
import com.shopsphere.common.enums.Role;
import com.shopsphere.seller.entity.Seller;
import com.shopsphere.seller.repository.SellerRepository;
import com.shopsphere.user.ShopUserRepository;
import com.shopsphere.user.entity.User;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SellerControllerTests {

	private static final String VALID_APPLICATION = """
			{
			  "businessName":"Rivera Ceramics",
			  "sellerFullName":"Jamie Rivera",
			  "email":"seller@example.com",
			  "phone":"+91 98765 43210",
			  "businessAddress":"12 Market Road, Jaipur, Rajasthan, 302001",
			  "gstNumber":"22AAAAA0000A1Z5",
			  "businessType":"SOLE_PROPRIETORSHIP",
			  "businessDescription":"Handmade homeware and ceramics."
			}
			""";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private SellerRepository sellers;

	@Autowired
	private ShopUserRepository users;

	@Test
	void customerApplicationIsPersistedAsPendingWithoutExposingSensitiveFields() throws Exception {
		RegisteredCustomer customer = registerCustomer();

		MvcResult result = mockMvc.perform(post("/api/v1/seller/applications")
				.header("Authorization", "Bearer " + customer.token())
				.contentType(MediaType.APPLICATION_JSON)
				.content(VALID_APPLICATION))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.businessName").value("Rivera Ceramics"))
				.andExpect(jsonPath("$.data.status").value("PENDING"))
				.andExpect(jsonPath("$.data.sellerEmail").doesNotExist())
				.andExpect(jsonPath("$.data.gstNumber").doesNotExist())
				.andReturn();

		Number applicationIdValue = JsonPath.read(result.getResponse().getContentAsString(), "$.data.id");
		Long applicationId = applicationIdValue.longValue();
		Seller savedApplication = sellers.findById(applicationId).orElseThrow();
		assertEquals("22AAAAA0000A1Z5", savedApplication.getGstNumber());
		assertEquals("seller@example.com", savedApplication.getSellerEmail());
		assertEquals(customer.userId(), savedApplication.getUser().getId());
	}

	@Test
	void rejectsDuplicateInvalidAndUnauthenticatedApplications() throws Exception {
		RegisteredCustomer customer = registerCustomer();
		mockMvc.perform(post("/api/v1/seller/applications")
				.header("Authorization", "Bearer " + customer.token())
				.contentType(MediaType.APPLICATION_JSON)
				.content(VALID_APPLICATION))
				.andExpect(status().isCreated());

		mockMvc.perform(post("/api/v1/seller/applications")
				.header("Authorization", "Bearer " + customer.token())
				.contentType(MediaType.APPLICATION_JSON)
				.content(VALID_APPLICATION))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.success").value(false));

		mockMvc.perform(post("/api/v1/seller/applications")
				.header("Authorization", "Bearer " + customer.token())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"businessName":"","email":"invalid","businessType":"NOT_A_TYPE"}
						"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.success").value(false));

		mockMvc.perform(post("/api/v1/seller/applications")
				.contentType(MediaType.APPLICATION_JSON)
				.content(VALID_APPLICATION))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.success").value(false));
	}

	@Test
	void rejectsAuthenticatedNonCustomerAccounts() throws Exception {
		RegisteredCustomer customer = registerCustomer();
		User user = users.findById(customer.userId()).orElseThrow();
		user.setRole(Role.SELLER);
		users.save(user);

		mockMvc.perform(post("/api/v1/seller/applications")
				.header("Authorization", "Bearer " + customer.token())
				.contentType(MediaType.APPLICATION_JSON)
				.content(VALID_APPLICATION))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.success").value(false));

		assertFalse(sellers.existsByUser_Id(customer.userId()));
	}

	private RegisteredCustomer registerCustomer() throws Exception {
		String email = UUID.randomUUID() + "@example.com";
		MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"fullName":"Jamie Rivera","email":"%s","password":"correct horse battery staple"}
						""".formatted(email)))
				.andExpect(status().isCreated())
				.andReturn();

		String response = result.getResponse().getContentAsString();
		Number userId = JsonPath.read(response, "$.data.user.id");
		return new RegisteredCustomer(
				userId.longValue(),
				JsonPath.read(response, "$.data.accessToken"));
	}

	private record RegisteredCustomer(Long userId, String token) {
	}
}
