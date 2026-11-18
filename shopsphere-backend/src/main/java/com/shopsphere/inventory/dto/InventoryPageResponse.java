package com.shopsphere.inventory.dto;

import java.util.List;

import org.springframework.data.domain.Page;

public record InventoryPageResponse(List<InventoryItemResponse> items, int page, int size, long totalElements, int totalPages) {
	public static InventoryPageResponse from(Page<InventoryItemResponse> page) {
		return new InventoryPageResponse(page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
	}
}
