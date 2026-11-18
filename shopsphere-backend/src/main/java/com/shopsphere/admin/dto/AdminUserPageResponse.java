package com.shopsphere.admin.dto;

import java.util.List;

import com.shopsphere.user.dto.UserResponse;

public record AdminUserPageResponse(
		List<UserResponse> items,
		int page,
		int size,
		long totalElements,
		int totalPages) {
}
