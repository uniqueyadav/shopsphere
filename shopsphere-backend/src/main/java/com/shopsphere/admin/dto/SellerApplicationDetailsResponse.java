package com.shopsphere.admin.dto;

import java.time.Instant;
import java.util.List;

import com.shopsphere.common.enums.BusinessType;
import com.shopsphere.common.enums.SellerStatus;
import com.shopsphere.common.enums.AccountStatus;
import com.shopsphere.seller.entity.Seller;
import com.shopsphere.seller.entity.SellerDocument;
import com.shopsphere.user.entity.User;

public record SellerApplicationDetailsResponse(
		Long id,
		String businessName,
		String sellerName,
		String email,
		String phone,
		String businessAddress,
		BusinessType businessType,
		String businessDescription,
		String gstNumber,
		SellerStatus status,
		Instant submittedAt,
		Reviewer reviewedBy,
		Instant reviewedAt,
		String rejectionReason,
		AccountStatus accountStatus,
		String suspensionReason,
		long totalProducts,
		long totalOrders,
		List<SubmittedDocument> documents) {

	public static SellerApplicationDetailsResponse from(Seller seller, long totalProducts, long totalOrders) {
		User user = seller.getUser();
		User reviewer = seller.getReviewedBy();
		return new SellerApplicationDetailsResponse(
				seller.getId(),
				seller.getStoreName(),
				seller.getSellerFullName() == null ? user.getFullName() : seller.getSellerFullName(),
				seller.getSellerEmail() == null ? user.getEmail() : seller.getSellerEmail(),
				seller.getSellerPhone() == null ? user.getPhone() : seller.getSellerPhone(),
				seller.getBusinessAddress(),
				seller.getBusinessType(),
				seller.getStoreDescription(),
				seller.getGstNumber(),
				seller.getApprovalStatus(),
				seller.getCreatedAt(),
				reviewer == null ? null : new Reviewer(reviewer.getId(), reviewer.getFullName()),
				seller.getReviewedAt(),
				seller.getRejectionReason(),
				user.getAccountStatus(),
				seller.getSuspensionReason(),
				totalProducts,
				totalOrders,
				seller.getDocuments().stream()
						.map(SubmittedDocument::from)
						.toList());
	}

	public record Reviewer(Long id, String fullName) {
	}

	public record SubmittedDocument(String type, String fileName, String fileUrl) {
		public static SubmittedDocument from(SellerDocument document) {
			return new SubmittedDocument(
					document.getDocumentType(),
					document.getOriginalFileName(),
					document.getFileUrl());
		}
	}
}
