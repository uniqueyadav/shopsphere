package com.shopsphere.coupon.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.shopsphere.cart.entity.CartItem;
import com.shopsphere.cart.repository.CartRepository;
import com.shopsphere.coupon.dto.CouponPreviewRequest;
import com.shopsphere.coupon.dto.CouponPreviewResponse;
import com.shopsphere.coupon.service.CouponService.CouponLine;
import com.shopsphere.coupon.service.CouponService.CouponQuote;

@Service
public class CustomerCouponPreviewService {
	private final CartRepository carts;
	private final CouponService coupons;

	public CustomerCouponPreviewService(CartRepository carts, CouponService coupons) {
		this.carts = carts;
		this.coupons = coupons;
	}

	@Transactional
	public CouponPreviewResponse preview(Long customerId, CouponPreviewRequest request) {
		var cart = carts.findByCustomer_Id(customerId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Your cart is empty."));
		List<CouponLine> lines = cart.getItems().stream().map(this::line).toList();
		if (lines.isEmpty()) {
			throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Your cart is empty.");
		}
		BigDecimal subtotal = lines.stream().map(CouponLine::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
		CouponQuote quote = coupons.quote(request.code(), customerId, lines, subtotal);
		return new CouponPreviewResponse(quote.coupon().getCode(), quote.discount());
	}

	private CouponLine line(CartItem item) {
		var product = item.getProduct();
		BigDecimal unitPrice = product.getDiscountPrice() != null && product.getDiscountPrice().compareTo(product.getPrice()) < 0
				? product.getDiscountPrice()
				: product.getPrice();
		return new CouponLine(product.getId(), product.getCategory().getId(), unitPrice.multiply(BigDecimal.valueOf(item.getQuantity())));
	}
}
