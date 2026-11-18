package com.shopsphere.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateUserRequest(
		@NotBlank @Size(max = 100) String fullName,
		@NotBlank @Email @Size(max = 254) String email,
		@Pattern(regexp = "^$|^[+0-9().\\-\\s]{7,20}$", message = "Phone number must contain 7 to 20 valid characters.") String phone) {
}
