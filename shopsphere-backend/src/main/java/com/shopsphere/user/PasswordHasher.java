package com.shopsphere.user;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import org.springframework.stereotype.Component;

@Component
public class PasswordHasher {

	private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
	private static final int ITERATIONS = 210_000;
	private static final int SALT_LENGTH = 16;
	private static final int KEY_LENGTH = 256;
	private static final SecureRandom RANDOM = new SecureRandom();

	public String hash(String password) {
		byte[] salt = new byte[SALT_LENGTH];
		RANDOM.nextBytes(salt);
		byte[] derivedKey = derive(password, salt, ITERATIONS);
		return "pbkdf2$" + ITERATIONS + "$" + Base64.getEncoder().encodeToString(salt)
				+ "$" + Base64.getEncoder().encodeToString(derivedKey);
	}

	public boolean matches(String password, String encodedPassword) {
		String[] parts = encodedPassword.split("\\$", -1);
		if (parts.length != 4 || !"pbkdf2".equals(parts[0])) {
			return false;
		}

		try {
			int iterations = Integer.parseInt(parts[1]);
			byte[] salt = Base64.getDecoder().decode(parts[2]);
			byte[] expected = Base64.getDecoder().decode(parts[3]);
			if (iterations < 1 || iterations > 1_000_000 || salt.length < 8 || expected.length == 0) {
				return false;
			}
			return MessageDigest.isEqual(expected, derive(password, salt, iterations));
		}
		catch (IllegalArgumentException exception) {
			return false;
		}
	}

	private byte[] derive(String password, byte[] salt, int iterations) {
		PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, KEY_LENGTH);
		try {
			return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).getEncoded();
		}
		catch (GeneralSecurityException exception) {
			throw new IllegalStateException("Password hashing is unavailable", exception);
		}
		finally {
			spec.clearPassword();
		}
	}
}
