package com.shopsphere.product.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.shopsphere.product.dto.ProductImageResponse;
import com.shopsphere.product.entity.Product;
import com.shopsphere.product.entity.ProductImage;
import com.shopsphere.product.repository.ProductImageRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ProductImageService {

	private static final Logger LOGGER = LoggerFactory.getLogger(ProductImageService.class);
	private static final Map<String, String> SUPPORTED_FORMATS = Map.of(
			"jpg", "image/jpeg",
			"jpeg", "image/jpeg",
			"png", "image/png",
			"webp", "image/webp");

	private final ProductService products;
	private final ProductImageRepository images;
	private final ProductImageStorage storage;
	private final long maximumFileSize;
	private final int maximumImageCount;

	public ProductImageService(
			ProductService products,
			ProductImageRepository images,
			ProductImageStorage storage,
			@Value("${shopsphere.product-image.max-size-bytes:5242880}") long maximumFileSize,
			@Value("${shopsphere.product-image.max-count:8}") int maximumImageCount) {
		this.products = products;
		this.images = images;
		this.storage = storage;
		this.maximumFileSize = maximumFileSize;
		this.maximumImageCount = maximumImageCount;
	}

	@Transactional(readOnly = true)
	public List<ProductImageResponse> listForSeller(Long userId, Long productId) {
		products.requireOwnedProduct(userId, productId);
		return responseList(productId);
	}

	@Transactional(readOnly = true)
	public List<ProductImageResponse> listForAdmin(Long productId) {
		products.requireAdminProduct(productId);
		return responseList(productId);
	}

	@Transactional(readOnly = true)
	public List<ProductImageResponse> listPublic(Long productId) {
		products.requirePublicProduct(productId);
		return responseList(productId);
	}

	@Transactional
	public List<ProductImageResponse> upload(Long userId, Long productId, List<MultipartFile> files) {
		Product product = products.requireOwnedProduct(userId, productId);
		return uploadToProduct(product, productId, files);
	}

	@Transactional
	public List<ProductImageResponse> uploadForAdmin(Long productId, List<MultipartFile> files) {
		Product product = products.requireAdminProduct(productId);
		return uploadToProduct(product, productId, files);
	}

	private List<ProductImageResponse> uploadToProduct(Product product, Long productId, List<MultipartFile> files) {
		if (files == null || files.isEmpty()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Select at least one product image.");
		}

		List<String> uploadedPublicIds = new ArrayList<>();
		try {
			List<ProductImage> existingImages = images.findByProduct_IdOrderByDisplayOrderAscIdAsc(productId);
			if (existingImages.size() + files.size() > maximumImageCount) {
				throw new ResponseStatusException(
						HttpStatus.BAD_REQUEST,
						"A product can have at most " + maximumImageCount + " images.");
			}
			int nextDisplayOrder = existingImages.size();
			boolean hasPrimary = existingImages.stream()
					.anyMatch(ProductImage::isPrimary);
			for (MultipartFile file : files) {
				byte[] content = validateAndRead(file);
				StoredProductImage stored = storage.upload(content, "shopsphere/products/" + productId);
				uploadedPublicIds.add(stored.publicId());
				images.save(new ProductImage(
						product,
						stored.url(),
						stored.publicId(),
						nextDisplayOrder++,
						!hasPrimary));
				hasPrimary = true;
			}
		} catch (RuntimeException exception) {
			cleanupRemoteUploads(uploadedPublicIds, exception);
			throw exception;
		}
		return responseList(productId);
	}

	@Transactional
	public List<ProductImageResponse> setPrimary(Long userId, Long productId, Long imageId) {
		products.requireOwnedProduct(userId, productId);
		return setPrimaryForProduct(productId, imageId);
	}

	@Transactional
	public List<ProductImageResponse> setPrimaryForAdmin(Long productId, Long imageId) {
		products.requireAdminProduct(productId);
		return setPrimaryForProduct(productId, imageId);
	}

	private List<ProductImageResponse> setPrimaryForProduct(Long productId, Long imageId) {
		List<ProductImage> productImages = images.findByProduct_IdOrderByDisplayOrderAscIdAsc(productId);
		if (productImages.stream().noneMatch(image -> image.getId().equals(imageId))) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Product image was not found.");
		}
		productImages.forEach(image -> image.setPrimary(image.getId().equals(imageId)));
		images.saveAll(productImages);
		return responseList(productId);
	}

	@Transactional
	public List<ProductImageResponse> reorder(Long userId, Long productId, List<Long> imageIds) {
		products.requireOwnedProduct(userId, productId);
		return reorderForProduct(productId, imageIds);
	}

	@Transactional
	public List<ProductImageResponse> reorderForAdmin(Long productId, List<Long> imageIds) {
		products.requireAdminProduct(productId);
		return reorderForProduct(productId, imageIds);
	}

	private List<ProductImageResponse> reorderForProduct(Long productId, List<Long> imageIds) {
		List<ProductImage> productImages = images.findByProduct_IdOrderByDisplayOrderAscIdAsc(productId);
		Set<Long> existingIds = new HashSet<>(productImages.stream().map(ProductImage::getId).toList());
		Set<Long> requestedIds = new HashSet<>(imageIds);
		if (requestedIds.size() != imageIds.size() || !existingIds.equals(requestedIds)) {
			throw new ResponseStatusException(
					HttpStatus.BAD_REQUEST,
					"Image order must contain each product image exactly once.");
		}
		Map<Long, ProductImage> imagesById = productImages.stream()
				.collect(java.util.stream.Collectors.toMap(ProductImage::getId, image -> image));
		for (int index = 0; index < imageIds.size(); index++) {
			imagesById.get(imageIds.get(index)).setDisplayOrder(index);
		}
		images.saveAll(productImages);
		return responseList(productId);
	}

	@Transactional
	public List<ProductImageResponse> delete(Long userId, Long productId, Long imageId) {
		products.requireOwnedProduct(userId, productId);
		return deleteForProduct(productId, imageId);
	}

	@Transactional
	public List<ProductImageResponse> deleteForAdmin(Long productId, Long imageId) {
		products.requireAdminProduct(productId);
		return deleteForProduct(productId, imageId);
	}

	private List<ProductImageResponse> deleteForProduct(Long productId, Long imageId) {
		ProductImage image = images.findByIdAndProduct_Id(imageId, productId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product image was not found."));
		storage.delete(image.getPublicId());
		boolean wasPrimary = image.isPrimary();
		images.delete(image);
		if (wasPrimary) {
			images.findByProduct_IdOrderByDisplayOrderAscIdAsc(productId).stream()
					.findFirst()
					.ifPresent(remaining -> remaining.setPrimary(true));
		}
		return responseList(productId);
	}

	private List<ProductImageResponse> responseList(Long productId) {
		return images.findByProduct_IdOrderByDisplayOrderAscIdAsc(productId).stream()
				.map(ProductImageResponse::from)
				.toList();
	}

	private byte[] validateAndRead(MultipartFile file) {
		if (file == null || file.isEmpty()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Image files cannot be empty.");
		}
		if (file.getSize() > maximumFileSize) {
			throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "Image exceeds the configured per-file size limit.");
		}

		String filename = file.getOriginalFilename();
		String extension = filename == null || filename.lastIndexOf('.') < 0
				? ""
				: filename.substring(filename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
		String expectedContentType = SUPPORTED_FORMATS.get(extension);
		if (expectedContentType == null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only JPG, PNG, and WebP images are supported.");
		}

		String declaredContentType = file.getContentType();
		if (declaredContentType == null
				|| !expectedContentType.equalsIgnoreCase(declaredContentType.split(";", 2)[0].trim())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Image file type does not match its extension.");
		}

		try {
			byte[] content = file.getBytes();
			if (!expectedContentType.equals(detectContentType(content))) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The uploaded file is not a supported image.");
			}
			return content;
		} catch (IOException exception) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The uploaded image could not be read.", exception);
		}
	}

	private String detectContentType(byte[] content) {
		if (content.length >= 3
				&& (content[0] & 0xFF) == 0xFF
				&& (content[1] & 0xFF) == 0xD8
				&& (content[2] & 0xFF) == 0xFF) {
			return "image/jpeg";
		}
		if (content.length >= 8
				&& (content[0] & 0xFF) == 0x89
				&& content[1] == 'P'
				&& content[2] == 'N'
				&& content[3] == 'G'
				&& content[4] == 0x0D
				&& content[5] == 0x0A
				&& content[6] == 0x1A
				&& content[7] == 0x0A) {
			return "image/png";
		}
		if (content.length >= 12
				&& content[0] == 'R'
				&& content[1] == 'I'
				&& content[2] == 'F'
				&& content[3] == 'F'
				&& content[8] == 'W'
				&& content[9] == 'E'
				&& content[10] == 'B'
				&& content[11] == 'P') {
			return "image/webp";
		}
		return "";
	}

	private void cleanupRemoteUploads(List<String> publicIds, RuntimeException originalException) {
		for (String publicId : publicIds) {
			try {
				storage.delete(publicId);
			} catch (RuntimeException cleanupException) {
				originalException.addSuppressed(cleanupException);
				LOGGER.error("Failed to clean up uploaded product image after a batch upload failure.", cleanupException);
			}
		}
	}
}
