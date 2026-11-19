package com.shopsphere.seller.controller;

import com.shopsphere.common.response.ApiResponse;
import com.shopsphere.security.CustomUserDetails;
import com.shopsphere.seller.dto.SellerApplicationRequest;
import com.shopsphere.seller.dto.SellerApplicationResponse;
import com.shopsphere.seller.dto.SellerDeliveryFeeRequest;
import com.shopsphere.seller.dto.SellerDeliveryFeeResponse;
import com.shopsphere.seller.service.SellerService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/seller")
public class SellerController {

	private final SellerService sellerService;

	public SellerController(SellerService sellerService) {
		this.sellerService = sellerService;
	}

	@PostMapping(value = "/applications", consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResponse<SellerApplicationResponse>> applyForSeller(
			@AuthenticationPrincipal CustomUserDetails principal,
			@Valid @RequestBody SellerApplicationRequest request) {
		return submit(principal, request, null);
	}

	@PostMapping(value = "/applications", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<ApiResponse<SellerApplicationResponse>> applyForSeller(
			@AuthenticationPrincipal CustomUserDetails principal,
			@Valid @RequestPart("application") SellerApplicationRequest request,
			@RequestParam(value = "businessPan", required = false) MultipartFile businessPan,
			@RequestParam(value = "gstCertificate", required = false) MultipartFile gstCertificate,
			@RequestParam(value = "addressProof", required = false) MultipartFile addressProof,
			@RequestParam(value = "identityProof", required = false) MultipartFile identityProof) {
		MultipartFile[] documents = { businessPan, gstCertificate, addressProof, identityProof };
		return submit(principal, request, documents);
	}

	private ResponseEntity<ApiResponse<SellerApplicationResponse>> submit(
			CustomUserDetails principal,
			SellerApplicationRequest request,
			MultipartFile[] documents) {
		if (principal == null) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication is required.");
		}
		return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
				"Seller application submitted for review.",
				sellerService.applyForSeller(principal.getId(), request, documents)));
	}

	@GetMapping("/me/delivery-fee")
	@PreAuthorize("hasRole('SELLER')")
	public ApiResponse<SellerDeliveryFeeResponse> getDeliveryFee(
			@AuthenticationPrincipal CustomUserDetails principal) {
		return ApiResponse.success("Seller delivery fee retrieved.", sellerService.getDeliveryFee(principal.getId()));
	}

	@PutMapping("/me/delivery-fee")
	@PreAuthorize("hasRole('SELLER')")
	public ApiResponse<SellerDeliveryFeeResponse> updateDeliveryFee(
			@AuthenticationPrincipal CustomUserDetails principal,
			@Valid @RequestBody SellerDeliveryFeeRequest request) {
		return ApiResponse.success("Seller delivery fee updated.", sellerService.updateDeliveryFee(principal.getId(), request));
	}
}
