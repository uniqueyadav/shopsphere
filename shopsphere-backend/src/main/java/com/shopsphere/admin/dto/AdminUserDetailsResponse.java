package com.shopsphere.admin.dto;

import com.shopsphere.common.enums.SellerStatus;
import com.shopsphere.user.dto.UserResponse;

public record AdminUserDetailsResponse(
		UserResponse user,
		String storeName,
		String businessAddress,
		String businessType,
		SellerStatus sellerStatus) {
}
