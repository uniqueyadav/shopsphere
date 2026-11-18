package com.shopsphere.order.dto;

import java.util.List;
import org.springframework.data.domain.Page;

public record SellerOrderManagementPageResponse(List<SellerOrderManagementResponse> items, int page, int size, long totalElements, int totalPages) {
	public static SellerOrderManagementPageResponse from(Page<SellerOrderManagementResponse> page) {
		return new SellerOrderManagementPageResponse(page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
	}
}
