package com.shopsphere.order.service;

import java.util.List;
import java.util.Map;
import java.util.Comparator;
import java.util.stream.Collectors;

import com.shopsphere.order.dto.OrderResponse;
import com.shopsphere.order.mapper.OrderMapper;
import com.shopsphere.order.repository.OrderRepository;
import com.shopsphere.product.repository.ProductImageRepository;
import com.shopsphere.product.entity.ProductImage;
import com.shopsphere.order.entity.Order;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class OrderService {

	private final OrderRepository orders;
	private final ProductImageRepository productImages;

	public OrderService(OrderRepository orders, ProductImageRepository productImages) {
		this.orders = orders;
		this.productImages = productImages;
	}

	@Transactional(readOnly = true)
	public OrderResponse getOrder(Long customerId, Long orderId) {
		Order order = orders.findByIdAndCustomer_Id(orderId, customerId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order was not found."));
		return OrderMapper.toResponse(order, getProductImages(List.of(order)));
	}

	@Transactional(readOnly = true)
	public List<OrderResponse> getMyOrders(Long customerId) {
		List<Order> customerOrders = orders.findByCustomer_IdOrderByCreatedAtDescIdDesc(customerId);
		Map<Long, String> images = getProductImages(customerOrders);
		return customerOrders.stream()
				.map(order -> OrderMapper.toResponse(order, images))
				.toList();
	}

	private Map<Long, String> getProductImages(List<Order> customerOrders) {
		List<Long> productIds = customerOrders.stream()
				.flatMap(order -> order.getSellerOrders().stream())
				.flatMap(sellerOrder -> sellerOrder.getItems().stream())
				.map(item -> item.getProduct().getId())
				.distinct()
				.toList();
		if (productIds.isEmpty()) return Map.of();
		return productImages.findByProduct_IdInOrderByDisplayOrderAscIdAsc(productIds).stream()
				.sorted(Comparator.comparing(ProductImage::isPrimary).reversed()
						.thenComparing(ProductImage::getDisplayOrder))
				.collect(Collectors.toMap(
						image -> image.getProduct().getId(),
						ProductImage::getUrl,
						(existing, next) -> existing));
	}
}
