package com.shopsphere.config;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nimbusds.jose.proc.SecurityContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

@Configuration
public class JwtConfig {

	private static final Logger logger = LoggerFactory.getLogger(JwtConfig.class);

	@Bean
	SecretKey jwtSecretKey(@Value("${app.jwt.secret-base64:}") String configuredSecret, Environment environment) {
		if (configuredSecret.isBlank()) {
			if (environment.acceptsProfiles(Profiles.of("prod"))) {
				throw new IllegalStateException("JWT_SECRET must be configured in the production profile.");
			}
			logger.warn("JWT_SECRET is unset; using the stable local development key. Configure JWT_SECRET outside development.");
			return new SecretKeySpec(
					"ShopSphere-local-development-only-jwt-signing-key-2026".getBytes(StandardCharsets.UTF_8),
					"HmacSHA256");
		}

		byte[] keyBytes;
		try {
			keyBytes = Base64.getDecoder().decode(configuredSecret);
		}
		catch (IllegalArgumentException exception) {
			throw new IllegalStateException("JWT_SECRET must be a valid Base64-encoded key.", exception);
		}
		if (keyBytes.length < 32) {
			throw new IllegalStateException("JWT_SECRET must decode to at least 32 bytes.");
		}
		return new SecretKeySpec(keyBytes, "HmacSHA256");
	}

	@Bean
	JwtEncoder jwtEncoder(SecretKey jwtSecretKey) {
		return new NimbusJwtEncoder(new ImmutableSecret<SecurityContext>(jwtSecretKey));
	}

	@Bean
	JwtDecoder jwtDecoder(SecretKey jwtSecretKey) {
		return NimbusJwtDecoder.withSecretKey(jwtSecretKey)
				.macAlgorithm(MacAlgorithm.HS256)
				.build();
	}
}
