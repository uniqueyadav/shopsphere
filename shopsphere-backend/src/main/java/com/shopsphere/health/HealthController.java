package com.shopsphere.health;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.shopsphere.common.response.ApiResponse;

@RestController
@RequestMapping("/api/v1/health")
public class HealthController {

	@GetMapping
	public ApiResponse<HealthStatus> health() {
		return ApiResponse.success("Service is healthy", new HealthStatus("UP"));
	}

	public record HealthStatus(String status) {
	}
}
