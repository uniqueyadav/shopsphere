package com.shopsphere.wishlist.repository;

import java.util.Optional;

import com.shopsphere.wishlist.entity.Wishlist;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WishlistRepository extends JpaRepository<Wishlist, Long> {

	@EntityGraph(attributePaths = {"items", "items.product", "items.product.seller"})
	Optional<Wishlist> findByCustomer_Id(Long customerId);
}