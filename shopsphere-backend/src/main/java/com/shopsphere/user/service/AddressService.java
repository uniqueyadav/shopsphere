package com.shopsphere.user.service;

import java.util.List;

import com.shopsphere.mapper.UserMapper;
import com.shopsphere.user.ShopUserRepository;
import com.shopsphere.user.dto.AddressRequest;
import com.shopsphere.user.dto.AddressResponse;
import com.shopsphere.user.entity.Address;
import com.shopsphere.user.entity.User;
import com.shopsphere.user.repository.AddressRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AddressService {

	private final AddressRepository addresses;
	private final ShopUserRepository users;

	public AddressService(AddressRepository addresses, ShopUserRepository users) {
		this.addresses = addresses;
		this.users = users;
	}

	@Transactional
	public AddressResponse create(Long customerId, AddressRequest request) {
		validateCoordinates(request);
		User customer = lockCustomer(customerId);
		List<Address> current = addresses.findAllByUser_Id(customerId);
		boolean useAsDefault = request.defaultAddress() || current.isEmpty();
		if (useAsDefault) {
			setDefaultFor(current, null);
		}
		Address address = UserMapper.toAddress(request, customer, useAsDefault);
		return UserMapper.toAddressResponse(addresses.save(address));
	}

	@Transactional(readOnly = true)
	public List<AddressResponse> list(Long customerId) {
		return addresses.findByUser_IdOrderByDefaultAddressDescCreatedAtDescIdDesc(customerId).stream()
				.map(UserMapper::toAddressResponse)
				.toList();
	}

	@Transactional(readOnly = true)
	public AddressResponse get(Long customerId, Long addressId) {
		return UserMapper.toAddressResponse(findOwnedAddress(customerId, addressId));
	}

	@Transactional
	public AddressResponse update(Long customerId, Long addressId, AddressRequest request) {
		validateCoordinates(request);
		lockCustomer(customerId);
		Address address = findOwnedAddress(customerId, addressId);
		if (request.defaultAddress()) {
			setDefaultFor(addresses.findAllByUser_Id(customerId), addressId);
		}
		UserMapper.updateAddress(address, request);
		return UserMapper.toAddressResponse(addresses.save(address));
	}

	@Transactional
	public AddressResponse delete(Long customerId, Long addressId) {
		lockCustomer(customerId);
		Address address = findOwnedAddress(customerId, addressId);
		Address nextDefault = address.isDefaultAddress()
				? addresses.findByUser_IdOrderByCreatedAtAscIdAsc(customerId).stream()
						.filter(candidate -> !candidate.getId().equals(addressId))
						.findFirst()
						.orElse(null)
				: null;

		addresses.delete(address);
		if (nextDefault != null) {
			nextDefault.setDefaultAddress(true);
			addresses.save(nextDefault);
		}
		return nextDefault == null ? null : UserMapper.toAddressResponse(nextDefault);
	}

	@Transactional
	public AddressResponse setDefault(Long customerId, Long addressId) {
		lockCustomer(customerId);
		Address address = findOwnedAddress(customerId, addressId);
		setDefaultFor(addresses.findAllByUser_Id(customerId), addressId);
		address.setDefaultAddress(true);
		return UserMapper.toAddressResponse(addresses.save(address));
	}

	private User lockCustomer(Long customerId) {
		return users.findByIdForUpdate(customerId)
				.orElseThrow(() -> new ResponseStatusException(
						HttpStatus.NOT_FOUND,
						"Customer account was not found."));
	}

	private void validateCoordinates(AddressRequest request) {
		if ((request.latitude() == null) != (request.longitude() == null))
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Both delivery location coordinates are required.");
	}

	private Address findOwnedAddress(Long customerId, Long addressId) {
		return addresses.findByIdAndUser_Id(addressId, customerId)
				.orElseThrow(() -> new ResponseStatusException(
						HttpStatus.NOT_FOUND,
						"Address was not found."));
	}

	private void setDefaultFor(List<Address> current, Long excludedAddressId) {
		for (Address address : current) {
			if (!address.getId().equals(excludedAddressId) && address.isDefaultAddress()) {
				address.setDefaultAddress(false);
				addresses.save(address);
			}
		}
	}
}
