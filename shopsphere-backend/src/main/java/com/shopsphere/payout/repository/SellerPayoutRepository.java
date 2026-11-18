package com.shopsphere.payout.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import com.shopsphere.payout.entity.SellerPayout;
import com.shopsphere.payout.entity.PayoutStatus;

public interface SellerPayoutRepository extends JpaRepository<SellerPayout,Long> {
	Optional<SellerPayout> findBySeller_IdAndIdempotencyKey(Long sellerId,String key);
	List<SellerPayout> findBySeller_IdAndStatusIn(Long sellerId,List<PayoutStatus> statuses);
	Page<SellerPayout> findBySeller_IdAndCreatedAtBetween(Long sellerId,Instant from,Instant to,Pageable pageable);
	@Query("select coalesce(sum(p.amount),0) from SellerPayout p where p.seller.id=:sellerId and p.status=:status")
	java.math.BigDecimal sumBySellerAndStatus(@Param("sellerId") Long sellerId,@Param("status") PayoutStatus status);
	@Query("select coalesce(sum(p.amount),0) from SellerPayout p where p.seller.id=:sellerId and p.status in :statuses")
	java.math.BigDecimal sumBySellerAndStatuses(@Param("sellerId") Long sellerId,@Param("statuses") List<PayoutStatus> statuses);
	@Query("select max(p.createdAt) from SellerPayout p where p.seller.id=:sellerId")
	Instant lastRequestAt(@Param("sellerId") Long sellerId);
	@Query("select max(p.createdAt) from SellerPayout p where p.seller.id=:sellerId and p.status<>:rejected")
	Instant lastOpenOrPaidRequestAt(@Param("sellerId") Long sellerId,@Param("rejected") PayoutStatus rejected);
	@Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select p from SellerPayout p where p.id=:id") Optional<SellerPayout> findByIdForUpdate(@Param("id") Long id);
	@org.springframework.data.jpa.repository.EntityGraph(attributePaths={"seller","account"})
	Page<SellerPayout> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
