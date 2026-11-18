package com.shopsphere.admin.service;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.shopsphere.common.enums.AccountStatus;
import com.shopsphere.common.enums.Role;
import com.shopsphere.user.ShopUserRepository;
import com.shopsphere.user.entity.User;

@Component
@ConditionalOnProperty(prefix = "app.admin-bootstrap", name = "enabled", havingValue = "true")
public class AdminBootstrapRunner implements ApplicationRunner {
	private final ShopUserRepository users;
	private final PasswordEncoder passwordEncoder;
	private final String fullName;
	private final String email;
	private final String password;

	public AdminBootstrapRunner(
			ShopUserRepository users,
			PasswordEncoder passwordEncoder,
			@Value("${app.admin-bootstrap.full-name:}") String fullName,
			@Value("${app.admin-bootstrap.email:}") String email,
			@Value("${app.admin-bootstrap.password:}") String password) {
		this.users = users;
		this.passwordEncoder = passwordEncoder;
		this.fullName = fullName;
		this.email = email;
		this.password = password;
	}

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		String normalizedEmail = validateAndNormalizeInputs();
		var existingUser = users.findByEmail(normalizedEmail);
		if (existingUser.isPresent()) {
			if (existingUser.get().getRole() != Role.SUPER_ADMIN) {
				throw new IllegalStateException(
						"Admin bootstrap email is already assigned to a non-admin account. Choose a different email.");
			}
			return;
		}

		User admin = new User(fullName.trim(), normalizedEmail, passwordEncoder.encode(password));
		admin.setRole(Role.SUPER_ADMIN);
		admin.setAccountStatus(AccountStatus.ACTIVE);
		users.saveAndFlush(admin);
	}

	private String validateAndNormalizeInputs() {
		if (!StringUtils.hasText(fullName) || fullName.trim().length() > 100) {
			throw new IllegalStateException("ADMIN_BOOTSTRAP_NAME must be set and no longer than 100 characters.");
		}
		if (!StringUtils.hasText(email) || email.trim().length() > 254 || !email.trim().matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
			throw new IllegalStateException("ADMIN_BOOTSTRAP_EMAIL must contain a valid email address.");
		}
		if (!StringUtils.hasText(password) || password.length() < 12
				|| password.getBytes(StandardCharsets.UTF_8).length > 72) {
			throw new IllegalStateException(
					"ADMIN_BOOTSTRAP_PASSWORD must be at least 12 characters and no more than 72 UTF-8 bytes.");
		}
		return email.trim().toLowerCase(Locale.ROOT);
	}
}
