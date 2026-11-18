package com.shopsphere.category.service;

import java.io.IOException;
import java.util.Locale;

import com.shopsphere.category.dto.CategoryImageUploadResponse;
import com.shopsphere.category.repository.CategoryRepository;
import com.shopsphere.product.service.ProductImageStorage;
import com.shopsphere.product.service.StoredProductImage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CategoryImageService {

	private final ProductImageStorage storage;
	private final CategoryRepository categories;
	private final long maximumSize;

	public CategoryImageService(
			ProductImageStorage storage,
			CategoryRepository categories,
			@Value("${shopsphere.category-image.max-size-bytes:5242880}") long maximumSize) {
		this.storage = storage;
		this.categories = categories;
		this.maximumSize = maximumSize > 0 ? maximumSize : 5L * 1024 * 1024;
	}

	public CategoryImageUploadResponse upload(MultipartFile image) {
		if (image == null || image.isEmpty()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Select a category image to upload.");
		}
		if (image.getSize() > maximumSize) {
			throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "Category image exceeds the 5 MB size limit.");
		}
		String filename = image.getOriginalFilename();
		String extension = filename == null || filename.lastIndexOf('.') < 0
				? "" : filename.substring(filename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
		String expectedType = switch (extension) {
			case "jpg", "jpeg" -> "image/jpeg";
			case "png" -> "image/png";
			case "webp" -> "image/webp";
			default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only JPG, PNG, or WebP category images are supported.");
		};
		String declaredType = image.getContentType();
		if (declaredType == null || !expectedType.equalsIgnoreCase(declaredType.split(";", 2)[0].trim())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Image file type does not match its extension.");
		}
		try {
			byte[] content = image.getBytes();
			if (!matchesSignature(content, expectedType)) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The uploaded file is not a valid supported image.");
			}
			StoredProductImage stored = storage.upload(content, "shopsphere/categories");
			return new CategoryImageUploadResponse(stored.url(), stored.publicId());
		} catch (IOException exception) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The category image could not be read.", exception);
		}
	}

	public void deleteIfUnreferenced(String publicId) {
		if (publicId == null || publicId.isBlank()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Image storage ID is required.");
		}
		if (!publicId.startsWith("shopsphere/categories/")) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only ShopSphere category images can be removed here.");
		}
		if (!categories.existsByImagePublicId(publicId)) {
			storage.delete(publicId);
		}
	}

	private boolean matchesSignature(byte[] data, String type) {
		if ("image/jpeg".equals(type)) return data.length > 3 && (data[0] & 0xff) == 0xff && (data[1] & 0xff) == 0xd8 && (data[2] & 0xff) == 0xff;
		if ("image/png".equals(type)) return data.length >= 8 && (data[0] & 0xff) == 0x89 && data[1] == 'P' && data[2] == 'N' && data[3] == 'G';
		return data.length >= 12 && data[0] == 'R' && data[1] == 'I' && data[2] == 'F' && data[3] == 'F'
				&& data[8] == 'W' && data[9] == 'E' && data[10] == 'B' && data[11] == 'P';
	}
}
