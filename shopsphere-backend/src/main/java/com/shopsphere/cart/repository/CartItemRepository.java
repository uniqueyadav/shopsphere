package com.shopsphere.cart.repository;

import java.util.Optional;

import com.shopsphere.cart.entity.CartItem;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {
	void deleteByProduct_Id(Long productId);

	@EntityGraph(attributePaths = {"cart", "product", "product.seller"})
	Optional<CartItem> findByCart_IdAndProduct_Id(Long cartId, Long productId);

	@EntityGraph(attributePaths = {"cart", "product", "product.seller"})
	Optional<CartItem> findByIdAndCart_Customer_Id(Long itemId, Long customerId);
}
