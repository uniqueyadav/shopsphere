package com.shopsphere.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import com.shopsphere.auth.dto.AuthResponse;
import com.shopsphere.auth.dto.LoginRequest;
import com.shopsphere.auth.dto.RegisterRequest;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.server.ResponseStatusException;

@SpringBootTest
@ActiveProfiles("test")
class AuthServiceTests {

	@Autowired
	private AuthService authService;

	@Autowired
	private ShopUserRepository users;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private PasswordHasher passwordHasher;

	@Test
	void registersAndAuthenticatesUserIgnoringEmailCase() {
		String email = UUID.randomUUID() + "@example.com";
		AuthResponse registered = authService.register(
				new RegisterRequest("Jamie Rivera", email, "correct horse battery staple", null));

		AuthResponse signedIn = authService.login(
				new LoginRequest(email.toUpperCase(), "correct horse battery staple"));

		assertEquals(registered.user().id(), signedIn.user().id());
		assertEquals("Jamie Rivera", signedIn.user().fullName());

		String storedHash = users.findByEmail(email).orElseThrow().getPasswordHash();
		assertNotEquals("correct horse battery staple", storedHash);
		assertTrue(passwordEncoder.matches("correct horse battery staple", storedHash));
	}

	@Test
	void rejectsIncorrectPassword() {
		String email = UUID.randomUUID() + "@example.com";
		authService.register(new RegisterRequest("Jamie Rivera", email, "correct horse battery staple", null));

		assertThrows(ResponseStatusException.class,
				() -> authService.login(new LoginRequest(email, "incorrect password")));
	}

	@Test
	void upgradesLegacyPasswordHashAfterSuccessfulLogin() {
		String email = UUID.randomUUID() + "@example.com";
		String rawPassword = "correct horse battery staple";
		users.saveAndFlush(new com.shopsphere.user.entity.User(
				"Jamie Rivera", email, passwordHasher.hash(rawPassword)));

		authService.login(new LoginRequest(email, rawPassword));

		String upgradedHash = users.findByEmail(email).orElseThrow().getPasswordHash();
		assertTrue(passwordEncoder.matches(rawPassword, upgradedHash));
	}
}
