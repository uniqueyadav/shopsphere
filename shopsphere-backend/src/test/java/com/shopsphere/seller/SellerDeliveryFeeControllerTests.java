package com.shopsphere.seller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.UUID;

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

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SellerDeliveryFeeControllerTests {

	private static final String DELIVERY_FEE = "/api/v1/seller/me/delivery-fee";

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
	void approvedSellerCanUpdateDeliveryFeeAndOtherRolesCannot() throws Exception {
		TestSeller approvedSeller = createSeller(SellerStatus.APPROVED);
		TestSeller pendingSeller = createSeller(SellerStatus.PENDING);
		TestSeller customer = createSeller(Role.CUSTOMER, SellerStatus.APPROVED);

		mockMvc.perform(get(DELIVERY_FEE)
				.header("Authorization", bearer(approvedSeller)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.deliveryFee").value(0));
		mockMvc.perform(put(DELIVERY_FEE)
				.header("Authorization", bearer(approvedSeller))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"deliveryFee\":19.95}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.deliveryFee").value(19.95));
		assertEquals(
				0,
				new BigDecimal("19.95").compareTo(
						sellers.findByUser_Id(approvedSeller.user().getId()).orElseThrow().getDeliveryFee()));

		mockMvc.perform(put(DELIVERY_FEE)
				.header("Authorization", bearer(approvedSeller))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"deliveryFee\":-1}"))
				.andExpect(status().isBadRequest());
		mockMvc.perform(get(DELIVERY_FEE)
				.header("Authorization", bearer(pendingSeller)))
				.andExpect(status().isForbidden());
		mockMvc.perform(get(DELIVERY_FEE)
				.header("Authorization", bearer(customer)))
				.andExpect(status().isForbidden());
		mockMvc.perform(get(DELIVERY_FEE))
				.andExpect(status().isUnauthorized());
	}

	private TestSeller createSeller(SellerStatus status) {
		return createSeller(Role.SELLER, status);
	}

	private TestSeller createSeller(Role role, SellerStatus status) {
		String suffix = UUID.randomUUID().toString();
		User user = new User(
				"Delivery Fee Seller",
				"delivery-fee-" + suffix + "@example.test",
				passwordEncoder.encode(suffix));
		user.setRole(role);
		User savedUser = users.save(user);
		Seller seller = new Seller(savedUser, "Delivery store " + suffix, "Test store", "Business address");
		seller.setApprovalStatus(status);
		Seller savedSeller = sellers.save(seller);
		String token = jwtService.generateAccessToken(new CustomUserDetails(savedUser));
		return new TestSeller(savedUser, savedSeller, token);
	}

	private String bearer(TestSeller seller) {
		return "Bearer " + seller.token();
	}

	private record TestSeller(User user, Seller seller, String token) {
	}
}
