package com.shopsphere.user.controller;

import com.shopsphere.admin.dto.AdminUserDetailsResponse;
import com.shopsphere.admin.dto.AdminUserPageResponse;
import com.shopsphere.admin.dto.AdminUserStatsResponse;
import com.shopsphere.admin.dto.UpdateUserStatusRequest;
import com.shopsphere.common.enums.AccountStatus;
import com.shopsphere.common.enums.Role;
import com.shopsphere.common.response.ApiResponse;
import com.shopsphere.user.dto.UpdateUserRequest;
import com.shopsphere.user.dto.UserResponse;
import com.shopsphere.user.service.UserService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@PreAuthorize("hasRole('SUPER_ADMIN')")
@RequestMapping("/api/v1/admin/users")
public class UserController {

	private final UserService service;

	public UserController(UserService service) {
		this.service = service;
	}

	@GetMapping
	public ApiResponse<AdminUserPageResponse> listUsers(
			@RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "10") @Min(1) @Max(100) int size,
			@RequestParam(required = false) String search,
			@RequestParam(required = false) Role role,
			@RequestParam(required = false) AccountStatus status) {
		return ApiResponse.success("Users retrieved.", service.listUsers(page, size, search, role, status));
	}

	@GetMapping("/stats")
	public ApiResponse<AdminUserStatsResponse> getStats() {
		return ApiResponse.success("User statistics retrieved.", service.getStats());
	}

	@GetMapping("/export")
	public ResponseEntity<byte[]> exportUsers(
			@RequestParam(required = false) String search,
			@RequestParam(required = false) Role role,
			@RequestParam(required = false) AccountStatus status) {
		return ResponseEntity.ok()
				.contentType(MediaType.parseMediaType("text/csv"))
				.header(HttpHeaders.CONTENT_DISPOSITION,
						ContentDisposition.attachment().filename("shopsphere-users.csv").build().toString())
				.body(service.exportUsers(search, role, status));
	}

	@GetMapping("/{id}")
	public ApiResponse<AdminUserDetailsResponse> getUser(@PathVariable @Positive Long id) {
		return ApiResponse.success("User details retrieved.", service.getUser(id));
	}

	@PatchMapping("/{id}")
	public ApiResponse<UserResponse> updateUser(
			@PathVariable @Positive Long id,
			@Valid @RequestBody UpdateUserRequest request) {
		return ApiResponse.success("User updated.", service.updateUser(id, request));
	}

	@PatchMapping("/{id}/status")
	public ApiResponse<UserResponse> updateStatus(
			@PathVariable @Positive Long id,
			@Valid @RequestBody UpdateUserStatusRequest request) {
		return ApiResponse.success("User status updated.", service.updateStatus(id, request.accountStatus()));
	}
}
