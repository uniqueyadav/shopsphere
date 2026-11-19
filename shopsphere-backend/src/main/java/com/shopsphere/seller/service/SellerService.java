package com.shopsphere.seller.service;

import java.util.Locale;
import java.io.IOException;
import java.util.Set;

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
import org.springframework.web.multipart.MultipartFile;

@Service
public class SellerService {

	private final SellerRepository sellers;
	private final ShopUserRepository users;
	private final SellerDocumentStorage documentStorage;

	public SellerService(SellerRepository sellers, ShopUserRepository users, SellerDocumentStorage documentStorage) {
		this.sellers = sellers;
		this.users = users;
		this.documentStorage = documentStorage;
	}

	@Transactional
	public SellerApplicationResponse applyForSeller(Long userId, SellerApplicationRequest request) {
		return applyForSeller(userId, request, null);
	}

	@Transactional
	public SellerApplicationResponse applyForSeller(Long userId, SellerApplicationRequest request, MultipartFile[] documents) {
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

		if (documents != null) {
			String[] documentTypes = { "businessPan", "gstCertificate", "addressProof", "identityProof" };
			Set<String> allowedExtensions = Set.of(".jpg", ".jpeg", ".png", ".pdf");
			for (int index = 0; index < documentTypes.length; index++) {
				MultipartFile file = documents[index];
				if (file == null || file.isEmpty()) {
					throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
							"Business PAN, GST certificate, address proof, and identity proof are required.");
				}
				if (file.getSize() > 5 * 1024 * 1024) {
					throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Each document must be 5 MB or smaller.");
				}
				String fileName = file.getOriginalFilename();
				String lowerName = fileName == null ? "" : fileName.toLowerCase(Locale.ROOT);
				boolean allowed = allowedExtensions.stream().anyMatch(lowerName::endsWith);
				if (!allowed) {
					throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only JPG, PNG, and PDF documents are allowed.");
				}
				try {
					SellerDocumentStorage.StoredSellerDocument stored = documentStorage.upload(
							file.getBytes(),
							"shopsphere/seller-documents/" + userId,
							fileName);
					seller.addDocument(new com.shopsphere.seller.entity.SellerDocument(
							documentTypes[index], fileName, stored.fileUrl(), stored.publicId()));
				} catch (IOException exception) {
					throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A document could not be read.", exception);
				}
			}
		}

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
		if ((request.pickupLatitude() == null) != (request.pickupLongitude() == null)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Both pickup coordinates are required.");
		seller.updatePickupLocation(request.pickupLatitude(), request.pickupLongitude());
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
		return new SellerDeliveryFeeResponse(seller.getId(), seller.getStoreName(), seller.getDeliveryFee(), seller.getPickupLatitude(), seller.getPickupLongitude());
	}

	private String normalizeOptional(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}
}
