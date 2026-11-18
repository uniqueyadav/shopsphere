package com.shopsphere.analytics.controller;

import com.shopsphere.analytics.dto.SellerDashboardResponse;
import com.shopsphere.analytics.dto.SellerEarningsResponse;
import com.shopsphere.analytics.service.SellerAnalyticsService;
import com.shopsphere.analytics.service.SellerEarningsService;
import com.shopsphere.common.response.ApiResponse;
import com.shopsphere.security.CustomUserDetails;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/seller/analytics")
@PreAuthorize("hasRole('SELLER')")
public class SellerAnalyticsController {
	private final SellerAnalyticsService sellerAnalyticsService;
	private final SellerEarningsService sellerEarningsService;

	public SellerAnalyticsController(SellerAnalyticsService sellerAnalyticsService, SellerEarningsService sellerEarningsService) {
		this.sellerAnalyticsService = sellerAnalyticsService;
		this.sellerEarningsService = sellerEarningsService;
	}

	@GetMapping("/dashboard")
	public ApiResponse<SellerDashboardResponse> dashboard(
			@AuthenticationPrincipal CustomUserDetails seller,
			@RequestParam(defaultValue = "THIRTY_DAYS") String period) {
		return ApiResponse.success("Seller dashboard loaded.", sellerAnalyticsService.getDashboard(seller.getId(), period));
	}

	@GetMapping("/earnings")
	public ApiResponse<SellerEarningsResponse> earnings(
			@AuthenticationPrincipal CustomUserDetails seller,
			@RequestParam(required = false) java.time.Instant from,
			@RequestParam(required = false) java.time.Instant to) {
		return ApiResponse.success("Seller earnings loaded.", sellerEarningsService.get(seller.getId(), from, to));
	}
}
