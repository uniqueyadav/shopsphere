package com.shopsphere.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
		@NotBlank @Size(max = 100) String fullName,
		@NotBlank @Email @Size(max = 254) String email,
		@NotBlank @Size(min = 8, max = 128) String password,
		@Size(max = 20) @Pattern(regexp = "^[+0-9().\\-\\s]{7,20}$") String phone) {
}