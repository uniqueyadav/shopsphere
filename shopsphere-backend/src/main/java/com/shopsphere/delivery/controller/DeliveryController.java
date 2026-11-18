package com.shopsphere.delivery.controller;

import com.shopsphere.common.response.ApiResponse;
import com.shopsphere.delivery.dto.CreatePartnerRequest;
import com.shopsphere.delivery.dto.PartnerStatusRequest;
import com.shopsphere.delivery.entity.PartnerStatus;
import com.shopsphere.delivery.service.DeliveryService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/delivery-partners")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class DeliveryController {
	private final DeliveryService service;
	public DeliveryController(DeliveryService service){this.service=service;}
	@GetMapping public ApiResponse<Page<?>> list(@RequestParam(required=false) String search,@RequestParam(required=false) PartnerStatus status,@RequestParam(required=false) String partnerType,@RequestParam(required=false) String city,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="10") int size){return ApiResponse.success("Delivery partners loaded.",service.list(search,status,partnerType,city,page,size));}
	@GetMapping("/stats") public ApiResponse<?> stats(){return ApiResponse.success("Partner statistics loaded.",service.stats());}
	@GetMapping("/{id}") public ApiResponse<?> get(@PathVariable Long id){return ApiResponse.success("Delivery partner loaded.",service.get(id));}
	@PostMapping public ApiResponse<?> create(@Valid @RequestBody CreatePartnerRequest request){return ApiResponse.success("Delivery partner profile created.",service.create(request));}
	@PatchMapping("/{id}/status") public ApiResponse<?> updateStatus(@PathVariable Long id,@Valid @RequestBody PartnerStatusRequest request){return ApiResponse.success("Partner status updated.",service.updateStatus(id,request));}
}
