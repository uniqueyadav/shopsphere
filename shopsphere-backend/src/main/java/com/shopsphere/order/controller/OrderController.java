package com.shopsphere.order.controller;

import java.util.List;

import com.shopsphere.common.response.ApiResponse;
import com.shopsphere.order.dto.CancellationResponse;
import com.shopsphere.order.dto.OrderResponse;
import com.shopsphere.order.dto.OrderTrackingResponse;
import com.shopsphere.order.service.OrderService;
import com.shopsphere.order.service.OrderStatusService;
import com.shopsphere.security.CustomUserDetails;

import jakarta.validation.constraints.Positive;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/orders")
@PreAuthorize("hasRole('CUSTOMER')")
public class OrderController {

	private final OrderService orderService;
	private final OrderStatusService orderStatusService;

	public OrderController(OrderService orderService, OrderStatusService orderStatusService) {
		this.orderService = orderService;
		this.orderStatusService = orderStatusService;
	}

	@GetMapping("/my-orders")
	public ApiResponse<List<OrderResponse>> getMyOrders(
			@AuthenticationPrincipal CustomUserDetails customer) {
		return ApiResponse.success("Orders retrieved.", orderService.getMyOrders(customer.getId()));
	}

	@GetMapping("/{orderId}")
	public ApiResponse<OrderResponse> getOrder(
			@AuthenticationPrincipal CustomUserDetails customer,
			@PathVariable @Positive Long orderId) {
		return ApiResponse.success("Order retrieved.", orderService.getOrder(customer.getId(), orderId));
	}

	@GetMapping("/{orderId}/tracking")
	public ApiResponse<OrderTrackingResponse> getTracking(
			@AuthenticationPrincipal CustomUserDetails customer,
			@PathVariable @Positive Long orderId) {
		return ApiResponse.success(
				"Order tracking retrieved.",
				orderStatusService.getTracking(customer.getId(), orderId));
	}

	@PostMapping("/{orderId}/cancel")
	public ApiResponse<CancellationResponse> cancelOrder(
			@AuthenticationPrincipal CustomUserDetails customer,
			@PathVariable @Positive Long orderId) {
		CancellationResponse result = orderStatusService.cancelCustomerOrder(customer.getId(), orderId);
		String message = result.refundPending()
				? "Order cancelled. Your refund is being processed."
				: "Order cancelled.";
		return ApiResponse.success(message, result);
	}
}