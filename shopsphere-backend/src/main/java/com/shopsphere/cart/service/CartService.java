package com.shopsphere.cart.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.shopsphere.cart.dto.AddToCartRequest;
import com.shopsphere.cart.dto.CartResponse;
import com.shopsphere.cart.dto.UpdateCartItemRequest;
import com.shopsphere.cart.entity.Cart;
import com.shopsphere.cart.entity.CartItem;
import com.shopsphere.cart.repository.CartItemRepository;
import com.shopsphere.cart.repository.CartRepository;
import com.shopsphere.product.entity.Product;
import com.shopsphere.product.entity.ProductImage;
import com.shopsphere.product.repository.ProductImageRepository;
import com.shopsphere.product.service.ProductService;
import com.shopsphere.user.ShopUserRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CartService {

	private final CartRepository carts;
	private final CartItemRepository cartItems;
	private final ProductService products;
	private final ProductImageRepository productImages;
	private final ShopUserRepository users;

	public CartService(
			CartRepository carts,
			CartItemRepository cartItems,
			ProductService products,
			ProductImageRepository productImages,
			ShopUserRepository users) {
		this.carts = carts;
		this.cartItems = cartItems;
		this.products = products;
		this.productImages = productImages;
		this.users = users;
	}

	@Transactional
	public CartResponse getCart(Long customerId) {
		return response(lockOrCreateCart(customerId));
	}

	@Transactional
	public CartResponse addItem(Long customerId, AddToCartRequest request) {
		Cart cart = lockOrCreateCart(customerId);
		Product product = products.requirePublicProduct(request.productId());
		CartItem existingItem = cartItems.findByCart_IdAndProduct_Id(cart.getId(), product.getId()).orElse(null);
		long requestedQuantity = (long) request.quantity()
				+ (existingItem == null ? 0 : existingItem.getQuantity());
		validateQuantity(product, requestedQuantity);

		if (existingItem == null) {
			cart.addItem(product, (int) requestedQuantity);
		} else {
			existingItem.updateQuantity((int) requestedQuantity);
		}

		return response(carts.save(cart));
	}

	@Transactional
	public CartResponse updateItem(Long customerId, Long itemId, UpdateCartItemRequest request) {
		Cart cart = lockOrCreateCart(customerId);
		CartItem item = findOwnedItem(customerId, itemId);
		Product product = products.requirePublicProduct(item.getProduct().getId());
		validateQuantity(product, request.quantity());
		item.updateQuantity(request.quantity());
		return response(cart);
	}

	@Transactional
	public CartResponse removeItem(Long customerId, Long itemId) {
		lockOrCreateCart(customerId);
		CartItem item = findOwnedItem(customerId, itemId);
		Cart cart = item.getCart();
		cart.removeItem(item);
		return response(cart);
	}

	@Transactional
	public CartResponse clearCart(Long customerId) {
		Cart cart = lockOrCreateCart(customerId);
		cart.clearItems();
		return response(carts.save(cart));
	}

	private CartResponse response(Cart cart) {
		List<Long> productIds = cart.getItems().stream()
				.map(item -> item.getProduct().getId())
				.distinct()
				.toList();
		Map<Long, String> imageUrls = new HashMap<>();
		if (!productIds.isEmpty()) {
			for (ProductImage image : productImages.findByProduct_IdInOrderByDisplayOrderAscIdAsc(productIds)) {
				Long productId = image.getProduct().getId();
				if (image.isPrimary() || !imageUrls.containsKey(productId)) {
					imageUrls.put(productId, image.getUrl());
				}
			}
		}

		List<com.shopsphere.cart.dto.CartItemResponse> items = cart.getItems().stream()
				.map(item -> com.shopsphere.cart.dto.CartItemResponse.from(
						item,
						imageUrls.get(item.getProduct().getId())))
				.toList();
		return CartResponse.from(cart, items);
	}

	private Cart lockOrCreateCart(Long customerId) {
		users.findByIdForUpdate(customerId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer account was not found."));
		return carts.findByCustomer_IdForUpdate(customerId)
				.orElseGet(() -> carts.save(new Cart(users.getReferenceById(customerId))));
	}

	private CartItem findOwnedItem(Long customerId, Long itemId) {
		return cartItems.findByIdAndCart_Customer_Id(itemId, customerId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cart item was not found."));
	}

	private void validateQuantity(Product product, long quantity) {
		if (product.getStockQuantity() < 1) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "This product is out of stock.");
		}
		if (quantity < 1 || quantity > product.getStockQuantity()) {
			throw new ResponseStatusException(
					HttpStatus.CONFLICT,
					"Requested quantity exceeds the available stock.");
		}
	}
}