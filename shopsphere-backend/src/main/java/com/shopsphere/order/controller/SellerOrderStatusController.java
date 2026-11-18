package com.shopsphere.order.controller;

import com.shopsphere.common.response.ApiResponse;
import com.shopsphere.order.dto.OrderStatusRequest;
import com.shopsphere.order.dto.SellerOrderTrackingResponse;
import com.shopsphere.order.dto.SellerOrderManagementPageResponse;
import com.shopsphere.order.dto.SellerOrderSummaryResponse;
import com.shopsphere.order.dto.SellerOrderManagementResponse;
import com.shopsphere.order.service.OrderStatusService;
import com.shopsphere.security.CustomUserDetails;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Size;

import java.time.Instant;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/seller/orders")
@PreAuthorize("hasRole('SELLER')")
public class SellerOrderStatusController {

	private final OrderStatusService orderStatusService;

	public SellerOrderStatusController(OrderStatusService orderStatusService) {
		this.orderStatusService = orderStatusService;
	}

	@GetMapping
	public ApiResponse<SellerOrderManagementPageResponse> listOrders(
			@AuthenticationPrincipal CustomUserDetails seller,
			@RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
			@RequestParam(required = false) @Size(max = 200) String search,
			@RequestParam(required = false) String status,
			@RequestParam(required = false) Instant from,
			@RequestParam(required = false) Instant to) {
		return ApiResponse.success("Seller orders retrieved.",
				orderStatusService.listSellerOrders(seller.getId(), page, size, search, status, from, to));
	}

	@GetMapping("/summary")
	public ApiResponse<SellerOrderSummaryResponse> summary(@AuthenticationPrincipal CustomUserDetails seller) {
		return ApiResponse.success("Seller order summary retrieved.", orderStatusService.sellerOrderSummary(seller.getId()));
	}

	@GetMapping("/{sellerOrderId}")
	public ApiResponse<SellerOrderManagementResponse> getOrderDetails(
			@AuthenticationPrincipal CustomUserDetails seller,
			@PathVariable @Positive Long sellerOrderId) {
		return ApiResponse.success("Seller order details retrieved.",
				orderStatusService.getSellerOrderDetails(seller.getId(), sellerOrderId));
	}

	@PatchMapping("/{sellerOrderId}/status")
	public ApiResponse<SellerOrderTrackingResponse> updateStatus(
			@AuthenticationPrincipal CustomUserDetails seller,
			@PathVariable @Positive Long sellerOrderId,
			@Valid @RequestBody OrderStatusRequest request) {
		return ApiResponse.success(
				"Order status updated.",
				orderStatusService.updateSellerStatus(seller.getId(), sellerOrderId, request.status()));
	}
}
