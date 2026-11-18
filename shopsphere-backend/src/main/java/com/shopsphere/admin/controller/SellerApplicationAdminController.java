package com.shopsphere.admin.controller;

import com.shopsphere.admin.dto.SellerApplicationDetailsResponse;
import com.shopsphere.admin.dto.SellerApplicationPageResponse;
import com.shopsphere.admin.dto.SellerRejectionRequest;
import com.shopsphere.admin.dto.SellerSuspensionRequest;
import com.shopsphere.admin.dto.UpdateSellerRequest;
import com.shopsphere.common.enums.AccountStatus;
import com.shopsphere.common.enums.BusinessType;
import com.shopsphere.admin.service.SellerApplicationAdminService;
import com.shopsphere.common.enums.SellerStatus;
import com.shopsphere.common.response.ApiResponse;
import com.shopsphere.security.CustomUserDetails;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;

import org.springframework.http.HttpStatus;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@Validated
@RestController
@PreAuthorize("hasRole('SUPER_ADMIN')")
@RequestMapping("/api/v1/admin/seller-applications")
public class SellerApplicationAdminController {

	private final SellerApplicationAdminService service;

	public SellerApplicationAdminController(SellerApplicationAdminService service) {
		this.service = service;
	}

	@GetMapping
	public ApiResponse<SellerApplicationPageResponse> listApplications(
			@RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "10") @Min(1) @Max(100) int size,
			@RequestParam(required = false) String search,
			@RequestParam(required = false) SellerStatus status,
			@RequestParam(required = false) BusinessType businessType,
			@RequestParam(required = false) AccountStatus accountStatus) {
		return ApiResponse.success("Seller applications retrieved.", service.listApplications(page, size, search, status, businessType, accountStatus));
	}

	@GetMapping("/export")
	public ResponseEntity<byte[]> exportApplications(
			@RequestParam(required = false) String search,
			@RequestParam(required = false) SellerStatus status,
			@RequestParam(required = false) BusinessType businessType,
			@RequestParam(required = false) AccountStatus accountStatus) {
		return ResponseEntity.ok()
				.contentType(MediaType.parseMediaType("text/csv"))
				.header(HttpHeaders.CONTENT_DISPOSITION,
						ContentDisposition.attachment().filename("shopsphere-sellers.csv").build().toString())
				.body(service.exportApplications(search, status, businessType, accountStatus));
	}

	@GetMapping("/{id}")
	public ApiResponse<SellerApplicationDetailsResponse> getApplication(@PathVariable @Positive Long id) {
		return ApiResponse.success("Seller application details retrieved.", service.getApplication(id));
	}

	@PutMapping("/{id}")
	public ApiResponse<SellerApplicationDetailsResponse> updateSeller(
			@PathVariable @Positive Long id,
			@Valid @RequestBody UpdateSellerRequest request) {
		return ApiResponse.success("Seller details updated.", service.updateSeller(id, request));
	}

	@PostMapping("/{id}/approve")
	public ApiResponse<SellerApplicationDetailsResponse> approve(
			@PathVariable @Positive Long id,
			@AuthenticationPrincipal CustomUserDetails reviewer) {
		return ApiResponse.success(
				"Seller application approved.",
				service.approve(id, requireReviewer(reviewer)));
	}

	@PostMapping("/{id}/reject")
	public ApiResponse<SellerApplicationDetailsResponse> reject(
			@PathVariable @Positive Long id,
			@Valid @RequestBody SellerRejectionRequest request,
			@AuthenticationPrincipal CustomUserDetails reviewer) {
		return ApiResponse.success(
				"Seller application rejected.",
				service.reject(id, requireReviewer(reviewer), request.rejectionReason()));
	}

	@PostMapping("/{id}/suspend")
	public ApiResponse<SellerApplicationDetailsResponse> suspend(
			@PathVariable @Positive Long id,
			@Valid @RequestBody SellerSuspensionRequest request,
			@AuthenticationPrincipal CustomUserDetails reviewer) {
		return ApiResponse.success("Seller suspended.",
				service.suspendSeller(id, requireReviewer(reviewer), request.reason()));
	}

	@PostMapping("/{id}/activate")
	public ApiResponse<SellerApplicationDetailsResponse> activate(
			@PathVariable @Positive Long id,
			@AuthenticationPrincipal CustomUserDetails reviewer) {
		return ApiResponse.success("Seller activated.", service.activateSeller(id, requireReviewer(reviewer)));
	}

	private Long requireReviewer(CustomUserDetails reviewer) {
		if (reviewer == null) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication is required.");
		}
		return reviewer.getId();
	}
}
