package com.shopsphere.inventory.dto;

import java.time.Instant;

import com.shopsphere.inventory.entity.Inventory;

public record InventoryResponse(
		Long id,
		Long productId,
		String productName,
		String sku,
		int previousQuantity,
		int newQuantity,
		String changeType,
		Long changedByUserId,
		String changedByName,
		Instant changedAt) {

	public static InventoryResponse from(Inventory event) {
		return new InventoryResponse(
				event.getId(), event.getProduct().getId(), event.getProduct().getProductName(), event.getProduct().getSku(),
				event.getPreviousQuantity(), event.getNewQuantity(), event.getChangeType(),
				event.getChangedBy() == null ? null : event.getChangedBy().getId(),
				event.getChangedBy() == null ? null : event.getChangedBy().getFullName(), event.getCreatedAt());
	}
}
