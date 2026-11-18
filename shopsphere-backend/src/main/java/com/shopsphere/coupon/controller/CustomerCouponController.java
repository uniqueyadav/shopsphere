package com.shopsphere.coupon.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.shopsphere.common.response.ApiResponse;
import com.shopsphere.coupon.dto.CouponPreviewRequest;
import com.shopsphere.coupon.dto.CouponPreviewResponse;
import com.shopsphere.coupon.service.CustomerCouponPreviewService;
import com.shopsphere.security.CustomUserDetails;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/coupons")
@PreAuthorize("hasRole('CUSTOMER')")
public class CustomerCouponController {
	private final CustomerCouponPreviewService service;

	public CustomerCouponController(CustomerCouponPreviewService service) {
		this.service = service;
	}

	@PostMapping("/preview")
	public ApiResponse<CouponPreviewResponse> preview(
			@AuthenticationPrincipal CustomUserDetails customer,
			@Valid @RequestBody CouponPreviewRequest request) {
		return ApiResponse.success("Coupon applied.", service.preview(customer.getId(), request));
	}
}
