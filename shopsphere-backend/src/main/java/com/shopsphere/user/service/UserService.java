package com.shopsphere.user.service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.shopsphere.admin.dto.AdminUserDetailsResponse;
import com.shopsphere.admin.dto.AdminUserPageResponse;
import com.shopsphere.admin.dto.AdminUserStatsResponse;
import com.shopsphere.common.enums.AccountStatus;
import com.shopsphere.common.enums.Role;
import com.shopsphere.seller.entity.Seller;
import com.shopsphere.seller.repository.SellerRepository;
import com.shopsphere.user.ShopUserRepository;
import com.shopsphere.user.dto.UpdateUserRequest;
import com.shopsphere.user.dto.UserResponse;
import com.shopsphere.user.entity.User;

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
public class UserService {

	private final ShopUserRepository users;
	private final SellerRepository sellers;

	public UserService(ShopUserRepository users, SellerRepository sellers) {
		this.users = users;
		this.sellers = sellers;
	}

	@Transactional(readOnly = true)
	public AdminUserPageResponse listUsers(int page, int size, String search, Role role, AccountStatus status) {
		Page<User> result = users.findAll(specification(search, role, status),
				PageRequest.of(page, size, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))));
		return new AdminUserPageResponse(result.getContent().stream().map(UserResponse::from).toList(),
				result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
	}

	@Transactional(readOnly = true)
	public AdminUserStatsResponse getStats() {
		return new AdminUserStatsResponse(users.count(), users.countByRole(Role.CUSTOMER),
				users.countByRole(Role.SELLER), users.countByRole(Role.DELIVERY_PARTNER));
	}

	@Transactional(readOnly = true)
	public AdminUserDetailsResponse getUser(Long id) {
		User user = findUser(id);
		Seller seller = user.getRole() == Role.SELLER ? sellers.findByUser_Id(id).orElse(null) : null;
		return new AdminUserDetailsResponse(UserResponse.from(user),
				seller == null ? null : seller.getStoreName(),
				seller == null ? null : seller.getBusinessAddress(),
				seller == null || seller.getBusinessType() == null ? null : seller.getBusinessType().name(),
				seller == null ? null : seller.getApprovalStatus());
	}

	@Transactional
	public UserResponse updateUser(Long id, UpdateUserRequest request) {
		User user = findUser(id);
		String fullName = request.fullName().trim();
		String email = request.email().trim().toLowerCase(Locale.ROOT);
		if (users.existsByEmailAndIdNot(email, id)) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "That email address is already in use.");
		}
		user.updateProfile(fullName, email);
		String phone = request.phone() == null ? null : request.phone().trim();
		user.setPhone(phone == null || phone.isEmpty() ? null : phone);
		return UserResponse.from(user);
	}

	@Transactional
	public UserResponse updateStatus(Long id, AccountStatus requestedStatus) {
		User user = findUser(id);
		if (user.getRole() == Role.SUPER_ADMIN) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Super admin accounts cannot be changed through user management.");
		}
		if (requestedStatus != AccountStatus.ACTIVE && requestedStatus != AccountStatus.SUSPENDED) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User status must be ACTIVE or SUSPENDED.");
		}
		user.setAccountStatus(requestedStatus);
		return UserResponse.from(user);
	}

	@Transactional(readOnly = true)
	public byte[] exportUsers(String search, Role role, AccountStatus status) {
		List<User> result = users.findAll(specification(search, role, status),
				Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
		StringBuilder csv = new StringBuilder("ID,Full name,Email,Phone,Role,Account status,Join date\r\n");
		for (User user : result) {
			csv.append(user.getId()).append(',')
					.append(csv(user.getFullName())).append(',')
					.append(csv(user.getEmail())).append(',')
					.append(csv(user.getPhone())).append(',')
					.append(user.getRole().name()).append(',')
					.append(user.getAccountStatus().name()).append(',')
					.append(user.getCreatedAt()).append("\r\n");
		}
		return csv.toString().getBytes(StandardCharsets.UTF_8);
	}

	private Specification<User> specification(String rawSearch, Role role, AccountStatus status) {
		return (root, query, builder) -> {
			List<Predicate> predicates = new ArrayList<>();
			if (role != null) predicates.add(builder.equal(root.get("role"), role));
			if (status != null) predicates.add(builder.equal(root.get("accountStatus"), status));
			if (rawSearch != null && !rawSearch.isBlank()) {
				String search = "%" + escapeLike(rawSearch.trim().toLowerCase(Locale.ROOT)) + "%";
				predicates.add(builder.or(
						builder.like(builder.lower(root.get("fullName")), search, '\\'),
						builder.like(builder.lower(root.get("email")), search, '\\'),
						builder.like(builder.lower(root.get("phone")), search, '\\')));
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

	private User findUser(Long id) {
		return users.findById(id).orElseThrow(() ->
				new ResponseStatusException(HttpStatus.NOT_FOUND, "User was not found."));
	}
}
