package com.shopsphere.inventory.controller;

import java.time.Instant;
import java.util.List;

import com.shopsphere.common.response.ApiResponse;
import com.shopsphere.inventory.dto.InventoryItemResponse;
import com.shopsphere.inventory.dto.InventoryPageResponse;
import com.shopsphere.inventory.dto.InventoryResponse;
import com.shopsphere.inventory.dto.InventorySummaryResponse;
import com.shopsphere.inventory.dto.StockUpdateRequest;
import com.shopsphere.inventory.service.InventoryService;
import com.shopsphere.security.CustomUserDetails;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@Validated
@RestController
@RequestMapping("/api/v1/seller/inventory")
public class InventoryController {
	private final InventoryService service;

	public InventoryController(InventoryService service) { this.service = service; }

	@GetMapping
	public ApiResponse<InventoryPageResponse> list(
			@AuthenticationPrincipal CustomUserDetails seller,
			@RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
			@RequestParam(required = false) String search,
			@RequestParam(required = false) @Positive Long categoryId,
			@RequestParam(required = false) String status,
			@RequestParam(required = false) String stock,
			@RequestParam(required = false) Instant updatedAfter,
			@RequestParam(required = false) Instant updatedBefore) {
		return ApiResponse.success("Seller inventory retrieved.", service.list(requireSeller(seller), page, size, search, categoryId, status, stock, updatedAfter, updatedBefore));
	}

	@GetMapping("/summary")
	public ApiResponse<InventorySummaryResponse> summary(@AuthenticationPrincipal CustomUserDetails seller) {
		return ApiResponse.success("Inventory summary retrieved.", service.summary(requireSeller(seller)));
	}

	@PutMapping("/{productId}/stock")
	public ApiResponse<InventoryItemResponse> updateStock(
			@AuthenticationPrincipal CustomUserDetails seller,
			@PathVariable @Positive Long productId,
			@Valid @RequestBody StockUpdateRequest request) {
		return ApiResponse.success("Inventory stock updated.", service.updateStock(requireSeller(seller), productId, request.stockQuantity()));
	}

	@GetMapping("/{productId}/history")
	public ApiResponse<List<InventoryResponse>> history(
			@AuthenticationPrincipal CustomUserDetails seller,
			@PathVariable @Positive Long productId) {
		return ApiResponse.success("Stock history retrieved.", service.history(requireSeller(seller), productId));
	}

	private Long requireSeller(CustomUserDetails seller) {
		if (seller == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication is required.");
		return seller.getId();
	}
}
