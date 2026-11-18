package com.shopsphere.review.repository;

import java.util.Optional;
import java.util.Collection;
import java.util.List;
import com.shopsphere.common.enums.ReviewStatus;
import com.shopsphere.review.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ReviewRepository extends JpaRepository<Review, Long>, JpaSpecificationExecutor<Review> {
	@Override @EntityGraph(attributePaths={"product","product.category","product.seller","customer","moderatedBy"}) Page<Review> findAll(org.springframework.data.jpa.domain.Specification<Review> spec, Pageable pageable);
	@Override @EntityGraph(attributePaths={"product","product.category","product.seller","customer","moderatedBy"}) Optional<Review> findById(Long id);
	long countByStatus(ReviewStatus status);
	void deleteByProduct_Id(Long productId);
	long countByRatingGreaterThanEqual(int rating);
	long countByRating(int rating);
	long countByProduct_IdAndStatus(Long productId, ReviewStatus status);
	@org.springframework.data.jpa.repository.Query("select avg(r.rating) from Review r where r.product.id=:productId and r.status=com.shopsphere.common.enums.ReviewStatus.APPROVED") Double averageForProduct(@org.springframework.data.repository.query.Param("productId") Long productId);
	@org.springframework.data.jpa.repository.Query("select r.product.id, avg(r.rating), count(r.id) from Review r where r.status=com.shopsphere.common.enums.ReviewStatus.APPROVED and r.product.id in :productIds group by r.product.id")
	List<Object[]> ratingSummaries(@org.springframework.data.repository.query.Param("productIds") Collection<Long> productIds);
	@org.springframework.data.jpa.repository.Query("select r.product.id from Review r where r.status=com.shopsphere.common.enums.ReviewStatus.APPROVED group by r.product.id having avg(r.rating) >= :minimumRating")
	List<Long> productIdsWithAverageRatingAtLeast(@org.springframework.data.repository.query.Param("minimumRating") double minimumRating);
	@org.springframework.data.jpa.repository.Query("select r.rating, count(r.id) from Review r where r.product.id=:productId and r.status=com.shopsphere.common.enums.ReviewStatus.APPROVED group by r.rating")
	List<Object[]> ratingDistribution(@org.springframework.data.repository.query.Param("productId") Long productId);
}
