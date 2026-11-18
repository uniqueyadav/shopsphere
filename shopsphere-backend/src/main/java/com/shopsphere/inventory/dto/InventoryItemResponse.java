package com.shopsphere.inventory.dto;

import java.time.Instant;
import java.util.List;

import com.shopsphere.product.dto.ProductImageResponse;
import com.shopsphere.product.entity.Product;

public record InventoryItemResponse(
		Long id,
		String productName,
		Long categoryId,
		String categoryName,
		String sku,
		int stockQuantity,
		Instant createdAt,
		Instant updatedAt,
		List<ProductImageResponse> images) {

	public static InventoryItemResponse from(Product product, List<ProductImageResponse> images) {
		return new InventoryItemResponse(product.getId(), product.getProductName(), product.getCategory().getId(),
				product.getCategory().getName(), product.getSku(), product.getStockQuantity(),
				product.getCreatedAt(), product.getUpdatedAt(), images);
	}
}
