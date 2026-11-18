package com.shopsphere.payout.controller;

import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import com.shopsphere.common.response.ApiResponse;
import com.shopsphere.payout.dto.PayoutDtos.AdminPayout;
import com.shopsphere.payout.dto.PayoutStatusRequest;
import com.shopsphere.payout.service.PayoutService;

@Validated
@RestController
@RequestMapping("/api/v1/admin/payouts")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class AdminPayoutController {
	private final PayoutService service;
	public AdminPayoutController(PayoutService service){this.service=service;}
	@GetMapping public ApiResponse<Page<AdminPayout>> list(@RequestParam(defaultValue="0") @Min(0) int page,@RequestParam(defaultValue="20") @Min(1) @Max(100) int size){return ApiResponse.success("Payout requests loaded.",service.adminList(page,size));}
	@PatchMapping("/{payoutId}/status") public ApiResponse<AdminPayout> update(@PathVariable @Min(1) Long payoutId,@Valid @RequestBody PayoutStatusRequest request){return ApiResponse.success("Payout request updated.",service.updateStatus(payoutId,request));}
}
