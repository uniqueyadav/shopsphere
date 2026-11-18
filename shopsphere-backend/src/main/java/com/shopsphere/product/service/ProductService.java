package com.shopsphere.product.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import com.shopsphere.category.entity.Category;
import com.shopsphere.category.repository.CategoryRepository;
import com.shopsphere.common.enums.ProductStatus;
import com.shopsphere.inventory.entity.Inventory;
import com.shopsphere.inventory.repository.InventoryRepository;
import com.shopsphere.common.enums.Role;
import com.shopsphere.common.enums.AccountStatus;
import com.shopsphere.common.enums.SellerStatus;
import com.shopsphere.product.dto.AdminProductCreateRequest;
import com.shopsphere.product.dto.AdminProductStatsResponse;
import com.shopsphere.product.dto.AdminSellerOptionResponse;
import com.shopsphere.product.dto.ProductPageResponse;
import com.shopsphere.product.dto.ProductBrandFacet;
import com.shopsphere.product.dto.ProductCategoryFacet;
import com.shopsphere.product.dto.ProductFacetsResponse;
import com.shopsphere.product.dto.ProductReviewSummaryResponse;
import com.shopsphere.product.dto.ProductImageResponse;
import com.shopsphere.product.dto.ProductRequest;
import com.shopsphere.product.dto.ProductResponse;
import com.shopsphere.product.entity.Product;
import com.shopsphere.product.entity.ProductImage;
import com.shopsphere.product.repository.ProductImageRepository;
import com.shopsphere.product.repository.ProductRepository;
import com.shopsphere.order.entity.OrderItem;
import com.shopsphere.order.repository.OrderItemRepository;
import com.shopsphere.cart.repository.CartItemRepository;
import com.shopsphere.wishlist.repository.WishlistItemRepository;
import com.shopsphere.common.enums.OrderStatus;
import com.shopsphere.review.repository.ReviewRepository;
import com.shopsphere.seller.entity.Seller;
import com.shopsphere.seller.repository.SellerRepository;
import com.shopsphere.user.ShopUserRepository;
import com.shopsphere.user.entity.User;

import jakarta.persistence.criteria.Predicate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ProductService {
	private static final Logger LOGGER = LoggerFactory.getLogger(ProductService.class);

	private final ProductRepository products;
	private final ProductImageRepository productImages;
	private final SellerRepository sellers;
	private final CategoryRepository categories;
	private final ShopUserRepository users;
	private final ReviewRepository reviews;
	private final InventoryRepository inventoryHistory;
	private final OrderItemRepository orderItems;
	private final CartItemRepository cartItems;
	private final WishlistItemRepository wishlistItems;
	private final ProductImageStorage imageStorage;

	public ProductService(
			ProductRepository products,
			ProductImageRepository productImages,
			SellerRepository sellers,
			CategoryRepository categories,
			ShopUserRepository users,
			ReviewRepository reviews,
			InventoryRepository inventoryHistory,
			OrderItemRepository orderItems,
			CartItemRepository cartItems,
			WishlistItemRepository wishlistItems,
			ProductImageStorage imageStorage) {
		this.products = products;
		this.productImages = productImages;
		this.sellers = sellers;
		this.categories = categories;
		this.users = users;
		this.reviews = reviews;
		this.inventoryHistory = inventoryHistory;
		this.orderItems = orderItems;
		this.cartItems = cartItems;
		this.wishlistItems = wishlistItems;
		this.imageStorage = imageStorage;
	}

	@Transactional
	public ProductResponse create(Long userId, ProductRequest request) {
		Seller seller = findApprovedSeller(userId);
		Category category = findActiveCategory(request.categoryId());
		ProductValues values = values(request, null);
		Product product = new Product(
				seller,
				category,
				values.productName(),
				values.slug(),
				values.description(),
				values.brand(),
				values.sku(),
				values.price(),
				values.discountPrice(),
				values.stockQuantity());
		Product saved = products.save(product);
		if (saved.getStockQuantity() > 0) inventoryHistory.save(new Inventory(saved, seller.getUser(), 0, saved.getStockQuantity(), "PRODUCT_CREATED"));
		return response(saved);
	}

	@Transactional
	public ProductResponse update(Long userId, Long productId, ProductRequest request) {
		Product product = findOwnedProduct(userId, productId);
		Category category = findActiveCategory(request.categoryId());
		ProductValues values = values(request, productId);
		int previousStock = product.getStockQuantity();
		product.update(
				category,
				values.productName(),
				values.slug(),
				values.description(),
				values.brand(),
				values.sku(),
				values.price(),
				values.discountPrice(),
				values.stockQuantity());
		if (previousStock != product.getStockQuantity()) inventoryHistory.save(new Inventory(product, product.getSeller().getUser(), previousStock, product.getStockQuantity(), "PRODUCT_EDIT"));
		return response(product);
	}

	@Transactional
	public void delete(Long userId, Long productId) {
		Product product = findOwnedProduct(userId, productId);
		if (orderItems.countByProduct_Id(productId) > 0) {
			throw new ResponseStatusException(HttpStatus.CONFLICT,
					"This product is linked to existing orders and cannot be permanently deleted.");
		}
		List<ProductImage> images = productImages.findByProduct_IdOrderByDisplayOrderAscIdAsc(productId);
		for (ProductImage image : images) {
			try {
				imageStorage.delete(image.getPublicId());
			} catch (RuntimeException exception) {
				LOGGER.warn("Could not remove remote image {} while deleting product {}.", image.getPublicId(), productId, exception);
			}
		}
		cartItems.deleteByProduct_Id(productId);
		wishlistItems.deleteByProduct_Id(productId);
		reviews.deleteByProduct_Id(productId);
		productImages.deleteByProduct_Id(productId);
		inventoryHistory.deleteByProduct_Id(productId);
		products.delete(product);
	}

	@Transactional
	public ProductResponse submitForApproval(Long userId, Long productId) {
		Product product = findOwnedProduct(userId, productId);
		if (!product.isActive()) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "An inactive product cannot be submitted.");
		}
		if (product.getStatus() != ProductStatus.DRAFT && product.getStatus() != ProductStatus.REJECTED) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Only draft or rejected products can be submitted.");
		}
		product.submitForApproval();
		return response(product);
	}

	@Transactional(readOnly = true)
	public ProductPageResponse listSellerProducts(
			Long userId,
			int page,
			int size,
			String search,
			ProductStatus status,
			Long categoryId,
			Boolean active,
			Boolean outOfStock,
			String sort) {
		Seller seller = findApprovedSeller(userId);
		Specification<Product> specification = buildSpecification(search, status, categoryId, null, null, null)
				.and((root, query, builder) -> {
					List<Predicate> predicates = new ArrayList<>();
					predicates.add(builder.equal(root.get("seller").get("id"), seller.getId()));
					if (active != null) {
						predicates.add(builder.equal(root.get("isActive"), active));
					}
					if (Boolean.TRUE.equals(outOfStock)) {
						predicates.add(builder.lessThanOrEqualTo(root.get("stockQuantity"), 0));
					} else if (Boolean.FALSE.equals(outOfStock)) {
						predicates.add(builder.greaterThan(root.get("stockQuantity"), 0));
					}
					return builder.and(predicates.toArray(Predicate[]::new));
				});
		return page(products.findAll(specification, sellerPageRequest(page, size, sort)));
	}

	private PageRequest sellerPageRequest(int page, int size, String sort) {
		Sort sorting = switch (sort == null ? "newest" : sort) {
			case "name_asc" -> Sort.by(Sort.Order.asc("productName"), Sort.Order.asc("id"));
			case "name_desc" -> Sort.by(Sort.Order.desc("productName"), Sort.Order.desc("id"));
			case "price_asc" -> Sort.by(Sort.Order.asc("price"), Sort.Order.asc("id"));
			case "price_desc" -> Sort.by(Sort.Order.desc("price"), Sort.Order.desc("id"));
			case "stock_asc" -> Sort.by(Sort.Order.asc("stockQuantity"), Sort.Order.asc("id"));
			case "stock_desc" -> Sort.by(Sort.Order.desc("stockQuantity"), Sort.Order.desc("id"));
			default -> Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));
		};
		return PageRequest.of(page, size, sorting);
	}

	@Transactional(readOnly = true)
	public ProductResponse getSellerProduct(Long userId, Long productId) {
		return response(findOwnedProduct(userId, productId));
	}

	@Transactional(readOnly = true)
	public Product requireOwnedProduct(Long userId, Long productId) {
		return findOwnedProduct(userId, productId);
	}

	@Transactional(readOnly = true)
	public ProductPageResponse listPublicProducts(
			int page,
			int size,
			String search,
			Long categoryId,
			String brand,
			BigDecimal minPrice,
			BigDecimal maxPrice,
			boolean discountedOnly,
			String availability,
			String sort,
			Integer minimumRating) {
		validatePriceRange(minPrice, maxPrice);
		validateAvailability(availability);
		Specification<Product> specification = buildSpecification(
				search, ProductStatus.APPROVED, categoryId, brand, minPrice, maxPrice)
				.and((root, query, builder) -> {
					List<Predicate> predicates = new ArrayList<>();
					predicates.add(builder.isTrue(root.get("isActive")));
					if (discountedOnly) {
						predicates.add(builder.isNotNull(root.get("discountPrice")));
					}
					if ("IN_STOCK".equalsIgnoreCase(availability)) {
						predicates.add(builder.greaterThan(root.get("stockQuantity"), 0));
					} else if ("OUT_OF_STOCK".equalsIgnoreCase(availability)) {
						predicates.add(builder.equal(root.get("stockQuantity"), 0));
					}
					return builder.and(predicates.toArray(Predicate[]::new));
				});
		if (minimumRating != null) {
			List<Long> ratedProductIds = reviews.productIdsWithAverageRatingAtLeast(minimumRating.doubleValue());
			specification = specification.and((root, query, builder) -> ratedProductIds.isEmpty()
					? builder.disjunction()
					: root.get("id").in(ratedProductIds));
		}
		if ("popular".equals(sort)) {
			specification = specification.and((root, query, builder) -> {
				var sales = query.subquery(Long.class);
				var item = sales.from(OrderItem.class);
				sales.select(builder.coalesce(builder.sum(item.<Integer>get("quantity")).as(Long.class), 0L));
				sales.where(
						builder.equal(item.get("product"), root),
						builder.notEqual(item.get("sellerOrder").get("order").get("orderStatus"), OrderStatus.CANCELLED),
						builder.notEqual(item.get("sellerOrder").get("order").get("orderStatus"), OrderStatus.PAYMENT_PENDING));
				query.orderBy(builder.desc(sales), builder.desc(root.get("createdAt")));
				return builder.conjunction();
			});
		}
		return page(products.findAll(specification, publicPageRequest(page, size, sort)));
	}

	@Transactional(readOnly = true)
	public ProductFacetsResponse getPublicProductFacets() {
		List<ProductBrandFacet> brands = products.findPublicBrandFacets().stream()
				.map(row -> new ProductBrandFacet((String) row[0], ((Number) row[1]).longValue()))
				.toList();
		List<ProductCategoryFacet> categoryFacets = products.findPublicCategoryFacets().stream()
				.map(row -> new ProductCategoryFacet(((Number) row[0]).longValue(), (String) row[1], ((Number) row[2]).longValue()))
				.toList();
		return new ProductFacetsResponse(brands, categoryFacets);
	}

	@Transactional(readOnly = true)
	public ProductReviewSummaryResponse getPublicReviewSummary(Long productId) {
		Product product = requirePublicProduct(productId);
		Double average = reviews.averageForProduct(product.getId());
		Map<Integer, Long> counts = new java.util.TreeMap<>();
		for (int star = 1; star <= 5; star++) counts.put(star, 0L);
		for (Object[] row : reviews.ratingDistribution(product.getId())) {
			counts.put(((Number) row[0]).intValue(), ((Number) row[1]).longValue());
		}
		long reviewCount = counts.values().stream().mapToLong(Long::longValue).sum();
		return new ProductReviewSummaryResponse(average == null ? 0d : average, reviewCount, counts);
	}

	@Transactional(readOnly = true)
	public ProductResponse getPublicProduct(Long productId) {
		Product product = requirePublicProduct(productId);
		return response(product);
	}

	@Transactional(readOnly = true)
	public ProductResponse getPublicProductBySlug(String slug) {
		Product product = products.findBySlugIgnoreCase(slug)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product was not found."));
		return response(requirePublicProduct(product));
	}

	@Transactional(readOnly = true)
	public Product requirePublicProduct(Long productId) {
		return requirePublicProduct(findProduct(productId));
	}

	private Product requirePublicProduct(Product product) {
		if (product.getStatus() != ProductStatus.APPROVED || !product.isActive()) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Product was not found.");
		}
		return product;
	}

	@Transactional(readOnly = true)
	public ProductPageResponse listAdminProducts(
			int page,
			int size,
			String search,
			ProductStatus status,
			Long categoryId,
			Long sellerId,
			Boolean active) {
		Specification<Product> specification = buildSpecification(search, status, categoryId, null, null, null)
				.and((root, query, builder) -> {
					List<Predicate> predicates = new ArrayList<>();
					if (sellerId != null) predicates.add(builder.equal(root.get("seller").get("id"), sellerId));
					if (active != null) predicates.add(builder.equal(root.get("isActive"), active));
					return builder.and(predicates.toArray(Predicate[]::new));
				});
		return page(products.findAll(
				specification,
				pageRequest(page, size)));
	}

	@Transactional(readOnly = true)
	public ProductResponse getAdminProduct(Long productId) {
		return response(findProduct(productId));
	}

	@Transactional(readOnly = true)
	public Product requireAdminProduct(Long productId) {
		return findProduct(productId);
	}

	@Transactional(readOnly = true)
	public AdminProductStatsResponse getAdminProductStats() {
		return new AdminProductStatsResponse(
				products.count(),
				products.countByStatus(ProductStatus.APPROVED),
				products.countByStatus(ProductStatus.PENDING),
				products.countByStatus(ProductStatus.REJECTED));
	}

	@Transactional(readOnly = true)
	public List<AdminSellerOptionResponse> getAdminSellerOptions() {
		return sellers.findAllByApprovalStatusAndUser_AccountStatusOrderByStoreNameAsc(
				SellerStatus.APPROVED, AccountStatus.ACTIVE).stream()
				.map(AdminSellerOptionResponse::from)
				.toList();
	}

	@Transactional
	public ProductResponse createAdminProduct(Long sellerId, ProductRequest request) {
		Seller seller = findActiveApprovedSeller(sellerId);
		Category category = findActiveCategory(request.categoryId());
		ProductValues values = values(request, null);
		Product saved = products.save(new Product(
				seller, category, values.productName(), values.slug(), values.description(), values.brand(),
				values.sku(), values.price(), values.discountPrice(), values.stockQuantity()));
		if (saved.getStockQuantity() > 0) inventoryHistory.save(new Inventory(saved, seller.getUser(), 0, saved.getStockQuantity(), "PRODUCT_CREATED"));
		return response(saved);
	}

	@Transactional
	public ProductResponse updateAdminProduct(Long productId, ProductRequest request) {
		Product product = findProduct(productId);
		Category category = findActiveCategory(request.categoryId());
		ProductValues values = values(request, productId);
		int previousStock = product.getStockQuantity();
		product.update(category, values.productName(), values.slug(), values.description(), values.brand(),
				values.sku(), values.price(), values.discountPrice(), values.stockQuantity());
		if (previousStock != product.getStockQuantity()) inventoryHistory.save(new Inventory(product, product.getSeller().getUser(), previousStock, product.getStockQuantity(), "PRODUCT_EDIT"));
		return response(product);
	}

	@Transactional
	public ProductResponse setAdminProductActive(Long productId, boolean active) {
		Product product = findProduct(productId);
		if (active) {
			product.activate();
		} else {
			product.deactivate();
		}
		return response(product);
	}

	@Transactional(readOnly = true)
	public String exportAdminProducts(String search, ProductStatus status, Long categoryId, Long sellerId, Boolean active) {
		Specification<Product> specification = buildSpecification(search, status, categoryId, null, null, null)
				.and((root, query, builder) -> {
					List<Predicate> predicates = new ArrayList<>();
					if (sellerId != null) predicates.add(builder.equal(root.get("seller").get("id"), sellerId));
					if (active != null) predicates.add(builder.equal(root.get("isActive"), active));
					return builder.and(predicates.toArray(Predicate[]::new));
				});
		StringBuilder csv = new StringBuilder("Product ID,Product,SKU,Category,Seller,Price,Discount Price,Stock,Status,Active,Created At\n");
		products.findAll(specification, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")))
				.forEach(product -> csv.append(product.getId()).append(',')
						.append(csv(product.getProductName())).append(',').append(csv(product.getSku())).append(',')
						.append(csv(product.getCategory().getName())).append(',').append(csv(product.getSeller().getStoreName())).append(',')
						.append(product.getPrice()).append(',').append(product.getDiscountPrice() == null ? "" : product.getDiscountPrice())
						.append(',').append(product.getStockQuantity()).append(',').append(product.getStatus()).append(',')
						.append(product.isActive()).append(',').append(product.getCreatedAt()).append('\n'));
		return csv.toString();
	}

	private String csv(String value) {
		return "\"" + (value == null ? "" : value.replace("\"", "\"\"")) + "\"";
	}

	@Transactional
	public ProductResponse approve(Long productId, Long reviewerId) {
		Product product = findProduct(productId);
		if (product.getStatus() != ProductStatus.PENDING && product.getStatus() != ProductStatus.DRAFT) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Only draft or pending products can be approved.");
		}
		if (!product.isActive()) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Inactive products cannot be approved.");
		}
		product.approve(findSuperAdmin(reviewerId), Instant.now());
		return response(product);
	}

	@Transactional
	public ProductResponse reject(Long productId, Long reviewerId, String reason) {
		Product product = findProduct(productId);
		if (product.getStatus() != ProductStatus.PENDING) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Only pending products can be reviewed.");
		}
		product.reject(findSuperAdmin(reviewerId), Instant.now(), reason.trim());
		return response(product);
	}

	private ProductPageResponse page(Page<Product> result) {
		List<Long> productIds = result.getContent().stream().map(Product::getId).toList();
		if (productIds.isEmpty()) {
			return ProductPageResponse.from(result.map(ProductResponse::from));
		}
		Map<Long, double[]> ratingsByProduct = reviews.ratingSummaries(productIds).stream().collect(Collectors.toMap(
				row -> ((Number) row[0]).longValue(),
				row -> new double[] { ((Number) row[1]).doubleValue(), ((Number) row[2]).longValue() }));
		Map<Long, List<ProductImageResponse>> imagesByProduct = productImages
				.findByProduct_IdInOrderByDisplayOrderAscIdAsc(productIds)
				.stream()
				.collect(Collectors.groupingBy(
						image -> image.getProduct().getId(),
						Collectors.mapping(ProductImageResponse::from, Collectors.toList())));
		return ProductPageResponse.from(result.map(product -> {
			double[] rating = ratingsByProduct.getOrDefault(product.getId(), new double[] { 0d, 0d });
			return ProductResponse.from(product, imagesByProduct.getOrDefault(product.getId(), List.of()), rating[0], (long) rating[1]);
		}));
	}

	private ProductResponse response(Product product) {
		List<ProductImageResponse> productImageResponses = productImages
				.findByProduct_IdOrderByDisplayOrderAscIdAsc(product.getId())
				.stream()
				.map(ProductImageResponse::from)
				.toList();
		List<Object[]> summaries = reviews.ratingSummaries(List.of(product.getId()));
		if (summaries.isEmpty()) return ProductResponse.from(product, productImageResponses);
		Object[] summary = summaries.get(0);
		return ProductResponse.from(product, productImageResponses, ((Number) summary[1]).doubleValue(), ((Number) summary[2]).longValue());
	}

	private PageRequest pageRequest(int page, int size) {
		return PageRequest.of(page, size, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
	}

	private PageRequest publicPageRequest(int page, int size, String sort) {
		if ("popular".equals(sort)) return PageRequest.of(page, size, Sort.unsorted());
		Sort sorting = switch (sort) {
			case "price_asc" -> Sort.by(Sort.Order.asc("price"), Sort.Order.asc("id"));
			case "price_desc" -> Sort.by(Sort.Order.desc("price"), Sort.Order.desc("id"));
			default -> Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));
		};
		return PageRequest.of(page, size, sorting);
	}

	private Specification<Product> buildSpecification(
			String rawSearch,
			ProductStatus status,
			Long categoryId,
			String rawBrand,
			BigDecimal minPrice,
			BigDecimal maxPrice) {
		return (root, query, builder) -> {
			List<Predicate> predicates = new ArrayList<>();
			if (status != null) {
				predicates.add(builder.equal(root.get("status"), status));
			}
			if (categoryId != null) {
				predicates.add(builder.equal(root.get("category").get("id"), categoryId));
			}
			if (rawBrand != null && !rawBrand.isBlank()) {
				predicates.add(builder.like(
						builder.lower(root.get("brand")),
						"%" + escapeLike(rawBrand.trim().toLowerCase(Locale.ROOT)) + "%",
						'\\'));
			}
			if (minPrice != null) {
				predicates.add(builder.greaterThanOrEqualTo(root.get("price"), minPrice));
			}
			if (maxPrice != null) {
				predicates.add(builder.lessThanOrEqualTo(root.get("price"), maxPrice));
			}
			if (rawSearch != null && !rawSearch.isBlank()) {
				String search = "%" + escapeLike(rawSearch.trim().toLowerCase(Locale.ROOT)) + "%";
				predicates.add(builder.or(
						builder.like(builder.lower(root.get("productName")), search, '\\'),
						builder.like(builder.lower(root.get("brand")), search, '\\'),
						builder.like(builder.lower(root.get("sku")), search, '\\'),
						builder.like(builder.lower(root.get("seller").get("storeName")), search, '\\')));
			}
			return builder.and(predicates.toArray(Predicate[]::new));
		};
	}

	private ProductValues values(ProductRequest request, Long currentProductId) {
		String productName = request.productName().trim();
		String slug = resolveSlug(request.slug(), productName);
		String sku = request.sku().trim().toUpperCase(Locale.ROOT);
		if (request.discountPrice() != null && request.discountPrice().compareTo(request.price()) >= 0) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Discount price must be less than the regular price.");
		}
		ensureUnique(slug, sku, currentProductId);
		return new ProductValues(
				productName,
				slug,
				normalizeOptional(request.description()),
				normalizeOptional(request.brand()),
				sku,
				request.price(),
				request.discountPrice(),
				request.stockQuantity());
	}

	private void ensureUnique(String slug, String sku, Long currentProductId) {
		boolean duplicateSlug = currentProductId == null
				? products.existsBySlugIgnoreCase(slug)
				: products.existsBySlugIgnoreCaseAndIdNot(slug, currentProductId);
		if (duplicateSlug) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "A product with this slug already exists.");
		}
		boolean duplicateSku = currentProductId == null
				? products.existsBySkuIgnoreCase(sku)
				: products.existsBySkuIgnoreCaseAndIdNot(sku, currentProductId);
		if (duplicateSku) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "A product with this SKU already exists.");
		}
	}

	private String resolveSlug(String requestedSlug, String productName) {
		String slug = requestedSlug == null || requestedSlug.isBlank()
				? productName.toLowerCase(Locale.ROOT)
						.replaceAll("[^\\p{L}\\p{N}]+", "-")
						.replaceAll("(^-|-$)", "")
				: requestedSlug.trim().toLowerCase(Locale.ROOT);
		if (slug.isBlank()) {
			throw new ResponseStatusException(
					HttpStatus.BAD_REQUEST,
					"Product name must contain a letter or number to generate a slug.");
		}
		return slug;
	}

	private Seller findApprovedSeller(Long userId) {
		Seller seller = sellers.findByUser_Id(userId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "An approved seller account is required."));
		if (seller.getApprovalStatus() != SellerStatus.APPROVED) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "An approved seller account is required.");
		}
		return seller;
	}

	private Seller findActiveApprovedSeller(Long sellerId) {
		Seller seller = sellers.findById(sellerId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Seller was not found."));
		if (seller.getApprovalStatus() != SellerStatus.APPROVED
				|| seller.getUser().getAccountStatus() != AccountStatus.ACTIVE) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Products can only be assigned to an active approved seller.");
		}
		return seller;
	}

	private Category findActiveCategory(Long categoryId) {
		Category category = categories.findById(categoryId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category was not found."));
		if (!category.isActive()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Products must use an active category.");
		}
		return category;
	}

	private Product findOwnedProduct(Long userId, Long productId) {
		Product product = findProduct(productId);
		if (!product.getSeller().getUser().getId().equals(userId)) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Product was not found.");
		}
		return product;
	}

	private Product findProduct(Long productId) {
		return products.findById(productId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product was not found."));
	}

	private User findSuperAdmin(Long reviewerId) {
		User reviewer = users.findById(reviewerId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication is required."));
		if (reviewer.getRole() != Role.SUPER_ADMIN) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only a super admin can review products.");
		}
		return reviewer;
	}

	private void validatePriceRange(BigDecimal minPrice, BigDecimal maxPrice) {
		if ((minPrice != null && minPrice.signum() < 0) || (maxPrice != null && maxPrice.signum() < 0)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Price filters cannot be negative.");
		}
		if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Minimum price cannot exceed maximum price.");
		}
	}

	private void validateAvailability(String availability) {
		if (!"ALL".equalsIgnoreCase(availability)
				&& !"IN_STOCK".equalsIgnoreCase(availability)
				&& !"OUT_OF_STOCK".equalsIgnoreCase(availability)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Availability filter is invalid.");
		}
	}

	private String escapeLike(String value) {
		return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
	}

	private String normalizeOptional(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}

	private record ProductValues(
			String productName,
			String slug,
			String description,
			String brand,
			String sku,
			BigDecimal price,
			BigDecimal discountPrice,
			int stockQuantity) {
	}
}
