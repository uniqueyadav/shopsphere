package com.shopsphere.seller.service;

import java.util.Locale;

import com.shopsphere.common.enums.Role;
import com.shopsphere.common.enums.SellerStatus;
import com.shopsphere.seller.dto.SellerApplicationRequest;
import com.shopsphere.seller.dto.SellerApplicationResponse;
import com.shopsphere.seller.dto.SellerDeliveryFeeRequest;
import com.shopsphere.seller.dto.SellerDeliveryFeeResponse;
import com.shopsphere.seller.entity.Seller;
import com.shopsphere.seller.repository.SellerRepository;
import com.shopsphere.user.ShopUserRepository;
import com.shopsphere.user.entity.User;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SellerService {

	private final SellerRepository sellers;
	private final ShopUserRepository users;

	public SellerService(SellerRepository sellers, ShopUserRepository users) {
		this.sellers = sellers;
		this.users = users;
	}

	@Transactional
	public SellerApplicationResponse applyForSeller(Long userId, SellerApplicationRequest request) {
		User user = users.findById(userId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User account was not found."));
		if (user.getRole() != Role.CUSTOMER) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only customer accounts can apply as sellers.");
		}
		if (sellers.existsByUser_Id(userId)) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "A seller application already exists for this account.");
		}

		String gstNumber = normalizeOptional(request.gstNumber());
		Seller seller = new Seller(
				user,
				request.businessName().trim(),
				request.sellerFullName().trim(),
				request.email().trim().toLowerCase(Locale.ROOT),
				request.phone().trim(),
				request.businessType(),
				gstNumber == null ? null : gstNumber.toUpperCase(Locale.ROOT),
				normalizeOptional(request.businessDescription()),
				request.businessAddress().trim());

		return SellerApplicationResponse.from(sellers.save(seller));
	}

	@Transactional(readOnly = true)
	public SellerDeliveryFeeResponse getDeliveryFee(Long userId) {
		return deliveryFeeResponse(requireApprovedSeller(userId));
	}

	@Transactional
	public SellerDeliveryFeeResponse updateDeliveryFee(Long userId, SellerDeliveryFeeRequest request) {
		Seller seller = sellers.findByUser_IdForUpdate(userId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Seller profile was not found."));
		if (seller.getApprovalStatus() != SellerStatus.APPROVED) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only approved sellers can update delivery fees.");
		}
		seller.updateDeliveryFee(request.deliveryFee());
		return deliveryFeeResponse(seller);
	}

	private Seller requireApprovedSeller(Long userId) {
		Seller seller = sellers.findByUser_Id(userId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Seller profile was not found."));
		if (seller.getApprovalStatus() != SellerStatus.APPROVED) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only approved sellers can view delivery fees.");
		}
		return seller;
	}

	private SellerDeliveryFeeResponse deliveryFeeResponse(Seller seller) {
		return new SellerDeliveryFeeResponse(seller.getId(), seller.getStoreName(), seller.getDeliveryFee());
	}

	private String normalizeOptional(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}
}
