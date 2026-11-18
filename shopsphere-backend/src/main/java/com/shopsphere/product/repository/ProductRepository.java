package com.shopsphere.product.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.shopsphere.product.entity.Product;
import com.shopsphere.common.enums.ProductStatus;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

	long countByStatus(ProductStatus status);

	long countBySeller_Id(Long sellerId);
	long countBySeller_User_Id(Long userId);
	long countBySeller_User_IdAndStockQuantityGreaterThan(Long userId, int quantity);
	long countBySeller_User_IdAndStockQuantityGreaterThanAndStockQuantityLessThanEqual(Long userId, int minimum, int maximum);
	long countBySeller_User_IdAndStockQuantity(Long userId, int quantity);

	long countByCategory_Id(Long categoryId);

	@Query("select product.brand, count(product.id) from Product product where product.status = com.shopsphere.common.enums.ProductStatus.APPROVED and product.isActive = true and product.brand is not null and trim(product.brand) <> '' group by product.brand order by count(product.id) desc, product.brand asc")
	List<Object[]> findPublicBrandFacets();

	@Query("select product.category.id, product.category.name, count(product.id) from Product product where product.status = com.shopsphere.common.enums.ProductStatus.APPROVED and product.isActive = true group by product.category.id, product.category.name order by count(product.id) desc, product.category.name asc")
	List<Object[]> findPublicCategoryFacets();

	@Override
	@EntityGraph(attributePaths = {"seller", "category", "reviewedBy"})
	Page<Product> findAll(Specification<Product> specification, Pageable pageable);

	@Override
	@EntityGraph(attributePaths = {"seller", "category", "reviewedBy"})
	Optional<Product> findById(Long id);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@EntityGraph(attributePaths = {"seller", "category"})
	@Query("select product from Product product where product.id = :productId and product.seller.user.id = :userId")
	Optional<Product> findOwnedByIdForUpdate(@Param("productId") Long productId, @Param("userId") Long userId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@EntityGraph(attributePaths = "seller")
	@Query("select product from Product product where product.id in :productIds order by product.id")
	List<Product> findAllByIdInForUpdate(@Param("productIds") Collection<Long> productIds);

	@EntityGraph(attributePaths = {"seller", "category", "reviewedBy"})
	Optional<Product> findBySlugIgnoreCase(String slug);

	boolean existsBySlugIgnoreCase(String slug);

	boolean existsBySlugIgnoreCaseAndIdNot(String slug, Long id);

	boolean existsBySkuIgnoreCase(String sku);

	boolean existsBySkuIgnoreCaseAndIdNot(String sku, Long id);

}
