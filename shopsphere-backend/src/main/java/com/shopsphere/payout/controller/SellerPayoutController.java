package com.shopsphere.payout.controller;

import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import com.shopsphere.common.response.ApiResponse;
import com.shopsphere.payout.dto.PayoutDtos.Summary;
import com.shopsphere.payout.dto.PayoutRequest;
import com.shopsphere.payout.service.PayoutService;
import com.shopsphere.security.CustomUserDetails;

@Validated
@RestController
@RequestMapping("/api/v1/seller/payouts")
@PreAuthorize("hasRole('SELLER')")
public class SellerPayoutController {
	private final PayoutService service;
	public SellerPayoutController(PayoutService service){this.service=service;}
	@GetMapping public ApiResponse<Summary> summary(@AuthenticationPrincipal CustomUserDetails seller,
			@RequestParam(required=false) Instant from,@RequestParam(required=false) Instant to,
			@RequestParam(defaultValue="0") @Min(0) int page,@RequestParam(defaultValue="10") @Min(1) @Max(100) int size){
		return ApiResponse.success("Seller payout information loaded.",service.summary(seller.getId(),from,to,page,size));
	}
	@PostMapping public org.springframework.http.ResponseEntity<ApiResponse<com.shopsphere.payout.dto.PayoutDtos.Transaction>> request(
			@AuthenticationPrincipal CustomUserDetails seller,@RequestHeader("Idempotency-Key") @NotBlank @Size(max=100) String idempotencyKey,
			@Valid @RequestBody PayoutRequest request){
		return org.springframework.http.ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Payout request submitted for processing.",service.request(seller.getId(),idempotencyKey,request)));
	}
}
