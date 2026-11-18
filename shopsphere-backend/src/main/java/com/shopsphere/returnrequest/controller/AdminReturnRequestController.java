package com.shopsphere.returnrequest.controller;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.shopsphere.common.response.ApiResponse;
import com.shopsphere.returnrequest.dto.ReturnRequestResponse;
import com.shopsphere.returnrequest.dto.UpdateReturnRequestStatus;
import com.shopsphere.returnrequest.service.CustomerReturnRequestService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;

@Validated
@RestController
@RequestMapping("/api/v1/admin/return-requests")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class AdminReturnRequestController {
	private final CustomerReturnRequestService service;
	public AdminReturnRequestController(CustomerReturnRequestService service) { this.service = service; }
	@GetMapping public ApiResponse<List<ReturnRequestResponse>> list() { return ApiResponse.success("Return requests loaded.", service.adminList()); }
	@PatchMapping("/{requestId}/status")
	public ApiResponse<ReturnRequestResponse> update(@PathVariable @Positive Long requestId, @Valid @RequestBody UpdateReturnRequestStatus request) {
		return ApiResponse.success("Return request status updated.", service.updateStatus(requestId, request));
	}
}
