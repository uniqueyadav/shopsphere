package com.shopsphere.cart.repository;

import java.util.Optional;

import com.shopsphere.cart.entity.Cart;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

public interface CartRepository extends JpaRepository<Cart, Long> {

	@EntityGraph(attributePaths = {"items", "items.product", "items.product.seller"})
	Optional<Cart> findByCustomer_Id(Long customerId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select cart from Cart cart where cart.customer.id = :customerId")
	Optional<Cart> findByCustomer_IdForUpdate(@Param("customerId") Long customerId);
}