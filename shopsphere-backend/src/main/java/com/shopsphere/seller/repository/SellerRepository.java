package com.shopsphere.seller.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.shopsphere.common.enums.SellerStatus;
import com.shopsphere.common.enums.AccountStatus;
import com.shopsphere.seller.entity.Seller;

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

public interface SellerRepository extends JpaRepository<Seller, Long>, JpaSpecificationExecutor<Seller> {

	boolean existsByUser_Id(Long userId);

	boolean existsByUser_IdAndApprovalStatus(Long userId, SellerStatus approvalStatus);

	Optional<Seller> findByUser_Id(Long userId);

	@EntityGraph(attributePaths = "user")
	List<Seller> findAllByApprovalStatusAndUser_AccountStatusOrderByStoreNameAsc(
			SellerStatus approvalStatus, AccountStatus accountStatus);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select seller from Seller seller where seller.id in :sellerIds order by seller.id")
	List<Seller> findAllByIdInForUpdate(@Param("sellerIds") Collection<Long> sellerIds);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select seller from Seller seller where seller.user.id = :userId")
	Optional<Seller> findByUser_IdForUpdate(@Param("userId") Long userId);

	long countByApprovalStatus(SellerStatus approvalStatus);

	long countByUser_AccountStatus(AccountStatus accountStatus);

	long countByApprovalStatusAndUser_AccountStatus(SellerStatus approvalStatus, AccountStatus accountStatus);

	@Override
	@EntityGraph(attributePaths = "user")
	Page<Seller> findAll(Specification<Seller> specification, Pageable pageable);

	@EntityGraph(attributePaths = {"user", "reviewedBy"})
	Optional<Seller> findById(Long id);
}
