package com.shopsphere.product.dto;

import java.time.Instant;

import com.shopsphere.product.entity.ProductImage;

public record ProductImageResponse(
		Long id,
		String url,
		boolean isPrimary,
		int displayOrder,
		Instant createdAt) {

	public static ProductImageResponse from(ProductImage image) {
		return new ProductImageResponse(
				image.getId(),
				image.getUrl(),
				image.isPrimary(),
				image.getDisplayOrder(),
				image.getCreatedAt());
	}
}
