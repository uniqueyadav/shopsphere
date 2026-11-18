package com.shopsphere.security;

import java.util.Locale;

import com.shopsphere.user.ShopUserRepository;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

	private final ShopUserRepository users;

	public CustomUserDetailsService(ShopUserRepository users) {
		this.users = users;
	}

	@Override
	public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
		String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
		return users.findByEmail(normalizedEmail)
				.map(CustomUserDetails::new)
				.orElseThrow(() -> new UsernameNotFoundException("User not found."));
	}
}