package com.shopsphere.category.dto;

import java.time.Instant;
import java.util.List;

public record CategoryResponse(
		Long id,
		String name,
		String slug,
		String description,
		String imageUrl,
		String imagePublicId,
		Long parentId,
		String parentName,
		boolean isActive,
		Instant createdAt,
		Instant updatedAt,
		long productCount,
		long subcategoryCount,
		List<String> subcategories) {
}
