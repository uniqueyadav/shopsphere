package com.shopsphere.category.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CategoryRequest(
		@NotBlank(message = "Category name is required.")
		@Size(max = 100, message = "Category name must be 100 characters or fewer.")
		String name,
		@Size(max = 120, message = "Category slug must be 120 characters or fewer.")
		@Pattern(
				regexp = "(?iu)^(?:|[\\p{L}\\p{N}]+(?:-[\\p{L}\\p{N}]+)*)$",
				message = "Category slug may contain only letters, numbers, and hyphens.")
		String slug,
		@Size(max = 1000, message = "Category description must be 1,000 characters or fewer.")
		String description,
		@Size(max = 2048, message = "Image URL must be 2,048 characters or fewer.")
		@Pattern(regexp = "(?i)^(?:|https?://\\S+)$", message = "Image URL must use HTTP or HTTPS.")
		String imageUrl,
		@Size(max = 255, message = "Image storage ID must be 255 characters or fewer.")
		String imagePublicId,
		@jakarta.validation.constraints.Positive Long parentId,
		Boolean isActive) {
}
