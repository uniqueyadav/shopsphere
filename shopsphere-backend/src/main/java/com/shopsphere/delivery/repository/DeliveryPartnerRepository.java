package com.shopsphere.delivery.repository;

import com.shopsphere.delivery.entity.DeliveryPartner;
import com.shopsphere.delivery.entity.PartnerStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface DeliveryPartnerRepository extends JpaRepository<DeliveryPartner,Long>, JpaSpecificationExecutor<DeliveryPartner> {
	@Override @EntityGraph(attributePaths="user") Page<DeliveryPartner> findAll(Specification<DeliveryPartner> spec,Pageable pageable);
	long countByStatus(PartnerStatus status);
	boolean existsByUser_Id(Long userId);
}
