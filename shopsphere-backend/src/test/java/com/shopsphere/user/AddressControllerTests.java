package com.shopsphere.user;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import com.shopsphere.common.enums.Role;
import com.shopsphere.security.CustomUserDetails;
import com.shopsphere.security.JwtService;
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
class AddressControllerTests {

	private static final String ADDRESSES = "/api/v1/addresses";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ShopUserRepository users;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private JwtService jwtService;

	@Test
	void customerCanCreateReadUpdateSetDefaultAndDeleteAddresses() throws Exception {
		TestUser customer = createUser(Role.CUSTOMER);
		long homeId = createAddress(customer, "Home", "HOME", false);

		mockMvc.perform(get(ADDRESSES + "/" + homeId)
				.header("Authorization", bearer(customer)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.fullName").value("Home"))
				.andExpect(jsonPath("$.data.mobileNumber").value("9876543210"))
				.andExpect(jsonPath("$.data.houseFlatNumber").value("12B"))
				.andExpect(jsonPath("$.data.streetArea").value("Market Road"))
				.andExpect(jsonPath("$.data.landmark").value("Near Central Park"))
				.andExpect(jsonPath("$.data.city").value("Jaipur"))
				.andExpect(jsonPath("$.data.state").value("Rajasthan"))
				.andExpect(jsonPath("$.data.pinCode").value("302001"))
				.andExpect(jsonPath("$.data.addressType").value("HOME"))
				.andExpect(jsonPath("$.data.defaultAddress").value(true));

		long workId = createAddress(customer, "Work", "WORK", true);
		mockMvc.perform(get(ADDRESSES)
				.header("Authorization", bearer(customer)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.length()").value(2))
				.andExpect(jsonPath("$.data[0].id").value(workId))
				.andExpect(jsonPath("$.data[0].defaultAddress").value(true))
				.andExpect(jsonPath("$.data[1].defaultAddress").value(false));

		mockMvc.perform(put(ADDRESSES + "/" + workId)
				.header("Authorization", bearer(customer))
				.contentType(MediaType.APPLICATION_JSON)
				.content(addressPayload("Work updated", "OTHER", false, "9988776655", "400001")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.fullName").value("Work updated"))
				.andExpect(jsonPath("$.data.addressType").value("OTHER"))
				.andExpect(jsonPath("$.data.defaultAddress").value(false));

		mockMvc.perform(patch(ADDRESSES + "/" + homeId + "/default")
				.header("Authorization", bearer(customer)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.defaultAddress").value(true));
		mockMvc.perform(get(ADDRESSES)
				.header("Authorization", bearer(customer)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data[0].id").value(homeId))
				.andExpect(jsonPath("$.data[0].defaultAddress").value(true))
				.andExpect(jsonPath("$.data[1].defaultAddress").value(false));

		mockMvc.perform(delete(ADDRESSES + "/" + homeId)
				.header("Authorization", bearer(customer)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true));
		mockMvc.perform(get(ADDRESSES + "/" + homeId)
				.header("Authorization", bearer(customer)))
				.andExpect(status().isNotFound());
		mockMvc.perform(get(ADDRESSES)
				.header("Authorization", bearer(customer)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.length()").value(1))
				.andExpect(jsonPath("$.data[0].defaultAddress").value(true));
	}

	@Test
	void addressesArePrivateAndCustomerOnly() throws Exception {
		TestUser customer = createUser(Role.CUSTOMER);
		TestUser anotherCustomer = createUser(Role.CUSTOMER);
		TestUser seller = createUser(Role.SELLER);
		long addressId = createAddress(customer, "Private", "HOME", true);

		mockMvc.perform(get(ADDRESSES))
				.andExpect(status().isUnauthorized());
		mockMvc.perform(get(ADDRESSES)
				.header("Authorization", bearer(seller)))
				.andExpect(status().isForbidden());

		mockMvc.perform(get(ADDRESSES + "/" + addressId)
				.header("Authorization", bearer(anotherCustomer)))
				.andExpect(status().isNotFound());
		mockMvc.perform(put(ADDRESSES + "/" + addressId)
				.header("Authorization", bearer(anotherCustomer))
				.contentType(MediaType.APPLICATION_JSON)
				.content(addressPayload("Attempted edit", "HOME", true, "9876543210", "302001")))
				.andExpect(status().isNotFound());
		mockMvc.perform(patch(ADDRESSES + "/" + addressId + "/default")
				.header("Authorization", bearer(anotherCustomer)))
				.andExpect(status().isNotFound());
		mockMvc.perform(delete(ADDRESSES + "/" + addressId)
				.header("Authorization", bearer(anotherCustomer)))
				.andExpect(status().isNotFound());
	}

	@Test
	void acceptsLowercaseAddressTypeValues() throws Exception {
		TestUser customer = createUser(Role.CUSTOMER);
		mockMvc.perform(post(ADDRESSES)
				.header("Authorization", bearer(customer))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{"fullName":"Home", "mobileNumber":"9876543210", "houseFlatNumber":"12B",
					 "streetArea":"Market Road", "landmark":"Near Central Park",
					 "city":"Jaipur", "state":"Rajasthan", "pinCode":"302001",
					 "addressType":"home", "defaultAddress":true}
					"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.addressType").value("HOME"));
	}

	@Test
	void validatesIndianMobileAndPinCode() throws Exception {
		TestUser customer = createUser(Role.CUSTOMER);
		mockMvc.perform(post(ADDRESSES)
				.header("Authorization", bearer(customer))
				.contentType(MediaType.APPLICATION_JSON)
				.content(addressPayload("Invalid", "HOME", true, "12345", "012345")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.success").value(false));
	}

	private long createAddress(TestUser customer, String fullName, String type, boolean defaultAddress) throws Exception {
		MvcResult result = mockMvc.perform(post(ADDRESSES)
				.header("Authorization", bearer(customer))
				.contentType(MediaType.APPLICATION_JSON)
				.content(addressPayload(fullName, type, defaultAddress, "9876543210", "302001")))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.success").value(true))
				.andReturn();
		Number id = JsonPath.read(result.getResponse().getContentAsString(), "$.data.id");
		return id.longValue();
	}

	private String addressPayload(
			String fullName,
			String type,
			boolean defaultAddress,
			String mobileNumber,
			String pinCode) {
		return """
				{"fullName":"%s","mobileNumber":"%s","houseFlatNumber":"12B",
				 "streetArea":"Market Road","landmark":"Near Central Park",
				 "city":"Jaipur","state":"Rajasthan","pinCode":"%s",
				 "addressType":"%s","defaultAddress":%s}
				""".formatted(fullName, mobileNumber, pinCode, type, defaultAddress);
	}

	private TestUser createUser(Role role) {
		String suffix = UUID.randomUUID().toString();
		User user = new User(
				"Address Test User",
				"address-test-" + suffix + "@example.test",
				passwordEncoder.encode(suffix));
		user.setRole(role);
		User saved = users.save(user);
		String token = jwtService.generateAccessToken(new CustomUserDetails(saved));
		return new TestUser(token);
	}

	private String bearer(TestUser user) {
		return "Bearer " + user.token();
	}

	private record TestUser(String token) {
	}
}
