package com.shopsphere.admin.service;

import java.time.Instant;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.shopsphere.admin.dto.SellerApplicationCounts;
import com.shopsphere.admin.dto.SellerApplicationDetailsResponse;
import com.shopsphere.admin.dto.SellerApplicationPageResponse;
import com.shopsphere.admin.dto.SellerApplicationSummaryResponse;
import com.shopsphere.admin.dto.UpdateSellerRequest;
import com.shopsphere.common.enums.AccountStatus;
import com.shopsphere.common.enums.BusinessType;
import com.shopsphere.common.enums.Role;
import com.shopsphere.common.enums.SellerStatus;
import com.shopsphere.seller.entity.Seller;
import com.shopsphere.seller.repository.SellerRepository;
import com.shopsphere.product.repository.ProductRepository;
import com.shopsphere.order.repository.SellerOrderRepository;
import com.shopsphere.user.ShopUserRepository;
import com.shopsphere.user.entity.User;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SellerApplicationAdminService {

	private final SellerRepository sellers;
	private final ShopUserRepository users;
	private final ProductRepository products;
	private final SellerOrderRepository sellerOrders;

	public SellerApplicationAdminService(
			SellerRepository sellers,
			ShopUserRepository users,
			ProductRepository products,
			SellerOrderRepository sellerOrders) {
		this.sellers = sellers;
		this.users = users;
		this.products = products;
		this.sellerOrders = sellerOrders;
	}

	@Transactional(readOnly = true)
	public SellerApplicationPageResponse listApplications(
			int page,
			int size,
			String search,
			SellerStatus status,
			BusinessType businessType,
			AccountStatus accountStatus) {
		Page<Seller> result = sellers.findAll(
				buildSpecification(search, status, businessType, accountStatus),
				PageRequest.of(
						page,
						size,
						Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))));
		return new SellerApplicationPageResponse(
				result.getContent().stream().map(SellerApplicationSummaryResponse::from).toList(),
				result.getNumber(),
				result.getSize(),
				result.getTotalElements(),
				result.getTotalPages(),
				getStatusCounts());
	}

	@Transactional(readOnly = true)
	public SellerApplicationDetailsResponse getApplication(Long id) {
		return details(findApplication(id));
	}

	@Transactional
	public SellerApplicationDetailsResponse approve(Long id, Long reviewerId) {
		Seller seller = findPendingApplication(id);
		User reviewer = findSuperAdmin(reviewerId);
		seller.recordReview(SellerStatus.APPROVED, reviewer, Instant.now(), null);
		seller.getUser().setRole(Role.SELLER);
		return details(seller);
	}

	@Transactional
	public SellerApplicationDetailsResponse reject(Long id, Long reviewerId, String reason) {
		Seller seller = findPendingApplication(id);
		User reviewer = findSuperAdmin(reviewerId);
		seller.recordReview(SellerStatus.REJECTED, reviewer, Instant.now(), reason.trim());
		seller.getUser().setRole(Role.CUSTOMER);
		return details(seller);
	}

	@Transactional
	public SellerApplicationDetailsResponse updateSeller(Long id, UpdateSellerRequest request) {
		Seller seller = findApplication(id);
		seller.updateProfile(
				request.storeName().trim(), request.sellerName().trim(),
				request.email().trim().toLowerCase(Locale.ROOT), request.phone().trim(),
				request.businessType(), normalizeOptional(request.gstNumber()),
				normalizeOptional(request.businessDescription()), request.businessAddress().trim());
		return details(seller);
	}

	@Transactional
	public SellerApplicationDetailsResponse suspendSeller(Long id, Long reviewerId, String reason) {
		Seller seller = findApplication(id);
		findSuperAdmin(reviewerId);
		if (seller.getApprovalStatus() != SellerStatus.APPROVED
				|| seller.getUser().getAccountStatus() != AccountStatus.ACTIVE) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Only active approved sellers can be suspended.");
		}
		seller.getUser().setAccountStatus(AccountStatus.SUSPENDED);
		seller.recordSuspension(reason.trim());
		return details(seller);
	}

	@Transactional
	public SellerApplicationDetailsResponse activateSeller(Long id, Long reviewerId) {
		Seller seller = findApplication(id);
		findSuperAdmin(reviewerId);
		if (seller.getUser().getAccountStatus() != AccountStatus.SUSPENDED) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Only suspended seller accounts can be activated.");
		}
		seller.getUser().setAccountStatus(AccountStatus.ACTIVE);
		seller.recordSuspension(null);
		return details(seller);
	}

	@Transactional(readOnly = true)
	public byte[] exportApplications(String search, SellerStatus status, BusinessType businessType, AccountStatus accountStatus) {
		List<Seller> result = sellers.findAll(
				buildSpecification(search, status, businessType, accountStatus),
				Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
		StringBuilder csv = new StringBuilder("ID,Seller,Email,Phone,Store,Business type,Status,Account status,Join date\r\n");
		for (Seller seller : result) {
			csv.append(seller.getId()).append(',')
					.append(csv(seller.getSellerFullName() == null ? seller.getUser().getFullName() : seller.getSellerFullName())).append(',')
					.append(csv(seller.getSellerEmail() == null ? seller.getUser().getEmail() : seller.getSellerEmail())).append(',')
					.append(csv(seller.getSellerPhone() == null ? seller.getUser().getPhone() : seller.getSellerPhone())).append(',')
					.append(csv(seller.getStoreName())).append(',')
					.append(seller.getBusinessType() == null ? "" : seller.getBusinessType().name()).append(',')
					.append(seller.getApprovalStatus().name()).append(',')
					.append(seller.getUser().getAccountStatus().name()).append(',')
					.append(seller.getCreatedAt()).append("\r\n");
		}
		return csv.toString().getBytes(StandardCharsets.UTF_8);
	}

	private SellerApplicationCounts getStatusCounts() {
		long pending = sellers.countByApprovalStatus(SellerStatus.PENDING);
		long suspended = sellers.countByApprovalStatusAndUser_AccountStatus(
				SellerStatus.APPROVED, AccountStatus.SUSPENDED);
		long approved = sellers.countByApprovalStatus(SellerStatus.APPROVED)
				- sellers.countByApprovalStatusAndUser_AccountStatus(SellerStatus.APPROVED, AccountStatus.SUSPENDED);
		long rejected = sellers.countByApprovalStatus(SellerStatus.REJECTED);
		return new SellerApplicationCounts(pending + approved + rejected + suspended, pending, approved, rejected, suspended);
	}

	private Specification<Seller> buildSpecification(String rawSearch, SellerStatus status, BusinessType businessType, AccountStatus accountStatus) {
		return (root, query, builder) -> {
			List<Predicate> predicates = new ArrayList<>();
			if (status != null) {
				predicates.add(builder.equal(root.get("approvalStatus"), status));
			}
			if (businessType != null) predicates.add(builder.equal(root.get("businessType"), businessType));
			if (accountStatus != null) predicates.add(builder.equal(root.join("user", JoinType.INNER).get("accountStatus"), accountStatus));
			if (rawSearch != null && !rawSearch.isBlank()) {
				String search = "%" + escapeLike(rawSearch.trim().toLowerCase(Locale.ROOT)) + "%";
				Join<Seller, User> user = root.join("user", JoinType.LEFT);
				predicates.add(builder.or(
						builder.like(builder.lower(root.get("storeName")), search, '\\'),
						builder.like(builder.lower(root.get("sellerFullName")), search, '\\'),
						builder.like(builder.lower(root.get("sellerEmail")), search, '\\'),
						builder.like(builder.lower(root.get("sellerPhone")), search, '\\'),
						builder.like(builder.lower(root.get("storeDescription")), search, '\\'),
						builder.like(builder.lower(user.get("fullName")), search, '\\'),
						builder.like(builder.lower(user.get("phone")), search, '\\'),
						builder.like(builder.lower(user.get("email")), search, '\\')));
			}
			return builder.and(predicates.toArray(Predicate[]::new));
		};
	}

	private String escapeLike(String value) {
		return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
	}

	private String csv(String value) {
		if (value == null) return "";
		return "\"" + value.replace("\"", "\"\"") + "\"";
	}

	private String normalizeOptional(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}

	private SellerApplicationDetailsResponse details(Seller seller) {
		return SellerApplicationDetailsResponse.from(
				seller, products.countBySeller_Id(seller.getId()), sellerOrders.countBySeller_Id(seller.getId()));
	}

	private Seller findApplication(Long id) {
		return sellers.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Seller application was not found."));
	}

	private Seller findPendingApplication(Long id) {
		Seller seller = findApplication(id);
		if (seller.getApprovalStatus() != SellerStatus.PENDING) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Only pending applications can be reviewed.");
		}
		return seller;
	}

	private User findSuperAdmin(Long reviewerId) {
		User reviewer = users.findById(reviewerId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication is required."));
		if (reviewer.getRole() != Role.SUPER_ADMIN) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only a super admin can review applications.");
		}
		return reviewer;
	}
}
