package com.shopsphere.security;

import java.time.Instant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

	private final JwtEncoder jwtEncoder;
	private final JwtDecoder jwtDecoder;
	private final long accessTokenExpirySeconds;
	private final long refreshTokenExpirySeconds;

	public JwtService(
			JwtEncoder jwtEncoder,
			JwtDecoder jwtDecoder,
			@Value("${app.jwt.access-token-expiry-seconds:86400}") long accessTokenExpirySeconds,
			@Value("${app.jwt.refresh-token-expiry-seconds:2592000}") long refreshTokenExpirySeconds) {
		if (accessTokenExpirySeconds <= 0) {
			throw new IllegalArgumentException("JWT access-token expiry must be greater than zero.");
		}
		if (refreshTokenExpirySeconds <= 0) {
			throw new IllegalArgumentException("JWT refresh-token expiry must be greater than zero.");
		}
		this.jwtEncoder = jwtEncoder;
		this.jwtDecoder = jwtDecoder;
		this.accessTokenExpirySeconds = accessTokenExpirySeconds;
		this.refreshTokenExpirySeconds = refreshTokenExpirySeconds;
	}

	public String generateAccessToken(CustomUserDetails userDetails) {
		Instant issuedAt = Instant.now();
		JwtClaimsSet claims = JwtClaimsSet.builder()
				.issuer("shopsphere")
				.subject(userDetails.getUsername())
				.issuedAt(issuedAt)
				.expiresAt(issuedAt.plusSeconds(accessTokenExpirySeconds))
				.claim("userId", userDetails.getId())
				.claim("token_use", "access")
				.build();
		JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
		return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
	}

	public String generateRefreshToken(CustomUserDetails userDetails) {
		Instant issuedAt = Instant.now();
		JwtClaimsSet claims = JwtClaimsSet.builder()
				.issuer("shopsphere")
				.subject(userDetails.getUsername())
				.issuedAt(issuedAt)
				.expiresAt(issuedAt.plusSeconds(refreshTokenExpirySeconds))
				.claim("userId", userDetails.getId())
				.claim("token_use", "refresh")
				.build();
		JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
		return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
	}

	public Jwt decodeRefreshToken(String token) {
		Jwt jwt = decodeToken(token);
		if (!"refresh".equals(jwt.getClaimAsString("token_use"))) {
			throw new BadJwtException("Refresh token type is invalid.");
		}
		return jwt;
	}

	public Jwt decodeToken(String token) {
		Jwt jwt = jwtDecoder.decode(token);
		if (jwt.getSubject() == null || jwt.getSubject().isBlank()) {
			throw new BadJwtException("Token subject is missing.");
		}
		return jwt;
	}

	public long getAccessTokenExpirySeconds() {
		return accessTokenExpirySeconds;
	}
}
