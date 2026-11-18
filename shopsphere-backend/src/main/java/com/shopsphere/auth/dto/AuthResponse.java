package com.shopsphere.auth.dto;

import com.shopsphere.common.enums.Role;
import com.shopsphere.user.entity.User;

public record AuthResponse(String accessToken, String tokenType, long expiresIn, UserInfo user, String refreshToken) {

	public static AuthResponse from(User user, String accessToken, long expiresIn, String refreshToken) {
		return new AuthResponse(accessToken, "Bearer", expiresIn, UserInfo.from(user), refreshToken);
	}

	public record UserInfo(Long id, String fullName, String email, String phone, Role role) {

		public static UserInfo from(User user) {
			return new UserInfo(user.getId(), user.getFullName(), user.getEmail(), user.getPhone(), user.getRole());
		}
	}
}
