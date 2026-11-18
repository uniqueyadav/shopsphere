package com.shopsphere.seller.dto;

import java.time.Instant;

import com.shopsphere.common.enums.SellerStatus;
import com.shopsphere.seller.entity.Seller;

public record SellerApplicationResponse(
		Long id,
		String businessName,
		SellerStatus status,
		Instant submittedAt) {

	public static SellerApplicationResponse from(Seller seller) {
		return new SellerApplicationResponse(
				seller.getId(),
				seller.getStoreName(),
				seller.getApprovalStatus(),
				seller.getCreatedAt());
	}
}
