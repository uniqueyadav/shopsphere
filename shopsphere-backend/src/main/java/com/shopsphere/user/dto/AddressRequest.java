package com.shopsphere.user.dto;

import com.shopsphere.common.enums.AddressType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AddressRequest(
		@NotBlank @Size(max = 100) String fullName,
		@NotBlank @Pattern(regexp = "^[6-9][0-9]{9}$", message = "Mobile number must be a valid 10-digit Indian number.")
		String mobileNumber,
		@NotBlank @Size(max = 100) String houseFlatNumber,
		@NotBlank @Size(max = 255) String streetArea,
		@Size(max = 150) String landmark,
		@NotBlank @Size(max = 100) String city,
		@NotBlank @Size(max = 100) String state,
		@NotBlank @Pattern(regexp = "^[1-9][0-9]{5}$", message = "PIN code must be a valid 6-digit Indian PIN code.")
		String pinCode,
		@NotNull AddressType addressType,
		boolean defaultAddress,
		@Size(max = 500) String deliveryInstructions,
		boolean leavePackageAtDoor,
		boolean callBeforeDelivery,
		@jakarta.validation.constraints.DecimalMin("-90.0") @jakarta.validation.constraints.DecimalMax("90.0") Double latitude,
		@jakarta.validation.constraints.DecimalMin("-180.0") @jakarta.validation.constraints.DecimalMax("180.0") Double longitude) {

	public AddressRequest(
			String fullName, String mobileNumber, String houseFlatNumber, String streetArea, String landmark,
			String city, String state, String pinCode, AddressType addressType, boolean defaultAddress) {
		this(fullName, mobileNumber, houseFlatNumber, streetArea, landmark, city, state, pinCode,
				addressType, defaultAddress, null, false, false, null, null);
	}
	public AddressRequest(String fullName, String mobileNumber, String houseFlatNumber, String streetArea, String landmark,
			String city, String state, String pinCode, AddressType addressType, boolean defaultAddress,
			String deliveryInstructions, boolean leavePackageAtDoor, boolean callBeforeDelivery) {
		this(fullName, mobileNumber, houseFlatNumber, streetArea, landmark, city, state, pinCode, addressType,
				defaultAddress, deliveryInstructions, leavePackageAtDoor, callBeforeDelivery, null, null);
	}
}
