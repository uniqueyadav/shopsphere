package com.shopsphere.category.dto;

import java.util.List;

import org.springframework.data.domain.Page;

public record CategoryPageResponse(
		List<CategoryResponse> items,
		int page,
		int size,
		long totalElements,
		int totalPages) {

	public static CategoryPageResponse from(Page<CategoryResponse> page) {
		return new CategoryPageResponse(
				page.getContent(),
				page.getNumber(),
				page.getSize(),
				page.getTotalElements(),
				page.getTotalPages());
	}
}
