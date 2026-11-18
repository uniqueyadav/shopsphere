package com.shopsphere.wishlist.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.shopsphere.common.enums.ProductStatus;
import com.shopsphere.product.entity.Product;
import com.shopsphere.product.entity.ProductImage;
import com.shopsphere.product.repository.ProductImageRepository;
import com.shopsphere.product.service.ProductService;
import com.shopsphere.review.repository.ReviewRepository;
import com.shopsphere.user.ShopUserRepository;
import com.shopsphere.wishlist.dto.WishlistItemResponse;
import com.shopsphere.wishlist.dto.WishlistResponse;
import com.shopsphere.wishlist.entity.Wishlist;
import com.shopsphere.wishlist.entity.WishlistItem;
import com.shopsphere.wishlist.repository.WishlistItemRepository;
import com.shopsphere.wishlist.repository.WishlistRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class WishlistService {

	private final WishlistRepository wishlists;
	private final WishlistItemRepository wishlistItems;
	private final ProductService products;
	private final ProductImageRepository productImages;
	private final ShopUserRepository users;
	private final ReviewRepository reviews;

	public WishlistService(
			WishlistRepository wishlists,
			WishlistItemRepository wishlistItems,
			ProductService products,
			ProductImageRepository productImages,
			ShopUserRepository users,
			ReviewRepository reviews) {
		this.wishlists = wishlists;
		this.wishlistItems = wishlistItems;
		this.products = products;
		this.productImages = productImages;
		this.users = users;
		this.reviews = reviews;
	}

	@Transactional
	public WishlistResponse getWishlist(Long customerId) {
		return response(getOrCreateWishlist(customerId));
	}

	@Transactional
	public WishlistResponse addItem(Long customerId, Long productId) {
		Wishlist wishlist = getOrCreateWishlist(customerId);
		Product product = products.requirePublicProduct(productId);
		if (wishlistItems.findByWishlist_IdAndProduct_Id(wishlist.getId(), productId).isEmpty()) {
			wishlist.addItem(product);
			wishlists.save(wishlist);
		}
		return response(wishlists.findByCustomer_Id(customerId).orElseThrow());
	}

	@Transactional
	public WishlistResponse removeItem(Long customerId, Long productId) {
		Wishlist wishlist = getOrCreateWishlist(customerId);
		WishlistItem item = wishlistItems.findByWishlist_IdAndProduct_Id(wishlist.getId(), productId)
				.orElseThrow(() -> new ResponseStatusException(
						HttpStatus.NOT_FOUND,
						"Wishlist item was not found."));
		wishlist.removeItem(item);
		wishlists.save(wishlist);
		return response(wishlist);
	}

	@Transactional
	public WishlistResponse clearWishlist(Long customerId) {
		Wishlist wishlist = getOrCreateWishlist(customerId);
		wishlist.clearItems();
		return response(wishlists.save(wishlist));
	}

	private WishlistResponse response(Wishlist wishlist) {
		List<WishlistItem> publicItems = wishlist.getItems().stream()
				.filter(item -> item.getProduct().getStatus() == ProductStatus.APPROVED)
				.filter(item -> item.getProduct().isActive())
				.toList();
		List<Long> productIds = publicItems.stream()
				.map(item -> item.getProduct().getId())
				.distinct()
				.toList();
		Map<Long, String> imageUrls = new HashMap<>();
		Map<Long, RatingSummary> ratingSummaries = new HashMap<>();
		if (!productIds.isEmpty()) {
			for (ProductImage image : productImages.findByProduct_IdInOrderByDisplayOrderAscIdAsc(productIds)) {
				Long productId = image.getProduct().getId();
				if (image.isPrimary() || !imageUrls.containsKey(productId)) {
					imageUrls.put(productId, image.getUrl());
				}
			}
			for (Object[] rating : reviews.ratingSummaries(productIds)) {
				ratingSummaries.put(
						(Long) rating[0],
						new RatingSummary(((Number) rating[1]).doubleValue(), ((Number) rating[2]).longValue()));
			}
		}
		List<WishlistItemResponse> items = publicItems.stream()
				.map(item -> {
					Long productId = item.getProduct().getId();
					RatingSummary rating = ratingSummaries.getOrDefault(productId, new RatingSummary(0d, 0L));
					return WishlistItemResponse.from(item, imageUrls.get(productId), rating.averageRating(), rating.reviewCount());
				})
				.toList();
		return WishlistResponse.from(wishlist, items);
	}

	private record RatingSummary(double averageRating, long reviewCount) {}

	private Wishlist getOrCreateWishlist(Long customerId) {
		return wishlists.findByCustomer_Id(customerId)
				.orElseGet(() -> wishlists.save(new Wishlist(users.getReferenceById(customerId))));
	}
}
