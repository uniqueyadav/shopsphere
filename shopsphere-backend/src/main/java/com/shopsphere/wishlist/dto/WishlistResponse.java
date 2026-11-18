package com.shopsphere.wishlist.dto;

import java.util.List;

import com.shopsphere.wishlist.entity.Wishlist;

public record WishlistResponse(Long id, List<WishlistItemResponse> items, int totalItems) {

	public static WishlistResponse from(Wishlist wishlist, List<WishlistItemResponse> items) {
		return new WishlistResponse(wishlist.getId(), items, items.size());
	}
}