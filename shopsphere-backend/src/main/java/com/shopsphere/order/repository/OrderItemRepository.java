package com.shopsphere.order.repository;

import com.shopsphere.order.entity.OrderItem;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
	long countByProduct_Id(Long productId);

	@EntityGraph(attributePaths = {"product", "sellerOrder", "sellerOrder.seller", "sellerOrder.order", "sellerOrder.statusHistory"})
	@Query("select distinct item from OrderItem item where item.sellerOrder.order.customer.id = :customerId and item.sellerOrder.orderStatus = com.shopsphere.common.enums.OrderStatus.DELIVERED")
	List<OrderItem> findDeliveredItemsByCustomerId(@Param("customerId") Long customerId);

	@EntityGraph(attributePaths = {"product", "sellerOrder", "sellerOrder.seller", "sellerOrder.order", "sellerOrder.statusHistory"})
	java.util.Optional<OrderItem> findByIdAndSellerOrder_Order_Customer_Id(Long id, Long customerId);
}
