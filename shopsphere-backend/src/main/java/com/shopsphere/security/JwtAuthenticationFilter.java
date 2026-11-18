package com.shopsphere.security;

import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopsphere.common.response.ApiResponse;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
	private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

	private final JwtService jwtService;
	private final CustomUserDetailsService userDetailsService;
	private final ObjectMapper objectMapper;

	public JwtAuthenticationFilter(
			JwtService jwtService,
			CustomUserDetailsService userDetailsService,
			ObjectMapper objectMapper) {
		this.jwtService = jwtService;
		this.userDetailsService = userDetailsService;
		this.objectMapper = objectMapper;
	}

	@Override
	protected void doFilterInternal(
			HttpServletRequest request,
			HttpServletResponse response,
			FilterChain filterChain) throws ServletException, IOException {
		String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
		if (authorization == null || !authorization.regionMatches(true, 0, "Bearer ", 0, 7)) {
			filterChain.doFilter(request, response);
			return;
		}

		String token = authorization.substring(7).trim();
		try {
			if (token.isEmpty()) {
				throw new JwtException("Bearer token is empty.");
			}
			Jwt jwt = jwtService.decodeToken(token);
			String tokenUse = jwt.getClaimAsString("token_use");
			if (tokenUse != null && !"access".equals(tokenUse)) {
				logger.warn("Rejected a non-access JWT for {}.", request.getRequestURI());
				writeUnauthorized(response, "The bearer token is not an access token.");
				return;
			}
			CustomUserDetails userDetails = (CustomUserDetails) userDetailsService.loadUserByUsername(jwt.getSubject());
			if (!userDetails.isEnabled()) {
				logger.warn("Rejected a JWT for a disabled account at {}.", request.getRequestURI());
				writeUnauthorized(response, "This account is not active.");
				return;
			}
			Authentication authentication = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
					userDetails, null, userDetails.getAuthorities());
			SecurityContextHolder.getContext().setAuthentication(authentication);
		}
		catch (JwtException exception) {
			SecurityContextHolder.clearContext();
			logger.warn("Rejected an invalid or expired JWT for {} ({}).", request.getRequestURI(), exception.getClass().getSimpleName());
			writeUnauthorized(response, "Invalid or expired access token.");
			return;
		}
		catch (UsernameNotFoundException exception) {
			SecurityContextHolder.clearContext();
			logger.warn("Rejected a JWT whose account no longer exists for {}.", request.getRequestURI());
			writeUnauthorized(response, "The token account was not found. Please sign in again.");
			return;
		}

		filterChain.doFilter(request, response);
	}

	private void writeUnauthorized(HttpServletResponse response, String message) throws IOException {
		response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		objectMapper.writeValue(response.getOutputStream(), ApiResponse.failure(message));
	}
}
