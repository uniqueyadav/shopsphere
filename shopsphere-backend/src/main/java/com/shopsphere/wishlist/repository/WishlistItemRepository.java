package com.shopsphere.wishlist.repository;

import java.util.Optional;

import com.shopsphere.wishlist.entity.WishlistItem;

import org.springframework.data.jpa.repository.JpaRepository;

public interface WishlistItemRepository extends JpaRepository<WishlistItem, Long> {
	void deleteByProduct_Id(Long productId);

	Optional<WishlistItem> findByWishlist_IdAndProduct_Id(Long wishlistId, Long productId);
}
