package com.shopsphere.seller.service;

import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import com.cloudinary.Cloudinary;
import com.shopsphere.product.service.CloudinaryProperties;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SellerDocumentStorage {

	private final Cloudinary cloudinary;
	private final CloudinaryProperties properties;

	public SellerDocumentStorage(Cloudinary cloudinary, CloudinaryProperties properties) {
		this.cloudinary = cloudinary;
		this.properties = properties;
	}

	public StoredSellerDocument upload(byte[] content, String folder, String originalFileName) {
		if (!properties.isConfigured()) {
			throw new ResponseStatusException(
					HttpStatus.SERVICE_UNAVAILABLE,
					"Document storage is not configured. Set the Cloudinary environment variables on the backend.");
		}
		try {
			String resourceType = originalFileName != null
					&& originalFileName.toLowerCase(Locale.ROOT).endsWith(".pdf")
							? "raw"
							: "image";
			Map<String, Object> uploadOptions = new HashMap<>();
			uploadOptions.put("folder", folder);
			uploadOptions.put("resource_type", resourceType);
			if ("image".equals(resourceType)) {
				uploadOptions.put("allowed_formats", "jpg,jpeg,png");
			}
			Map<?, ?> result = cloudinary.uploader().upload(
					content,
					uploadOptions);
			Object url = result.get("secure_url");
			Object publicId = result.get("public_id");
			if (url instanceof String secureUrl && publicId instanceof String id) {
				return new StoredSellerDocument(secureUrl, id);
			}
			throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Document storage returned an invalid response.");
		} catch (IOException exception) {
			throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Document upload failed.", exception);
		}
	}

	public record StoredSellerDocument(String fileUrl, String publicId) {
	}
}
