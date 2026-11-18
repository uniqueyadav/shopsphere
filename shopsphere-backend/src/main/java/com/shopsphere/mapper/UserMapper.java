package com.shopsphere.mapper;

import com.shopsphere.user.dto.AddressRequest;
import com.shopsphere.user.dto.AddressResponse;
import com.shopsphere.user.entity.Address;
import com.shopsphere.user.entity.User;

public final class UserMapper {

	private UserMapper() {
	}

	public static Address toAddress(AddressRequest request, User user, boolean defaultAddress) {
		return new Address(
				user,
				request.fullName().trim(),
				request.mobileNumber().trim(),
				request.houseFlatNumber().trim(),
				request.streetArea().trim(),
				normalizeOptional(request.landmark()),
				request.city().trim(),
				request.state().trim(),
				request.pinCode().trim(),
				request.addressType(),
				defaultAddress,
				normalizeOptional(request.deliveryInstructions()),
				request.leavePackageAtDoor(),
				request.callBeforeDelivery());
	}

	public static void updateAddress(Address address, AddressRequest request) {
		address.update(
				request.fullName().trim(),
				request.mobileNumber().trim(),
				request.houseFlatNumber().trim(),
				request.streetArea().trim(),
				normalizeOptional(request.landmark()),
				request.city().trim(),
				request.state().trim(),
				request.pinCode().trim(),
				request.addressType(),
				normalizeOptional(request.deliveryInstructions()),
				request.leavePackageAtDoor(),
				request.callBeforeDelivery());
		address.setDefaultAddress(request.defaultAddress());
	}

	public static AddressResponse toAddressResponse(Address address) {
		return new AddressResponse(
				address.getId(),
				address.getFullName(),
				address.getMobileNumber(),
				address.getHouseFlatNumber(),
				address.getStreetArea(),
				address.getLandmark(),
				address.getCity(),
				address.getState(),
				address.getPinCode(),
				address.getAddressType(),
				address.isDefaultAddress(),
				address.getDeliveryInstructions(),
				address.isLeavePackageAtDoor(),
				address.isCallBeforeDelivery());
	}

	private static String normalizeOptional(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}
}
