package com.shopsphere.returnrequest.controller;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.shopsphere.common.response.ApiResponse;
import com.shopsphere.returnrequest.dto.CreateReturnRequest;
import com.shopsphere.returnrequest.dto.EligibleReturnItemResponse;
import com.shopsphere.returnrequest.dto.ReturnRequestResponse;
import com.shopsphere.returnrequest.service.CustomerReturnRequestService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import com.shopsphere.security.CustomUserDetails;

@Validated
@RestController
@RequestMapping("/api/v1/returns")
@PreAuthorize("hasRole('CUSTOMER')")
public class CustomerReturnRequestController {
	private final CustomerReturnRequestService service;
	public CustomerReturnRequestController(CustomerReturnRequestService service) { this.service = service; }

	@GetMapping("/eligible-items")
	public ApiResponse<List<EligibleReturnItemResponse>> eligible(@AuthenticationPrincipal CustomUserDetails customer) {
		return ApiResponse.success("Eligible items loaded.", service.eligibleItems(customer.getId()));
	}
	@GetMapping
	public ApiResponse<List<ReturnRequestResponse>> list(@AuthenticationPrincipal CustomUserDetails customer) {
		return ApiResponse.success("Return requests loaded.", service.list(customer.getId()));
	}
	@PostMapping
	public ApiResponse<ReturnRequestResponse> create(@AuthenticationPrincipal CustomUserDetails customer, @Valid @RequestBody CreateReturnRequest request) {
		return ApiResponse.success("Return request submitted.", service.create(customer.getId(), request));
	}
	@GetMapping("/{requestId}")
	public ApiResponse<ReturnRequestResponse> get(@AuthenticationPrincipal CustomUserDetails customer, @PathVariable @Positive Long requestId) {
		return ApiResponse.success("Return request loaded.", service.get(customer.getId(), requestId));
	}
	@PostMapping("/{requestId}/cancel")
	public ApiResponse<ReturnRequestResponse> cancel(@AuthenticationPrincipal CustomUserDetails customer, @PathVariable @Positive Long requestId) {
		return ApiResponse.success("Return request cancelled.", service.cancel(customer.getId(), requestId));
	}
}
