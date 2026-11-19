package com.shopsphere.admin.dto;

import java.time.Instant;

import com.shopsphere.common.enums.SellerStatus;
import com.shopsphere.common.enums.AccountStatus;
import com.shopsphere.common.enums.BusinessType;
import com.shopsphere.seller.entity.Seller;

public record SellerApplicationSummaryResponse(
		Long id,
		String businessName,
		String sellerName,
		String email,
		String phone,
		String businessDescription,
		BusinessType businessType,
		SellerStatus status,
		AccountStatus accountStatus,
		Instant submittedAt,
		int documentCount) {

	public static SellerApplicationSummaryResponse from(Seller seller) {
		String sellerName = seller.getSellerFullName() == null || seller.getSellerFullName().isBlank()
				? seller.getUser().getFullName()
				: seller.getSellerFullName();
		String email = seller.getSellerEmail() == null || seller.getSellerEmail().isBlank()
				? seller.getUser().getEmail()
				: seller.getSellerEmail();
		String phone = seller.getSellerPhone() == null || seller.getSellerPhone().isBlank()
				? seller.getUser().getPhone()
				: seller.getSellerPhone();
		return new SellerApplicationSummaryResponse(
				seller.getId(),
				seller.getStoreName(),
				sellerName,
				email,
				phone,
				seller.getStoreDescription(),
				seller.getBusinessType(),
				seller.getApprovalStatus(),
				seller.getUser().getAccountStatus(),
				seller.getCreatedAt(),
				seller.getDocuments().size());
	}
}
