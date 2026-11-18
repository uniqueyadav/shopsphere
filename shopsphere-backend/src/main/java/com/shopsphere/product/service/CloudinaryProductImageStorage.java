package com.shopsphere.product.service;

import java.io.IOException;
import java.util.Map;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CloudinaryProductImageStorage implements ProductImageStorage {

	private final Cloudinary cloudinary;
	private final CloudinaryProperties properties;

	public CloudinaryProductImageStorage(Cloudinary cloudinary, CloudinaryProperties properties) {
		this.cloudinary = cloudinary;
		this.properties = properties;
	}

	@Override
	public StoredProductImage upload(byte[] content, String folder) {
		requireConfiguration();
		try {
			Map<?, ?> result = cloudinary.uploader().upload(
					content,
					ObjectUtils.asMap(
							"folder", folder,
							"resource_type", "image",
							"allowed_formats", "jpg,jpeg,png,webp"));
			Object url = result.get("secure_url");
			Object publicId = result.get("public_id");
			if (url instanceof String secureUrl && publicId instanceof String id) {
				return new StoredProductImage(secureUrl, id);
			}
			throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Image storage returned an invalid response.");
		} catch (IOException exception) {
			throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Image upload failed.", exception);
		}
	}

	@Override
	public void delete(String publicId) {
		requireConfiguration();
		try {
			Map<?, ?> result = cloudinary.uploader().destroy(publicId, ObjectUtils.asMap("invalidate", true));
			Object status = result.get("result");
			if (!"ok".equals(status) && !"not found".equals(status)) {
				throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Image storage could not remove the image.");
			}
		} catch (IOException exception) {
			throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Image removal failed.", exception);
		}
	}

	private void requireConfiguration() {
		if (!properties.isConfigured()) {
			throw new ResponseStatusException(
					HttpStatus.SERVICE_UNAVAILABLE,
					"Image storage is not configured. Set the Cloudinary environment variables on the backend.");
		}
	}
}
