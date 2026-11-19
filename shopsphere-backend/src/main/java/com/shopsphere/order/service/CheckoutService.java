package com.shopsphere.order.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.shopsphere.cart.entity.Cart;
import com.shopsphere.cart.entity.CartItem;
import com.shopsphere.cart.repository.CartRepository;
import com.shopsphere.coupon.service.CouponService;
import com.shopsphere.coupon.service.CouponService.CouponLine;
import com.shopsphere.coupon.service.CouponService.CouponQuote;
import com.shopsphere.common.enums.PaymentMethod;
import com.shopsphere.common.enums.ProductStatus;
import com.shopsphere.common.enums.SellerStatus;
import com.shopsphere.order.dto.CheckoutRequest;
import com.shopsphere.order.dto.CheckoutResult;
import com.shopsphere.order.entity.Order;
import com.shopsphere.order.entity.SellerOrder;
import com.shopsphere.order.mapper.OrderMapper;
import com.shopsphere.order.repository.OrderRepository;
import com.shopsphere.inventory.service.InventoryService;
import com.shopsphere.product.entity.Product;
import com.shopsphere.product.repository.ProductRepository;
import com.shopsphere.seller.entity.Seller;
import com.shopsphere.seller.repository.SellerRepository;
import com.shopsphere.user.ShopUserRepository;
import com.shopsphere.user.entity.Address;
import com.shopsphere.user.entity.User;
import com.shopsphere.user.repository.AddressRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CheckoutService {

	private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2);
	private static final BigDecimal MAX_ORDER_AMOUNT = new BigDecimal("9999999999.99");

	private final OrderRepository orders;
	private final CartRepository carts;
	private final ShopUserRepository users;
	private final AddressRepository addresses;
	private final ProductRepository products;
	private final SellerRepository sellers;
	private final CouponService coupons;
	private final InventoryService inventory;

	public CheckoutService(
			OrderRepository orders,
			CartRepository carts,
			ShopUserRepository users,
			AddressRepository addresses,
			ProductRepository products,
			SellerRepository sellers,
			CouponService coupons,
			InventoryService inventory) {
		this.orders = orders;
		this.carts = carts;
		this.users = users;
		this.addresses = addresses;
		this.products = products;
		this.sellers = sellers;
		this.coupons = coupons;
		this.inventory = inventory;
	}

	@Transactional
	public CheckoutResult checkout(Long customerId, String requestKey, CheckoutRequest request) {
		String idempotencyKey = requestKey.trim();
		User customer = users.findByIdForUpdate(customerId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer account was not found."));
		Order previousOrder = orders.findByCustomer_IdAndIdempotencyKey(customerId, idempotencyKey).orElse(null);
		if (previousOrder != null) {
			return replayIfMatching(previousOrder, request);
		}

		Cart cart = carts.findByCustomer_IdForUpdate(customerId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "Your cart is empty."));

		List<CartItem> cartItems = List.copyOf(cart.getItems());
		if (cartItems.isEmpty()) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Your cart is empty.");
		}

		Address shippingAddress = addresses.findByIdAndUser_Id(request.addressId(), customerId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Shipping address was not found."));
		List<Long> productIds = cartItems.stream()
				.map(item -> item.getProduct().getId())
				.distinct()
				.sorted()
				.toList();
		Map<Long, Product> lockedProducts = new HashMap<>();
		for (Product product : products.findAllByIdInForUpdate(productIds)) {
			lockedProducts.put(product.getId(), product);
		}
		if (lockedProducts.size() != productIds.size()) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "A product in your cart is no longer available.");
		}

		Map<Long, List<CheckoutLine>> linesBySeller = new TreeMap<>();
		for (CartItem cartItem : cartItems) {
			Product product = lockedProducts.get(cartItem.getProduct().getId());
			if (product.getStatus() != ProductStatus.APPROVED || !product.isActive()) {
				throw new ResponseStatusException(HttpStatus.CONFLICT, "A product in your cart is no longer available.");
			}
			if (cartItem.getQuantity() > product.getStockQuantity()) {
				throw new ResponseStatusException(
						HttpStatus.CONFLICT,
						"Not enough stock is available for " + product.getProductName() + ".");
			}
			BigDecimal unitPrice = currentPrice(product);
			if (unitPrice.signum() <= 0) {
				throw new ResponseStatusException(HttpStatus.CONFLICT, "A product in your cart has an invalid price.");
			}
			linesBySeller.computeIfAbsent(product.getSeller().getId(), ignored -> new ArrayList<>())
					.add(new CheckoutLine(product, cartItem.getQuantity(), unitPrice));
		}

		List<Long> sellerIds = List.copyOf(linesBySeller.keySet());
		Map<Long, Seller> lockedSellers = new HashMap<>();
		for (Seller seller : sellers.findAllByIdInForUpdate(sellerIds)) {
			lockedSellers.put(seller.getId(), seller);
		}
		if (lockedSellers.size() != sellerIds.size()
				|| lockedSellers.values().stream().anyMatch(seller -> seller.getApprovalStatus() != SellerStatus.APPROVED)) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "A seller in your cart is no longer available.");
		}

		BigDecimal subtotal = ZERO;
		BigDecimal deliveryCharges = ZERO;
		Map<Long, BigDecimal> sellerSubtotals = new HashMap<>();
		for (Map.Entry<Long, List<CheckoutLine>> entry : linesBySeller.entrySet()) {
			BigDecimal sellerSubtotal = entry.getValue().stream()
					.map(CheckoutLine::lineSubtotal)
					.reduce(ZERO, BigDecimal::add);
			sellerSubtotals.put(entry.getKey(), sellerSubtotal);
			subtotal = subtotal.add(sellerSubtotal);
			deliveryCharges = deliveryCharges.add(deliveryFee(lockedSellers.get(entry.getKey()), shippingAddress));
		}
		BigDecimal totalAmount = subtotal.add(deliveryCharges);
		List<CouponLine> couponLines = new ArrayList<>();
		for (List<CheckoutLine> sellerLines : linesBySeller.values()) {
			for (CheckoutLine line : sellerLines) {
				Long categoryId = line.product().getCategory() == null ? null : line.product().getCategory().getId();
				couponLines.add(new CouponLine(line.product().getId(), categoryId, line.lineSubtotal()));
			}
		}
		CouponQuote couponQuote = coupons.quote(request.couponCode(), customerId, couponLines, subtotal);
		if (subtotal.compareTo(MAX_ORDER_AMOUNT) > 0
				|| deliveryCharges.compareTo(MAX_ORDER_AMOUNT) > 0
				|| totalAmount.compareTo(MAX_ORDER_AMOUNT) > 0) {
			throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Order total exceeds the supported amount.");
		}

		Order order = new Order(
				customer,
				idempotencyKey,
				shippingAddress.getId(),
				shippingAddress,
				request.paymentMethod(),
				subtotal,
				deliveryCharges,
				totalAmount);
		if (couponQuote != null) order.applyCoupon(couponQuote.coupon().getCode(), couponQuote.discount());
		for (Map.Entry<Long, List<CheckoutLine>> entry : linesBySeller.entrySet()) {
			Seller seller = lockedSellers.get(entry.getKey());
			BigDecimal sellerSubtotal = sellerSubtotals.get(entry.getKey());
			BigDecimal deliveryCharge = deliveryFee(seller, shippingAddress);
			SellerOrder sellerOrder = order.addSellerOrder(
					seller,
					sellerSubtotal,
					deliveryCharge,
					sellerSubtotal.add(deliveryCharge));
			for (CheckoutLine line : entry.getValue()) {
				int previousQuantity = line.product().getStockQuantity();
				line.product().decreaseStock(line.quantity());
				inventory.recordChange(line.product(), previousQuantity, line.product().getStockQuantity(), "ORDER_PLACED");
				sellerOrder.addItem(line.product(), line.quantity(), line.unitPrice());
			}
		}

		Order savedOrder = orders.save(order);
		coupons.recordUsage(couponQuote, savedOrder, customer);
		cart.clearItems();
		return new CheckoutResult(OrderMapper.toResponse(savedOrder), false);
	}

	private CheckoutResult replayIfMatching(Order order, CheckoutRequest request) {
		if (!order.getShippingAddressId().equals(request.addressId())
				|| order.getPaymentMethod() != request.paymentMethod()
				|| !java.util.Objects.equals(order.getCouponCode(), request.couponCode() == null ? null : request.couponCode().trim().toUpperCase(java.util.Locale.ROOT))) {
			throw new ResponseStatusException(
					HttpStatus.CONFLICT,
					"Idempotency key was already used for a different checkout request.");
		}
		return new CheckoutResult(OrderMapper.toResponse(order), true);
	}

	private BigDecimal currentPrice(Product product) {
		BigDecimal discountPrice = product.getDiscountPrice();
		return discountPrice != null && discountPrice.compareTo(product.getPrice()) < 0
				? discountPrice
				: product.getPrice();
	}

	private BigDecimal deliveryFee(Seller seller, Address address) {
		if (seller.getPickupLatitude() == null || seller.getPickupLongitude() == null || address.getLatitude() == null || address.getLongitude() == null) {
			return seller.getDeliveryFee().setScale(2, java.math.RoundingMode.HALF_UP);
		}
		double lat1 = Math.toRadians(seller.getPickupLatitude()), lat2 = Math.toRadians(address.getLatitude());
		double dLat = lat2 - lat1, dLon = Math.toRadians(address.getLongitude() - seller.getPickupLongitude());
		double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) + Math.cos(lat1) * Math.cos(lat2) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
		a = Math.min(1.0, Math.max(0.0, a));
		double km = 6371.0 * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
		long billableKm = (long) Math.ceil(km);
		return new BigDecimal("20.00").add(new BigDecimal("5.00").multiply(BigDecimal.valueOf(billableKm))).setScale(2, java.math.RoundingMode.HALF_UP);
	}

	private record CheckoutLine(Product product, int quantity, BigDecimal unitPrice) {
		BigDecimal lineSubtotal() {
			return unitPrice.multiply(BigDecimal.valueOf(quantity));
		}
	}
}
