package com.shopsphere.user;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PasswordHasherTests {

	private final PasswordHasher passwordHasher = new PasswordHasher();

	@Test
	void hashesPasswordsWithUniqueSaltsAndChecksThem() {
		String firstHash = passwordHasher.hash("correct horse battery staple");
		String secondHash = passwordHasher.hash("correct horse battery staple");

		assertNotEquals(firstHash, secondHash);
		assertTrue(passwordHasher.matches("correct horse battery staple", firstHash));
		assertFalse(passwordHasher.matches("incorrect password", firstHash));
	}
}
