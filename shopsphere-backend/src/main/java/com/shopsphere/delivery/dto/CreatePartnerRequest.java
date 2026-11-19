package com.shopsphere.delivery.dto;

import com.shopsphere.delivery.entity.VehicleType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreatePartnerRequest(
		@NotBlank @Size(max=100) String fullName,
		@NotBlank @Email @Size(max=254) String email,
		@NotBlank @Size(min=8,max=128) String password,
		@Size(max=20) @Pattern(regexp="^[+0-9().\\-\\s]{7,20}$") String phone,
		@NotNull VehicleType vehicleType,
		@Size(max=100) String city,
		@Size(max=500) String address,
		@Size(max=30) String vehicleRegistrationNumber) {}
