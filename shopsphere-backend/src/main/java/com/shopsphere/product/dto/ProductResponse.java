package com.shopsphere.product.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.shopsphere.common.enums.ProductStatus;
import com.shopsphere.product.entity.Product;

public record ProductResponse(
		Long id,
		Long sellerId,
		String storeName,
		Long categoryId,
		String categoryName,
		String productName,
		String slug,
		String description,
		String brand,
		String sku,
		BigDecimal price,
		BigDecimal discountPrice,
		int stockQuantity,
		ProductStatus status,
		boolean isActive,
		Long reviewedById,
		Instant reviewedAt,
		String rejectionReason,
		Instant createdAt,
		Instant updatedAt,
		List<ProductImageResponse> images,
		double averageRating,
		long reviewCount) {

	public static ProductResponse from(Product product) {
		return from(product, List.of());
	}

	public static ProductResponse from(Product product, List<ProductImageResponse> images) {
		return from(product, images, 0d, 0L);
	}

	public static ProductResponse from(Product product, List<ProductImageResponse> images, double averageRating, long reviewCount) {
		return new ProductResponse(
				product.getId(),
				product.getSeller().getId(),
				product.getSeller().getStoreName(),
				product.getCategory().getId(),
				product.getCategory().getName(),
				product.getProductName(),
				product.getSlug(),
				product.getDescription(),
				product.getBrand(),
				product.getSku(),
				product.getPrice(),
				product.getDiscountPrice(),
				product.getStockQuantity(),
				product.getStatus(),
				product.isActive(),
				product.getReviewedBy() == null ? null : product.getReviewedBy().getId(),
				product.getReviewedAt(),
				product.getRejectionReason(),
				product.getCreatedAt(),
				product.getUpdatedAt(),
				images,
				averageRating,
				reviewCount);
	}
}
