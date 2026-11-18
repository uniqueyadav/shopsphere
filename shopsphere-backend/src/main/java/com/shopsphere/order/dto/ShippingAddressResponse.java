package com.shopsphere.order.dto;

import com.shopsphere.common.enums.AddressType;

public record ShippingAddressResponse(
		String fullName,
		String mobileNumber,
		String houseFlatNumber,
		String streetArea,
		String landmark,
		String city,
		String state,
		String pinCode,
		AddressType addressType) {
}
