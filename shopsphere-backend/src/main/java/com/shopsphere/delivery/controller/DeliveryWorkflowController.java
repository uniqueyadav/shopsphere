package com.shopsphere.delivery.controller;

import com.shopsphere.common.response.ApiResponse;
import com.shopsphere.delivery.dto.DeliveryWorkflowDtos.AvailabilityRequest;
import com.shopsphere.delivery.dto.DeliveryWorkflowDtos.Dashboard;
import com.shopsphere.delivery.dto.DeliveryWorkflowDtos.LocationRequest;
import com.shopsphere.delivery.dto.DeliveryWorkflowDtos.StatusRequest;
import com.shopsphere.delivery.service.DeliveryWorkflowService;
import com.shopsphere.security.CustomUserDetails;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/delivery")
public class DeliveryWorkflowController {
    private final DeliveryWorkflowService service;
    public DeliveryWorkflowController(DeliveryWorkflowService service) { this.service = service; }
    @GetMapping("/dashboard")
    public ApiResponse<Dashboard> dashboard(@AuthenticationPrincipal CustomUserDetails user) { return ApiResponse.success("Delivery dashboard loaded.", service.dashboard(user.getId())); }
    @PatchMapping("/availability")
    public ApiResponse<Boolean> availability(@AuthenticationPrincipal CustomUserDetails user, @Valid @RequestBody AvailabilityRequest request) { return ApiResponse.success("Availability updated.", service.setAvailability(user.getId(), request.online())); }
    @PatchMapping("/location")
    public ApiResponse<?> location(@AuthenticationPrincipal CustomUserDetails user, @Valid @RequestBody LocationRequest request) { service.updateLocation(user.getId(), request.latitude(), request.longitude()); return ApiResponse.success("Location updated.", java.util.Map.of("latitude", request.latitude(), "longitude", request.longitude())); }
    @PatchMapping("/assignments/{id}/status")
    public ApiResponse<?> status(@AuthenticationPrincipal CustomUserDetails user, @PathVariable Long id, @Valid @RequestBody StatusRequest request) { return ApiResponse.success("Delivery status updated.", service.updateStatus(user.getId(), id, request.status(), request.latitude(), request.longitude())); }
    @PatchMapping("/assignments/{id}/accept")
    public ApiResponse<?> accept(@AuthenticationPrincipal CustomUserDetails user, @PathVariable Long id) { return ApiResponse.success("Delivery accepted.", service.acceptAssignment(user.getId(), id)); }
    @GetMapping("/assignments/{id}")
    public ApiResponse<?> details(@AuthenticationPrincipal CustomUserDetails user, @PathVariable Long id) { return ApiResponse.success("Delivery order details loaded.", service.assignmentDetails(user.getId(), id)); }
}
