package com.shopsphere.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import com.jayway.jsonpath.JsonPath;

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
class AuthControllerTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void registersCustomerLogsInAndReadsProtectedProfile() throws Exception {
		String email = UUID.randomUUID() + "@example.com";
		String registration = """
				{"fullName":"Jamie Rivera","email":"%s","password":"correct horse battery staple","role":"SUPER_ADMIN"}
				""".formatted(email);

		MvcResult registrationResult = mockMvc.perform(post("/api/v1/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content(registration))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.user.role").value("CUSTOMER"))
				.andExpect(jsonPath("$.data.user.passwordHash").doesNotExist())
				.andExpect(jsonPath("$.data.user.password").doesNotExist())
				.andReturn();
		String accessToken = JsonPath.read(registrationResult.getResponse().getContentAsString(),
				"$.data.accessToken");

		mockMvc.perform(get("/api/v1/users/me")
				.header("Authorization", "Bearer " + accessToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.email").value(email))
				.andExpect(jsonPath("$.data.role").value("CUSTOMER"));

		mockMvc.perform(post("/api/v1/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"email":"%s","password":"correct horse battery staple"}
						""".formatted(email)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.accessToken").isNotEmpty());
	}

	@Test
	void rejectsDuplicateRegistrationAndInvalidCredentials() throws Exception {
		String email = UUID.randomUUID() + "@example.com";
		String registration = """
				{"fullName":"Jamie Rivera","email":"%s","password":"correct horse battery staple"}
				""".formatted(email);
		mockMvc.perform(post("/api/v1/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content(registration))
				.andExpect(status().isCreated());

		mockMvc.perform(post("/api/v1/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content(registration))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.success").value(false));

		mockMvc.perform(post("/api/v1/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"email":"%s","password":"incorrect password"}
						""".formatted(email)))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.success").value(false));
	}

	@Test
	void protectsProfileAndRejectsTamperedTokensAndInsufficientRoles() throws Exception {
		mockMvc.perform(get("/api/v1/users/me"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.success").value(false));

		String email = UUID.randomUUID() + "@example.com";
		MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"fullName":"Jamie Rivera","email":"%s","password":"correct horse battery staple"}
						""".formatted(email)))
				.andExpect(status().isCreated())
				.andReturn();
		String accessToken = JsonPath.read(result.getResponse().getContentAsString(), "$.data.accessToken");

		mockMvc.perform(get("/api/v1/users/me")
				.header("Authorization", "Bearer " + accessToken + "tampered"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.success").value(false));

		mockMvc.perform(get("/api/v1/admin/users")
				.header("Authorization", "Bearer " + accessToken))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.success").value(false));
	}

	@Test
	void validatesRegistrationPayload() throws Exception {
		mockMvc.perform(post("/api/v1/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"fullName":"","email":"not-an-email","password":"short"}
						"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.success").value(false));
	}
}
