package com.shopsphere.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.nullValue;

import java.util.UUID;

import com.jayway.jsonpath.JsonPath;
import com.shopsphere.common.enums.Role;
import com.shopsphere.common.enums.SellerStatus;
import com.shopsphere.security.CustomUserDetails;
import com.shopsphere.security.JwtService;
import com.shopsphere.seller.entity.Seller;
import com.shopsphere.seller.repository.SellerRepository;
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

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SellerApplicationAdminControllerTests {

	private static final String ADMIN_APPLICATIONS = "/api/v1/admin/seller-applications";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ShopUserRepository users;

	@Autowired
	private SellerRepository sellers;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private JwtService jwtService;

	@Test
	void listsApplicationsWithSearchStatusPaginationAndGlobalCounts() throws Exception {
		long beforeTotal = sellers.count();
		long beforePending = sellers.countByApprovalStatus(SellerStatus.PENDING);
		long beforeApproved = sellers.countByApprovalStatus(SellerStatus.APPROVED);
		long beforeRejected = sellers.countByApprovalStatus(SellerStatus.REJECTED);
		TestUser admin = createUser(Role.SUPER_ADMIN, "Marketplace Admin");
		String searchTerm = "needle-" + UUID.randomUUID().toString().substring(0, 8);
		createApplication("Needle " + searchTerm + " Studio One");
		createApplication("needle " + searchTerm + " Studio Two");

		mockMvc.perform(get(ADMIN_APPLICATIONS)
				.header("Authorization", "Bearer " + admin.token())
				.param("search", searchTerm)
				.param("status", "PENDING")
				.param("page", "1")
				.param("size", "1"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.page").value(1))
				.andExpect(jsonPath("$.data.size").value(1))
				.andExpect(jsonPath("$.data.totalElements").value(2))
				.andExpect(jsonPath("$.data.totalPages").value(2))
				.andExpect(jsonPath("$.data.items.length()").value(1))
				.andExpect(jsonPath("$.data.items[0].status").value("PENDING"))
				.andExpect(jsonPath("$.data.statusCounts.total").value((int) beforeTotal + 2))
				.andExpect(jsonPath("$.data.statusCounts.pending").value((int) beforePending + 2))
				.andExpect(jsonPath("$.data.statusCounts.approved").value((int) beforeApproved))
				.andExpect(jsonPath("$.data.statusCounts.rejected").value((int) beforeRejected));

		mockMvc.perform(get(ADMIN_APPLICATIONS)
				.header("Authorization", "Bearer " + admin.token())
				.param("status", "NOT_A_STATUS"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.success").value(false));
	}

	@Test
	void superAdminCanViewAndApproveAndOnlyApprovedSellersReachSellerApis() throws Exception {
		TestUser admin = createUser(Role.SUPER_ADMIN, "Marketplace Admin");
		ApplicationRecord record = createApplication("Rivera Ceramics");

		mockMvc.perform(get("/api/v1/seller/applications")
				.header("Authorization", "Bearer " + record.owner().token()))
				.andExpect(status().isForbidden());

		mockMvc.perform(get(ADMIN_APPLICATIONS + "/" + record.id())
				.header("Authorization", "Bearer " + admin.token()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.businessName").value(record.businessName()))
				.andExpect(jsonPath("$.data.businessType").value("SOLE_PROPRIETORSHIP"))
				.andExpect(jsonPath("$.data.gstNumber").value("22AAAAA0000A1Z5"))
				.andExpect(jsonPath("$.data.status").value("PENDING"))
				.andExpect(jsonPath("$.data.reviewedBy").value(nullValue()));

		mockMvc.perform(post(ADMIN_APPLICATIONS + "/" + record.id() + "/approve")
				.header("Authorization", "Bearer " + admin.token()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.status").value("APPROVED"))
				.andExpect(jsonPath("$.data.reviewedBy.fullName").value("Marketplace Admin"))
				.andExpect(jsonPath("$.data.reviewedAt").isNotEmpty())
				.andExpect(jsonPath("$.data.rejectionReason").value(nullValue()))
				.andExpect(jsonPath("$.data.reviewedBy.passwordHash").doesNotExist());

		Seller reviewed = sellers.findById(record.id()).orElseThrow();
		assertEquals(SellerStatus.APPROVED, reviewed.getApprovalStatus());
		assertEquals(Role.SELLER, reviewed.getUser().getRole());
		assertEquals(admin.id(), reviewed.getReviewedBy().getId());
		assertNotNull(reviewed.getReviewedAt());

		mockMvc.perform(get("/api/v1/seller/applications")
				.header("Authorization", "Bearer " + record.owner().token()))
				.andExpect(status().isMethodNotAllowed());

		mockMvc.perform(post(ADMIN_APPLICATIONS + "/" + record.id() + "/reject")
				.header("Authorization", "Bearer " + admin.token())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"rejectionReason":"This seller does not meet the requirements."}
						"""))
				.andExpect(status().isConflict());
	}

	@Test
	void rejectionRequiresReasonAndNonSuperAdminsCannotReview() throws Exception {
		TestUser admin = createUser(Role.SUPER_ADMIN, "Marketplace Admin");
		ApplicationRecord record = createApplication("Northwind Home Goods");

		mockMvc.perform(get(ADMIN_APPLICATIONS)
				.header("Authorization", "Bearer " + record.owner().token()))
				.andExpect(status().isForbidden());
		mockMvc.perform(get(ADMIN_APPLICATIONS))
				.andExpect(status().isUnauthorized());
		mockMvc.perform(post(ADMIN_APPLICATIONS + "/" + record.id() + "/approve")
				.header("Authorization", "Bearer " + record.owner().token()))
				.andExpect(status().isForbidden());

		mockMvc.perform(post(ADMIN_APPLICATIONS + "/" + record.id() + "/reject")
				.header("Authorization", "Bearer " + admin.token())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"rejectionReason":"   "}
						"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.success").value(false));

		mockMvc.perform(post(ADMIN_APPLICATIONS + "/" + record.id() + "/reject")
				.header("Authorization", "Bearer " + admin.token())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"rejectionReason":"Please provide clearer business details."}
						"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.status").value("REJECTED"))
				.andExpect(jsonPath("$.data.rejectionReason").value("Please provide clearer business details."))
				.andExpect(jsonPath("$.data.reviewedBy.fullName").value("Marketplace Admin"))
				.andExpect(jsonPath("$.data.reviewedAt").isNotEmpty());

		Seller reviewed = sellers.findById(record.id()).orElseThrow();
		assertEquals(SellerStatus.REJECTED, reviewed.getApprovalStatus());
		assertEquals("Please provide clearer business details.", reviewed.getRejectionReason());
		assertEquals(Role.CUSTOMER, reviewed.getUser().getRole());
		assertEquals(admin.id(), reviewed.getReviewedBy().getId());
		assertNotNull(reviewed.getReviewedAt());
	}

	private ApplicationRecord createApplication(String businessName) throws Exception {
		TestUser owner = createUser(Role.CUSTOMER, "Seller Applicant");
		String contactEmail = "seller-contact-" + UUID.randomUUID() + "@example.test";
		MvcResult result = mockMvc.perform(post("/api/v1/seller/applications")
				.header("Authorization", "Bearer " + owner.token())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "businessName":"%s",
						  "sellerFullName":"Seller Applicant",
						  "email":"%s",
						  "phone":"+91 98765 43210",
						  "businessAddress":"12 Market Road, Jaipur, Rajasthan, 302001",
						  "gstNumber":"22AAAAA0000A1Z5",
						  "businessType":"SOLE_PROPRIETORSHIP",
						  "businessDescription":"Handmade homeware and ceramics."
						}
						""".formatted(businessName, contactEmail)))
				.andExpect(status().isCreated())
				.andReturn();
		Number id = JsonPath.read(result.getResponse().getContentAsString(), "$.data.id");
		return new ApplicationRecord(id.longValue(), businessName, owner);
	}

	private TestUser createUser(Role role, String fullName) {
		User user = new User(
				fullName,
				"admin-test-" + UUID.randomUUID() + "@example.test",
				passwordEncoder.encode(UUID.randomUUID().toString()));
		user.setRole(role);
		User saved = users.save(user);
		String token = jwtService.generateAccessToken(new CustomUserDetails(saved));
		return new TestUser(saved.getId(), token);
	}

	private record TestUser(Long id, String token) {
	}

	private record ApplicationRecord(Long id, String businessName, TestUser owner) {
	}
}
