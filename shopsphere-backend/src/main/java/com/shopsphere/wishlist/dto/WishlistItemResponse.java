package com.shopsphere.wishlist.dto;

import java.math.BigDecimal;
import java.time.Instant;

import com.shopsphere.common.enums.ProductStatus;
import com.shopsphere.product.entity.Product;
import com.shopsphere.wishlist.entity.WishlistItem;

public record WishlistItemResponse(
		Long productId,
		String productName,
		String slug,
		String sellerName,
		String brand,
		String imageUrl,
		BigDecimal price,
		BigDecimal discountPrice,
		int stockQuantity,
		boolean available,
		double averageRating,
		long reviewCount,
		Instant addedAt) {

	public static WishlistItemResponse from(WishlistItem item, String imageUrl, double averageRating, long reviewCount) {
		Product product = item.getProduct();
		return new WishlistItemResponse(
				product.getId(),
				product.getProductName(),
				product.getSlug(),
				product.getSeller().getStoreName(),
				product.getBrand(),
				imageUrl,
				product.getPrice(),
				product.getDiscountPrice(),
				product.getStockQuantity(),
				product.getStatus() == ProductStatus.APPROVED
						&& product.isActive()
						&& product.getStockQuantity() > 0,
				averageRating,
				reviewCount,
				item.getCreatedAt());
	}
}
