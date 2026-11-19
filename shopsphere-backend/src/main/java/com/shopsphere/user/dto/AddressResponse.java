package com.shopsphere.user.dto;

import com.shopsphere.common.enums.AddressType;

public record AddressResponse(
		Long id,
		String fullName,
		String mobileNumber,
		String houseFlatNumber,
		String streetArea,
		String landmark,
		String city,
		String state,
		String pinCode,
		AddressType addressType,
		boolean defaultAddress,
		String deliveryInstructions,
		boolean leavePackageAtDoor,
		boolean callBeforeDelivery,
		Double latitude,
		Double longitude) {
}
