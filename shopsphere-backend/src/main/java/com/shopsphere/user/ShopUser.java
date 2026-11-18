package com.shopsphere.user;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import com.shopsphere.common.entity.BaseEntity;

@MappedSuperclass
public abstract class ShopUser extends BaseEntity {

	@NotBlank
	@Size(max = 100)
	@Column(nullable = false, length = 100)
	private String fullName;

	@NotBlank
	@Email
	@Size(max = 254)
	@Column(nullable = false, unique = true, length = 254)
	private String email;

	@NotBlank
	@Size(max = 255)
	@Column(name = "password_hash", nullable = false, length = 255)
	private String password;

	protected ShopUser() {
	}

	protected ShopUser(String fullName, String email, String passwordHash) {
		this.fullName = fullName;
		this.email = email;
		this.password = passwordHash;
	}

	public String getFullName() {
		return fullName;
	}

	public String getEmail() {
		return email;
	}

	public String getPasswordHash() {
		return password;
	}

	public void updateProfile(String fullName, String email) {
		this.fullName = fullName;
		this.email = email;
	}

	protected void setPasswordHash(String passwordHash) {
		this.password = passwordHash;
	}
}
