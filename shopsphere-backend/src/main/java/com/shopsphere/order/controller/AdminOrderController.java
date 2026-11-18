package com.shopsphere.order.controller;

import java.time.LocalDate;

import com.shopsphere.common.enums.OrderStatus;
import com.shopsphere.common.enums.PaymentStatus;
import com.shopsphere.common.response.ApiResponse;
import com.shopsphere.order.dto.AdminOrderPageResponse;
import com.shopsphere.order.dto.AdminOrderResponse;
import com.shopsphere.order.dto.AdminOrderStatsResponse;
import com.shopsphere.order.dto.OrderCancellationRequest;
import com.shopsphere.order.service.AdminOrderService;
import com.shopsphere.order.service.OrderStatusService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/admin/orders")
public class AdminOrderController {

	private final AdminOrderService service;
	private final OrderStatusService orderStatusService;

	public AdminOrderController(AdminOrderService service, OrderStatusService orderStatusService) {
		this.service = service;
		this.orderStatusService = orderStatusService;
	}

	@GetMapping
	public ApiResponse<AdminOrderPageResponse> list(
			@RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "10") @Min(1) @Max(100) int size,
			@RequestParam(required = false) @Size(max = 200) String search,
			@RequestParam(required = false) OrderStatus orderStatus,
			@RequestParam(required = false) PaymentStatus paymentStatus,
			@RequestParam(required = false) OrderStatus deliveryStatus,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
		return ApiResponse.success("Orders retrieved.",
				service.list(page, size, search, orderStatus, paymentStatus, deliveryStatus, from, to));
	}

	@GetMapping("/stats")
	public ApiResponse<AdminOrderStatsResponse> stats() {
		return ApiResponse.success("Order statistics retrieved.", service.stats());
	}

	@GetMapping(value = "/export", produces = "text/csv")
	public ResponseEntity<String> export(
			@RequestParam(required = false) @Size(max = 200) String search,
			@RequestParam(required = false) OrderStatus orderStatus,
			@RequestParam(required = false) PaymentStatus paymentStatus,
			@RequestParam(required = false) OrderStatus deliveryStatus,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=shopsphere-orders.csv")
				.contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
				.body(service.export(search, orderStatus, paymentStatus, deliveryStatus, from, to));
	}

	@GetMapping("/{orderId}")
	public ApiResponse<AdminOrderResponse> get(@PathVariable @Positive Long orderId) {
		return ApiResponse.success("Order retrieved.", service.get(orderId));
	}

	@PostMapping("/{orderId}/cancel")
	@ResponseStatus(HttpStatus.OK)
	public ApiResponse<AdminOrderResponse> cancel(
			@PathVariable @Positive Long orderId,
			@Valid @RequestBody OrderCancellationRequest request) {
		var cancellation = orderStatusService.cancelAdminOrder(orderId, request.reason());
		String message = cancellation.refundPending()
				? "Order cancelled. The payment refund is being processed."
				: "Order cancelled.";
		return ApiResponse.success(message, service.get(orderId));
	}
}
