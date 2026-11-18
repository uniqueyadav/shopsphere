package com.shopsphere.order.repository;

import java.util.List;
import java.util.Optional;

import com.shopsphere.common.enums.OrderStatus;
import com.shopsphere.order.entity.SellerOrder;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.domain.Specification;

public interface SellerOrderRepository extends JpaRepository<SellerOrder, Long>, JpaSpecificationExecutor<SellerOrder> {

	long countBySeller_Id(Long sellerId);
	long countBySeller_User_Id(Long userId);
	long countBySeller_User_IdAndOrderStatus(Long userId, OrderStatus status);

	List<SellerOrder> findByOrder_IdOrderByIdAsc(Long orderId);

	Optional<SellerOrder> findByIdAndSeller_User_Id(Long sellerOrderId, Long sellerUserId);

	@EntityGraph(attributePaths = {"seller", "order", "order.customer", "items", "items.product", "statusHistory"})
	Optional<SellerOrder> findWithDetailsByIdAndSeller_User_Id(Long sellerOrderId, Long sellerUserId);

	@Override
	@EntityGraph(attributePaths = {"seller", "order", "order.customer", "items", "items.product", "statusHistory"})
	Page<SellerOrder> findAll(Specification<SellerOrder> specification, Pageable pageable);
}
