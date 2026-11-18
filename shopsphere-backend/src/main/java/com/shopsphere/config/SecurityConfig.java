package com.shopsphere.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopsphere.common.enums.SellerStatus;
import com.shopsphere.common.response.ApiResponse;
import com.shopsphere.security.CustomUserDetails;
import com.shopsphere.security.JwtAuthenticationFilter;
import com.shopsphere.seller.repository.SellerRepository;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

	@Bean
	SecurityFilterChain securityFilterChain(
			HttpSecurity http,
			JwtAuthenticationFilter jwtAuthenticationFilter,
			ObjectMapper objectMapper,
			AuthorizationManager<RequestAuthorizationContext> approvedSellerAuthorizationManager) throws Exception {
		return http
				.csrf(AbstractHttpConfigurer::disable)
				.cors(org.springframework.security.config.Customizer.withDefaults())
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.exceptionHandling(exceptions -> exceptions
						.authenticationEntryPoint((request, response, exception) ->
								writeSecurityError(response, objectMapper, HttpServletResponse.SC_UNAUTHORIZED,
										"Authentication is required."))
						.accessDeniedHandler((request, response, exception) ->
								writeSecurityError(response, objectMapper, HttpServletResponse.SC_FORBIDDEN,
										"You do not have permission to access this resource.")))
				.authorizeHttpRequests(authorize -> authorize
						.requestMatchers("/error").permitAll()
						.requestMatchers("/api/v1/health", "/api/v1/auth/register", "/api/v1/auth/login", "/api/v1/auth/refresh").permitAll()
						.requestMatchers(HttpMethod.POST, "/api/v1/payments/webhook").permitAll()
						.requestMatchers("/api/v1/admin/**").hasRole("SUPER_ADMIN")
						.requestMatchers(HttpMethod.GET, "/api/v1/categories", "/api/v1/categories/**").permitAll()
						.requestMatchers(HttpMethod.GET, "/api/v1/products", "/api/v1/products/**").permitAll()
						.requestMatchers(HttpMethod.POST, "/api/v1/seller/applications").hasRole("CUSTOMER")
						.requestMatchers("/api/v1/seller/**").access(approvedSellerAuthorizationManager)
						.requestMatchers("/api/v1/delivery/**").hasRole("DELIVERY_PARTNER")
						.anyRequest().authenticated())
				.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
				.build();
	}

	@Bean
	AuthorizationManager<RequestAuthorizationContext> approvedSellerAuthorizationManager(SellerRepository sellers) {
		return (authenticationSupplier, context) -> {
			Authentication authentication = authenticationSupplier.get();
			if (authentication == null
					|| !authentication.isAuthenticated()
					|| !authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_SELLER"))
					|| !(authentication.getPrincipal() instanceof CustomUserDetails userDetails)) {
				return new AuthorizationDecision(false);
			}
			return new AuthorizationDecision(
					sellers.existsByUser_IdAndApprovalStatus(userDetails.getId(), SellerStatus.APPROVED));
		};
	}

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder(12);
	}

	@Bean
	FilterRegistrationBean<JwtAuthenticationFilter> jwtFilterServletRegistration(
			JwtAuthenticationFilter jwtAuthenticationFilter) {
		FilterRegistrationBean<JwtAuthenticationFilter> registration =
				new FilterRegistrationBean<>(jwtAuthenticationFilter);
		registration.setEnabled(false);
		return registration;
	}

	private void writeSecurityError(
			HttpServletResponse response,
			ObjectMapper objectMapper,
			int status,
			String message) throws java.io.IOException {
		response.setStatus(status);
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		objectMapper.writeValue(response.getOutputStream(), ApiResponse.failure(message));
	}
}
