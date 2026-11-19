package com.shopsphere.order.repository;

import java.util.List;
import java.util.Optional;
import java.util.Collection;

import com.shopsphere.common.enums.OrderStatus;
import com.shopsphere.order.entity.Order;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.query.Param;

public interface OrderRepository extends JpaRepository<Order, Long>, JpaSpecificationExecutor<Order> {
	long countByCustomer_Id(Long customerId);

	long countByOrderStatus(OrderStatus orderStatus);

	@Query("select distinct o from Order o where o.id in :orderIds")
	@EntityGraph(attributePaths = {
			"customer", "sellerOrders", "sellerOrders.seller", "sellerOrders.items",
			"sellerOrders.items.product"
	})
	List<Order> findAllAdminDetailsByIdIn(@Param("orderIds") Collection<Long> orderIds);

	@EntityGraph(attributePaths = {"sellerOrders", "sellerOrders.seller", "sellerOrders.items", "sellerOrders.items.product"})
	Optional<Order> findByCustomer_IdAndIdempotencyKey(Long customerId, String idempotencyKey);

	@EntityGraph(attributePaths = {
			"sellerOrders",
			"sellerOrders.seller",
			"sellerOrders.items",
			"sellerOrders.items.product"
	})
	Optional<Order> findByIdAndCustomer_Id(Long orderId, Long customerId);

	@EntityGraph(attributePaths = {"sellerOrders", "sellerOrders.seller", "sellerOrders.statusHistory"})
	Optional<Order> findTrackingByIdAndCustomer_Id(Long orderId, Long customerId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select o from Order o where o.id = :orderId and o.customer.id = :customerId")
	Optional<Order> findByIdAndCustomer_IdForUpdate(
			@Param("orderId") Long orderId,
			@Param("customerId") Long customerId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select o from Order o where o.id = :orderId")
	Optional<Order> findByIdForUpdate(@Param("orderId") Long orderId);

	@EntityGraph(attributePaths = {"sellerOrders", "sellerOrders.seller", "sellerOrders.items", "sellerOrders.items.product"})
	List<Order> findByCustomer_IdOrderByCreatedAtDescIdDesc(Long customerId);
}
