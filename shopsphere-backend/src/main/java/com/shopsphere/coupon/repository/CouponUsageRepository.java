package com.shopsphere.coupon.repository;

import java.math.BigDecimal;
import java.util.List;

import com.shopsphere.coupon.entity.CouponUsage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CouponUsageRepository extends JpaRepository<CouponUsage, Long> {
	long countByCoupon_Id(Long couponId);
	long countByCoupon_IdAndCustomer_Id(Long couponId, Long customerId);
	Page<CouponUsage> findByCoupon_IdOrderByCreatedAtDesc(Long couponId, Pageable pageable);
	@Query("select coalesce(sum(u.discountAmount),0) from CouponUsage u where u.coupon.id=:couponId") BigDecimal sumDiscountByCouponId(@Param("couponId") Long couponId);
}
