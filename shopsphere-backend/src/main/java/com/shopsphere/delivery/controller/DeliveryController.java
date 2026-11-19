package com.shopsphere.delivery.controller;

import com.shopsphere.common.response.ApiResponse;
import com.shopsphere.delivery.dto.CreatePartnerRequest;
import com.shopsphere.delivery.dto.PartnerStatusRequest;
import com.shopsphere.delivery.dto.PartnerServiceCityRequest;
import com.shopsphere.delivery.entity.PartnerStatus;
import com.shopsphere.delivery.service.DeliveryService;
import com.shopsphere.delivery.service.CourierDashboardService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/delivery-partners")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class DeliveryController {
	private final DeliveryService service;
	private final CourierDashboardService dashboardService;
	public DeliveryController(DeliveryService service, CourierDashboardService dashboardService){this.service=service;this.dashboardService=dashboardService;}
	@GetMapping("/dashboard") public ApiResponse<?> dashboard(@RequestParam(defaultValue="30") int days){if(days!=7&&days!=30&&days!=90)throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST,"Days must be 7, 30, or 90.");return ApiResponse.success("Courier dashboard loaded.",dashboardService.dashboard(days));}
	@GetMapping public ApiResponse<Page<?>> list(@RequestParam(required=false) String search,@RequestParam(required=false) PartnerStatus status,@RequestParam(required=false) String partnerType,@RequestParam(required=false) String city,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="10") int size){return ApiResponse.success("Delivery partners loaded.",service.list(search,status,partnerType,city,page,size));}
	@GetMapping("/stats") public ApiResponse<?> stats(){return ApiResponse.success("Partner statistics loaded.",service.stats());}
	@GetMapping("/{id}") public ApiResponse<?> get(@PathVariable Long id){return ApiResponse.success("Delivery partner loaded.",service.get(id));}
	@PatchMapping("/{id}/service-city") public ApiResponse<?> updateServiceCity(@PathVariable Long id,@Valid @RequestBody PartnerServiceCityRequest request){return ApiResponse.success("Partner service city updated.",service.updateServiceCity(id,request.city()));}
	@PostMapping public ApiResponse<?> create(@Valid @RequestBody CreatePartnerRequest request){return ApiResponse.success("Delivery partner profile created.",service.create(request));}
	@PatchMapping("/{id}/status") public ApiResponse<?> updateStatus(@PathVariable Long id,@Valid @RequestBody PartnerStatusRequest request){return ApiResponse.success("Partner status updated.",service.updateStatus(id,request));}
}
