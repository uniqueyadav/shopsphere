package com.shopsphere.returnrequest.repository;

import java.util.List;
import java.util.Optional;
import java.util.Collection;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.shopsphere.common.enums.ReturnRequestStatus;
import com.shopsphere.returnrequest.entity.CustomerReturnRequest;

public interface CustomerReturnRequestRepository extends JpaRepository<CustomerReturnRequest, Long> {
	@EntityGraph(attributePaths = {"customer", "orderItem", "orderItem.sellerOrder", "orderItem.sellerOrder.order"})
	List<CustomerReturnRequest> findByCustomer_IdOrderByCreatedAtDescIdDesc(Long customerId);

	@EntityGraph(attributePaths = {"customer", "orderItem", "orderItem.sellerOrder", "orderItem.sellerOrder.order"})
	Optional<CustomerReturnRequest> findByIdAndCustomer_Id(Long id, Long customerId);

	@EntityGraph(attributePaths = {"customer", "orderItem", "orderItem.sellerOrder", "orderItem.sellerOrder.order"})
	Optional<CustomerReturnRequest> findById(Long id);

	boolean existsByCustomer_IdAndOrderItem_IdAndStatusIn(Long customerId, Long orderItemId, Collection<ReturnRequestStatus> statuses);

	@EntityGraph(attributePaths = {"customer", "orderItem", "orderItem.sellerOrder", "orderItem.sellerOrder.order"})
	List<CustomerReturnRequest> findAllByOrderByCreatedAtDescIdDesc();
}
