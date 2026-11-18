package com.shopsphere.commission.controller;

import java.math.BigDecimal;
import com.shopsphere.common.response.ApiResponse;
import com.shopsphere.commission.dto.CommissionDtos.UpdateRequest;
import com.shopsphere.commission.service.CommissionService;
import com.shopsphere.security.CustomUserDetails;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/v1/admin/commission") @PreAuthorize("hasRole('SUPER_ADMIN')")
public class CommissionAdminController {
	private final CommissionService service;
	public CommissionAdminController(CommissionService service){this.service=service;}
	@GetMapping("/rules") public ApiResponse<?> rules(){return ApiResponse.success("Commission rules loaded.",service.list());}
	@GetMapping("/platform") public ApiResponse<?> platform(){return ApiResponse.success("Platform commission loaded.",service.currentPlatform());}
	@PutMapping("/platform") public ApiResponse<?> updatePlatform(@Valid @RequestBody UpdateRequest request,@AuthenticationPrincipal CustomUserDetails admin){return ApiResponse.success("Platform commission updated.",service.updatePlatform(request,admin.getId()));}
	@PutMapping("/categories/{categoryId}") public ApiResponse<?> updateCategory(@PathVariable Long categoryId,@Valid @RequestBody UpdateRequest request,@AuthenticationPrincipal CustomUserDetails admin){return ApiResponse.success("Category commission updated.",service.updateCategory(categoryId,request,admin.getId()));}
	@GetMapping("/preview") public ApiResponse<?> preview(@RequestParam BigDecimal amount,@RequestParam BigDecimal rate){return ApiResponse.success("Commission preview calculated.",service.preview(amount,rate));}
}
