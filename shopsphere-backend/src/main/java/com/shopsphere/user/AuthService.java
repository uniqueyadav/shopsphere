package com.shopsphere.user;

import java.util.Locale;

import com.shopsphere.auth.dto.AuthResponse;
import com.shopsphere.auth.dto.LoginRequest;
import com.shopsphere.auth.dto.RegisterRequest;
import com.shopsphere.auth.dto.RefreshTokenRequest;
import com.shopsphere.common.enums.AccountStatus;
import com.shopsphere.common.enums.Role;
import com.shopsphere.security.CustomUserDetails;
import com.shopsphere.security.JwtService;
import com.shopsphere.user.entity.User;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.oauth2.jwt.JwtException;

@Service
public class AuthService {

	private final ShopUserRepository users;
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;
	private final PasswordHasher legacyPasswordHasher;

	public AuthService(
			ShopUserRepository users,
			PasswordEncoder passwordEncoder,
			JwtService jwtService,
			PasswordHasher legacyPasswordHasher) {
		this.users = users;
		this.passwordEncoder = passwordEncoder;
		this.jwtService = jwtService;
		this.legacyPasswordHasher = legacyPasswordHasher;
	}

	@Transactional
	public AuthResponse register(RegisterRequest request) {
		String normalizedEmail = normalizeEmail(request.email());
		if (users.existsByEmail(normalizedEmail)) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "An account with this email already exists.");
		}

		User user = new User(
				request.fullName().trim(),
				normalizedEmail,
				passwordEncoder.encode(request.password()));
		if (request.phone() != null) {
			user.setPhone(request.phone().trim());
		}
		user.setRole(Role.CUSTOMER);
		user.setAccountStatus(AccountStatus.ACTIVE);
		return createAuthResponse(users.save(user));
	}

	@Transactional
	public AuthResponse login(LoginRequest request) {
		User user = users.findByEmail(normalizeEmail(request.email()))
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Email or password is incorrect."));
		if (!passwordMatchesAndUpgrade(user, request.password())) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Email or password is incorrect.");
		}
		if (user.getAccountStatus() != AccountStatus.ACTIVE) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This account is not active.");
		}
		return createAuthResponse(user);
	}

	@Transactional(readOnly = true)
	public AuthResponse refresh(RefreshTokenRequest request) {
		try {
			String email = jwtService.decodeRefreshToken(request.refreshToken()).getSubject();
			User user = users.findByEmail(normalizeEmail(email))
					.filter(candidate -> candidate.getAccountStatus() == AccountStatus.ACTIVE)
					.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Session has expired. Please sign in again."));
			return createAuthResponse(user);
		} catch (JwtException exception) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Session has expired. Please sign in again.");
		}
	}

	private boolean passwordMatchesAndUpgrade(User user, String rawPassword) {
		String storedPassword = user.getPasswordHash();
		if (storedPassword.startsWith("pbkdf2$")) {
			if (!legacyPasswordHasher.matches(rawPassword, storedPassword)) {
				return false;
			}
			user.setPasswordHash(passwordEncoder.encode(rawPassword));
			return true;
		}
		return passwordEncoder.matches(rawPassword, storedPassword);
	}

	private AuthResponse createAuthResponse(User user) {
		CustomUserDetails userDetails = new CustomUserDetails(user);
		String accessToken = jwtService.generateAccessToken(userDetails);
		String refreshToken = jwtService.generateRefreshToken(userDetails);
		return AuthResponse.from(user, accessToken, jwtService.getAccessTokenExpirySeconds(), refreshToken);
	}

	private String normalizeEmail(String email) {
		return email.trim().toLowerCase(Locale.ROOT);
	}
}
