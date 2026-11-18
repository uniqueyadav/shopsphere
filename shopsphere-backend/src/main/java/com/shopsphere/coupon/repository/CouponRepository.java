package com.shopsphere.coupon.repository;

import java.time.Instant;
import java.util.Optional;

import com.shopsphere.coupon.entity.Coupon;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CouponRepository extends JpaRepository<Coupon, Long>, JpaSpecificationExecutor<Coupon> {
	Optional<Coupon> findByCodeIgnoreCase(String code);
	boolean existsByCodeIgnoreCase(String code);
	@Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select c from Coupon c where upper(c.code)=upper(:code)") Optional<Coupon> findByCodeForUpdate(@Param("code") String code);
	@Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select c from Coupon c where c.id=:id") Optional<Coupon> findByIdForUpdate(@Param("id") Long id);
	long countByArchivedFalse();
	long countByArchivedFalseAndActiveTrueAndStartsAtLessThanEqualAndExpiresAtGreaterThan(Instant now1, Instant now2);
	long countByArchivedFalseAndExpiresAtLessThanEqual(Instant now);
	@Query("select count(c) from Coupon c where c.archived=false and c.active=false and c.expiresAt>:now") long countInactiveNotExpired(@Param("now") Instant now);
	long countByArchivedFalseAndActiveTrueAndStartsAtAfter(Instant now);
}
