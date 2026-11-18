package com.shopsphere.product.dto;

import com.shopsphere.seller.entity.Seller;

public record AdminSellerOptionResponse(Long id, String storeName, String sellerName, String email) {
	public static AdminSellerOptionResponse from(Seller seller) {
		return new AdminSellerOptionResponse(
				seller.getId(), seller.getStoreName(), seller.getSellerFullName(), seller.getSellerEmail());
	}
}
