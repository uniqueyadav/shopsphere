package com.shopsphere.admin.controller;

import java.util.Map;
import com.shopsphere.admin.dto.MarketplaceSettingsRequest;
import com.shopsphere.admin.service.MarketplaceSettingsService;
import com.shopsphere.common.response.ApiResponse;
import com.shopsphere.security.CustomUserDetails;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/v1/admin/settings") @PreAuthorize("hasRole('SUPER_ADMIN')")
public class MarketplaceSettingsController {
	private final MarketplaceSettingsService service;
	public MarketplaceSettingsController(MarketplaceSettingsService service){this.service=service;}
	@GetMapping public ApiResponse<?> get(){return ApiResponse.success("Settings loaded.",service.get());}
	@PutMapping public ApiResponse<?> update(@Valid @RequestBody MarketplaceSettingsRequest request,@AuthenticationPrincipal CustomUserDetails admin){Map<String,String> saved=service.update(request.settings(),admin.getId());return ApiResponse.success("Settings saved.",saved);}
}
