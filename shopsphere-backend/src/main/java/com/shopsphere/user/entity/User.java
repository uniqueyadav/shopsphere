package com.shopsphere.user.entity;

import com.shopsphere.common.enums.AccountStatus;
import com.shopsphere.common.enums.Role;
import com.shopsphere.user.ShopUser;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "shop_users")
public class User extends ShopUser {

	@Size(max = 20)
	@Pattern(regexp = "^[+0-9().\\-\\s]{7,20}$")
	@Column(length = 20)
	private String phone;

	@NotNull
	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private AccountStatus accountStatus = AccountStatus.ACTIVE;

	@NotNull
	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private Role role = Role.CUSTOMER;

	protected User() {
	}

	public User(String fullName, String email, String passwordHash) {
		super(fullName, email, passwordHash);
	}

	public String getPhone() {
		return phone;
	}

	public void setPhone(String phone) {
		this.phone = phone;
	}

	public AccountStatus getAccountStatus() {
		return accountStatus;
	}

	public void setAccountStatus(AccountStatus accountStatus) {
		this.accountStatus = accountStatus;
	}

	public Role getRole() {
		return role;
	}

	public void setRole(Role role) {
		this.role = role;
	}
}