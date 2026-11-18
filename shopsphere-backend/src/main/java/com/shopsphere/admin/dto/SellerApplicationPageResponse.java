package com.shopsphere.admin.dto;

import java.util.List;

public record SellerApplicationPageResponse(
		List<SellerApplicationSummaryResponse> items,
		int page,
		int size,
		long totalElements,
		int totalPages,
		SellerApplicationCounts statusCounts) {
}
