package com.shopsphere.inventory.repository;

import java.util.List;

import com.shopsphere.inventory.entity.Inventory;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {
	void deleteByProduct_Id(Long productId);
	Page<Inventory> findByProduct_IdAndProduct_Seller_User_IdOrderByCreatedAtDescIdDesc(Long productId, Long userId, Pageable pageable);
	List<Inventory> findTop100ByProduct_IdAndProduct_Seller_User_IdOrderByCreatedAtDescIdDesc(Long productId, Long userId);
}
