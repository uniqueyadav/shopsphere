package com.shopsphere.inventory.service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.shopsphere.common.enums.SellerStatus;
import com.shopsphere.inventory.dto.InventoryItemResponse;
import com.shopsphere.inventory.dto.InventoryPageResponse;
import com.shopsphere.inventory.dto.InventoryResponse;
import com.shopsphere.inventory.dto.InventorySummaryResponse;
import com.shopsphere.inventory.entity.Inventory;
import com.shopsphere.inventory.repository.InventoryRepository;
import com.shopsphere.product.dto.ProductImageResponse;
import com.shopsphere.product.entity.Product;
import com.shopsphere.product.repository.ProductImageRepository;
import com.shopsphere.product.repository.ProductRepository;
import com.shopsphere.seller.entity.Seller;
import com.shopsphere.seller.repository.SellerRepository;
import com.shopsphere.user.ShopUserRepository;

import jakarta.persistence.criteria.Predicate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class InventoryService {
	public static final int LOW_STOCK_THRESHOLD = 10;

	private final ProductRepository products;
	private final ProductImageRepository images;
	private final InventoryRepository history;
	private final SellerRepository sellers;
	private final ShopUserRepository users;

	public InventoryService(ProductRepository products, ProductImageRepository images, InventoryRepository history,
			SellerRepository sellers, ShopUserRepository users) {
		this.products = products;
		this.images = images;
		this.history = history;
		this.sellers = sellers;
		this.users = users;
	}

	@Transactional(readOnly = true)
	public InventoryPageResponse list(Long userId, int page, int size, String search, Long categoryId,
			String status, String stock, Instant updatedAfter, Instant updatedBefore) {
		requireApprovedSeller(userId);
		validateDateRange(updatedAfter, updatedBefore);
		Specification<Product> spec = (root, query, cb) -> {
			var terms = new java.util.ArrayList<Predicate>();
			terms.add(cb.equal(root.get("seller").get("user").get("id"), userId));
			if (categoryId != null) terms.add(cb.equal(root.get("category").get("id"), categoryId));
			if (search != null && !search.isBlank()) {
				String pattern = "%" + escape(search.trim().toLowerCase()) + "%";
				terms.add(cb.or(cb.like(cb.lower(root.get("productName")), pattern, '\\'),
						cb.like(cb.lower(root.get("sku")), pattern, '\\'), cb.like(cb.lower(root.get("brand")), pattern, '\\')));
			}
			String selectedStatus = status == null || status.isBlank() ? null : status.trim().toUpperCase();
			String selectedStock = stock == null || stock.isBlank() ? null : stock.trim().toUpperCase();
			validateStockFilter(selectedStatus);
			validateStockFilter(selectedStock);
			applyStockFilter(terms, cb, root, selectedStatus);
			applyStockFilter(terms, cb, root, selectedStock);
			if (updatedAfter != null) terms.add(cb.greaterThanOrEqualTo(root.get("updatedAt"), updatedAfter));
			if (updatedBefore != null) terms.add(cb.lessThanOrEqualTo(root.get("updatedAt"), updatedBefore));
			return cb.and(terms.toArray(Predicate[]::new));
		};
		Page<Product> productPage = products.findAll(spec, PageRequest.of(page, size, Sort.by(Sort.Order.desc("updatedAt"), Sort.Order.desc("id"))));
		List<Long> ids = productPage.getContent().stream().map(Product::getId).toList();
		Map<Long, List<ProductImageResponse>> imageMap = ids.isEmpty() ? Map.of() : images.findByProduct_IdInOrderByDisplayOrderAscIdAsc(ids)
				.stream().collect(Collectors.groupingBy(image -> image.getProduct().getId(),
						Collectors.mapping(ProductImageResponse::from, Collectors.toList())));
		return InventoryPageResponse.from(productPage.map(product -> InventoryItemResponse.from(product, imageMap.getOrDefault(product.getId(), List.of()))));
	}

	@Transactional(readOnly = true)
	public InventorySummaryResponse summary(Long userId) {
		requireApprovedSeller(userId);
		return new InventorySummaryResponse(products.countBySeller_User_Id(userId),
				products.countBySeller_User_IdAndStockQuantityGreaterThan(userId, LOW_STOCK_THRESHOLD),
				products.countBySeller_User_IdAndStockQuantityGreaterThanAndStockQuantityLessThanEqual(userId, 0, LOW_STOCK_THRESHOLD),
				products.countBySeller_User_IdAndStockQuantity(userId, 0));
	}

	@Transactional
	public InventoryItemResponse updateStock(Long userId, Long productId, int quantity) {
		if (quantity < 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Stock quantity cannot be negative.");
		Product product = products.findOwnedByIdForUpdate(productId, userId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product was not found."));
		int previous = product.getStockQuantity();
		if (previous != quantity) {
			product.setStockQuantity(quantity);
			var user = users.findById(userId)
					.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication is required."));
			history.save(new Inventory(product, user, previous, quantity, "SELLER_UPDATE"));
		}
		List<ProductImageResponse> productImages = images.findByProduct_IdOrderByDisplayOrderAscIdAsc(productId).stream().map(ProductImageResponse::from).toList();
		return InventoryItemResponse.from(product, productImages);
	}

	@Transactional(readOnly = true)
	public List<InventoryResponse> history(Long userId, Long productId) {
		requireOwnedProduct(userId, productId);
		return history.findTop100ByProduct_IdAndProduct_Seller_User_IdOrderByCreatedAtDescIdDesc(productId, userId)
				.stream().map(InventoryResponse::from).toList();
	}

	/** Called by checkout and cancellation flows inside their existing transaction. */
	@Transactional
	public void recordChange(Product product, int previousQuantity, int newQuantity, String changeType) {
		if (previousQuantity != newQuantity) history.save(new Inventory(product, null, previousQuantity, newQuantity, changeType));
	}

	private Product requireOwnedProduct(Long userId, Long productId) {
		return products.findById(productId)
				.filter(product -> product.getSeller().getUser().getId().equals(userId))
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product was not found."));
	}

	private Seller requireApprovedSeller(Long userId) {
		Seller seller = sellers.findByUser_Id(userId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "An approved seller account is required."));
		if (seller.getApprovalStatus() != SellerStatus.APPROVED) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "An approved seller account is required.");
		}
		return seller;
	}

	private void validateDateRange(Instant after, Instant before) {
		if (after != null && before != null && after.isAfter(before)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Updated-after must be before updated-before.");
		}
	}

	private void validateStockFilter(String value) {
		if (value != null && !List.of("IN_STOCK", "LOW_STOCK", "OUT_OF_STOCK").contains(value)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Inventory stock filter is invalid.");
		}
	}

	private void applyStockFilter(List<Predicate> terms, jakarta.persistence.criteria.CriteriaBuilder cb,
			jakarta.persistence.criteria.Root<Product> root, String value) {
		if ("OUT_OF_STOCK".equals(value)) terms.add(cb.equal(root.get("stockQuantity"), 0));
		if ("LOW_STOCK".equals(value)) terms.add(cb.between(root.get("stockQuantity"), 1, LOW_STOCK_THRESHOLD));
		if ("IN_STOCK".equals(value)) terms.add(cb.greaterThan(root.get("stockQuantity"), LOW_STOCK_THRESHOLD));
	}

	private String escape(String value) { return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_"); }
}
