package com.shopsphere.order.dto;

import java.util.List;

import org.springframework.data.domain.Page;

public record AdminOrderPageResponse(List<AdminOrderResponse> items, int page, int size, long totalElements, int totalPages) {
	public static AdminOrderPageResponse from(Page<AdminOrderResponse> page) {
		return new AdminOrderPageResponse(page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
	}
}
