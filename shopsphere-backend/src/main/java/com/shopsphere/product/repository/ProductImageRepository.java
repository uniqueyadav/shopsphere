package com.shopsphere.product.repository;

import java.util.List;
import java.util.Optional;
import java.util.Collection;

import com.shopsphere.product.entity.ProductImage;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {

	List<ProductImage> findByProduct_IdOrderByDisplayOrderAscIdAsc(Long productId);

	List<ProductImage> findByProduct_IdInOrderByDisplayOrderAscIdAsc(Collection<Long> productIds);

	Optional<ProductImage> findByIdAndProduct_Id(Long id, Long productId);

	long countByProduct_Id(Long productId);
	void deleteByProduct_Id(Long productId);
}
