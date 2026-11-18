package com.shopsphere.user;

import com.shopsphere.auth.dto.AuthResponse;
import com.shopsphere.auth.dto.LoginRequest;
import com.shopsphere.auth.dto.RegisterRequest;
import com.shopsphere.auth.dto.RefreshTokenRequest;
import com.shopsphere.common.response.ApiResponse;
import com.shopsphere.security.CustomUserDetails;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthController {

	private final AuthService authService;

	public AuthController(AuthService authService) {
		this.authService = authService;
	}

	@PostMapping("/api/v1/auth/register")
	public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
		AuthResponse response = authService.register(request);
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(ApiResponse.success("Account registered successfully.", response));
	}

	@PostMapping("/api/v1/auth/login")
	public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
		return ApiResponse.success("Login successful.", authService.login(request));
	}

	@PostMapping("/api/v1/auth/refresh")
	public ApiResponse<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
		return ApiResponse.success("Session refreshed.", authService.refresh(request));
	}

	@GetMapping("/api/v1/users/me")
	public ApiResponse<AuthResponse.UserInfo> currentUser(
			@AuthenticationPrincipal CustomUserDetails userDetails) {
		return ApiResponse.success("Current user retrieved successfully.",
				AuthResponse.UserInfo.from(userDetails.getUser()));
	}
}
