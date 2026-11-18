package com.shopsphere.user.dto;

import java.time.Instant;

import com.shopsphere.common.enums.AccountStatus;
import com.shopsphere.common.enums.Role;
import com.shopsphere.user.entity.User;

public record UserResponse(
		Long id,
		String fullName,
		String email,
		String phone,
		Role role,
		AccountStatus accountStatus,
		Instant createdAt) {

	public static UserResponse from(User user) {
		return new UserResponse(user.getId(), user.getFullName(), user.getEmail(), user.getPhone(),
				user.getRole(), user.getAccountStatus(), user.getCreatedAt());
	}
}
