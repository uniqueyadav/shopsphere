package com.shopsphere.product.dto;

import java.util.List;

import org.springframework.data.domain.Page;

public record ProductPageResponse(
		List<ProductResponse> items,
		int page,
		int size,
		long totalElements,
		int totalPages) {

	public static ProductPageResponse from(Page<ProductResponse> page) {
		return new ProductPageResponse(
				page.getContent(),
				page.getNumber(),
				page.getSize(),
				page.getTotalElements(),
				page.getTotalPages());
	}
}
