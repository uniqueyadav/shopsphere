
package com.shopsphere.delivery.repository;

import java.time.Instant;
import java.util.List;
import com.shopsphere.delivery.entity.DeliveryAssignment;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeliveryAssignmentRepository extends JpaRepository<DeliveryAssignment, Long> {
    boolean existsBySellerOrder_Id(Long sellerOrderId);
    @EntityGraph(attributePaths = {"partner", "partner.user", "sellerOrder", "sellerOrder.order", "sellerOrder.seller"})
    List<DeliveryAssignment> findByPartner_User_IdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    @EntityGraph(attributePaths = {"partner", "sellerOrder", "sellerOrder.order", "sellerOrder.seller"})
    java.util.Optional<DeliveryAssignment> findByIdAndPartner_User_Id(Long id, Long userId);
    long countByPartner_User_IdAndCreatedAtGreaterThanEqual(Long userId, Instant from);
}
