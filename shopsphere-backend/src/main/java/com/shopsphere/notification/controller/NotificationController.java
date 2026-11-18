package com.shopsphere.notification.controller;

import com.shopsphere.common.response.ApiResponse;
import com.shopsphere.notification.dto.AdminNotificationDtos.CreateRequest;
import com.shopsphere.notification.entity.Notification;
import com.shopsphere.notification.service.NotificationService;
import com.shopsphere.security.CustomUserDetails;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/v1/admin/notifications") @PreAuthorize("hasRole('SUPER_ADMIN')")
public class NotificationController {
	private final NotificationService service;
	public NotificationController(NotificationService service){this.service=service;}
	@GetMapping public ApiResponse<Page<?>> list(@RequestParam(required=false) String search,@RequestParam(required=false) Notification.Status status,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="10") int size){return ApiResponse.success("Notifications loaded.",service.list(search,status,page,size));}
	@GetMapping("/stats") public ApiResponse<?> stats(){return ApiResponse.success("Notification statistics loaded.",service.stats());}
	@GetMapping("/{id}") public ApiResponse<?> get(@PathVariable Long id){return ApiResponse.success("Notification details loaded.",service.get(id));}
	@PostMapping public ApiResponse<?> create(@Valid @RequestBody CreateRequest request,@AuthenticationPrincipal CustomUserDetails admin){return ApiResponse.success("Notification created.",service.create(request,admin.getId()));}
}
