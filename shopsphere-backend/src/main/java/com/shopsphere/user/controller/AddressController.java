package com.shopsphere.user.controller;

import java.util.List;

import com.shopsphere.common.response.ApiResponse;
import com.shopsphere.security.CustomUserDetails;
import com.shopsphere.user.dto.AddressRequest;
import com.shopsphere.user.dto.AddressResponse;
import com.shopsphere.user.service.AddressService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/addresses")
@PreAuthorize("hasRole('CUSTOMER')")
public class AddressController {

	private final AddressService service;

	public AddressController(AddressService service) {
		this.service = service;
	}

	@PostMapping
	public ResponseEntity<ApiResponse<AddressResponse>> create(
			@AuthenticationPrincipal CustomUserDetails customer,
			@Valid @RequestBody AddressRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(ApiResponse.success("Address created.", service.create(customer.getId(), request)));
	}

	@GetMapping
	public ApiResponse<List<AddressResponse>> list(
			@AuthenticationPrincipal CustomUserDetails customer) {
		return ApiResponse.success("Addresses retrieved.", service.list(customer.getId()));
	}

	@GetMapping("/{id}")
	public ApiResponse<AddressResponse> get(
			@AuthenticationPrincipal CustomUserDetails customer,
			@PathVariable @Positive Long id) {
		return ApiResponse.success("Address retrieved.", service.get(customer.getId(), id));
	}

	@PutMapping("/{id}")
	public ApiResponse<AddressResponse> update(
			@AuthenticationPrincipal CustomUserDetails customer,
			@PathVariable @Positive Long id,
			@Valid @RequestBody AddressRequest request) {
		return ApiResponse.success("Address updated.", service.update(customer.getId(), id, request));
	}

	@DeleteMapping("/{id}")
	public ApiResponse<AddressResponse> delete(
			@AuthenticationPrincipal CustomUserDetails customer,
			@PathVariable @Positive Long id) {
		AddressResponse newDefault = service.delete(customer.getId(), id);
		return ApiResponse.success("Address deleted.", newDefault);
	}

	@PatchMapping("/{id}/default")
	public ApiResponse<AddressResponse> setDefault(
			@AuthenticationPrincipal CustomUserDetails customer,
			@PathVariable @Positive Long id) {
		return ApiResponse.success("Default address updated.", service.setDefault(customer.getId(), id));
	}
}
