package com.shopsphere.config;

import java.util.HashMap;
import java.util.Map;

import com.cloudinary.Cloudinary;
import com.shopsphere.product.service.CloudinaryProperties;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(CloudinaryProperties.class)
public class CloudinaryConfig {

	@Bean
	Cloudinary cloudinary(CloudinaryProperties properties) {
		Map<String, Object> config = new HashMap<>();
		config.put("cloud_name", properties.cloudName());
		config.put("api_key", properties.apiKey());
		config.put("api_secret", properties.apiSecret());
		return new Cloudinary(config);
	}
}