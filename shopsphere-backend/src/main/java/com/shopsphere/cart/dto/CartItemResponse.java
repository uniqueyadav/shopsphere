package com.shopsphere.cart.dto;

import java.math.BigDecimal;

import com.shopsphere.cart.entity.CartItem;
import com.shopsphere.common.enums.ProductStatus;
import com.shopsphere.product.entity.Product;

public record CartItemResponse(
		Long id,
		Long productId,
		String productName,
		Long sellerId,
		String sellerName,
		BigDecimal sellerDeliveryFee,
		String brand,
		String sku,
		String imageUrl,
		int quantity,
		BigDecimal unitPrice,
		BigDecimal lineSubtotal,
		int stockQuantity,
		boolean available) {

	public static CartItemResponse from(CartItem item, String imageUrl) {
		Product product = item.getProduct();
		BigDecimal unitPrice = product.getDiscountPrice() == null
				? product.getPrice()
				: product.getDiscountPrice();
		return new CartItemResponse(
				item.getId(),
				product.getId(),
				product.getProductName(),
				product.getSeller().getId(),
				product.getSeller().getStoreName(),
				product.getSeller().getDeliveryFee(),
				product.getBrand(),
				product.getSku(),
				imageUrl,
				item.getQuantity(),
				unitPrice,
				unitPrice.multiply(BigDecimal.valueOf(item.getQuantity())),
				product.getStockQuantity(),
				product.getStatus() == ProductStatus.APPROVED
						&& product.isActive()
						&& product.getStockQuantity() >= item.getQuantity());
	}
}
