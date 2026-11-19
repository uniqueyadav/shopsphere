package com.shopsphere.delivery.repository;

import com.shopsphere.delivery.entity.DeliveryPartner;
import com.shopsphere.delivery.entity.PartnerStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.EntityGraph;

public interface DeliveryPartnerRepository extends JpaRepository<DeliveryPartner,Long>, JpaSpecificationExecutor<DeliveryPartner> {
	@EntityGraph(attributePaths="user") java.util.Optional<DeliveryPartner> findByUser_Id(Long userId);
	@Override @EntityGraph(attributePaths="user") Page<DeliveryPartner> findAll(Specification<DeliveryPartner> spec,Pageable pageable);
	long countByStatus(PartnerStatus status);
	boolean existsByUser_Id(Long userId);
}
