package com.shopsphere.returnrequest.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.shopsphere.common.enums.OrderStatus;
import com.shopsphere.common.enums.ReturnRequestStatus;
import com.shopsphere.order.entity.OrderItem;
import com.shopsphere.order.entity.OrderStatusHistory;
import com.shopsphere.order.repository.OrderItemRepository;
import com.shopsphere.product.repository.ProductImageRepository;
import com.shopsphere.returnrequest.dto.CreateReturnRequest;
import com.shopsphere.returnrequest.dto.EligibleReturnItemResponse;
import com.shopsphere.returnrequest.dto.ReturnRequestHistoryResponse;
import com.shopsphere.returnrequest.dto.ReturnRequestResponse;
import com.shopsphere.returnrequest.dto.UpdateReturnRequestStatus;
import com.shopsphere.returnrequest.entity.CustomerReturnRequest;
import com.shopsphere.returnrequest.entity.ReturnRequestHistory;
import com.shopsphere.returnrequest.repository.CustomerReturnRequestRepository;
import com.shopsphere.returnrequest.repository.ReturnRequestHistoryRepository;
import com.shopsphere.user.ShopUserRepository;

@Service
public class CustomerReturnRequestService {
	private static final Set<ReturnRequestStatus> OPEN_STATUSES = Set.of(
			ReturnRequestStatus.REQUESTED, ReturnRequestStatus.APPROVED, ReturnRequestStatus.PICKUP_SCHEDULED,
			ReturnRequestStatus.QUALITY_CHECK, ReturnRequestStatus.REFUND_PROCESSING, ReturnRequestStatus.REPLACEMENT_APPROVED);

	private final CustomerReturnRequestRepository requests;
	private final ReturnRequestHistoryRepository history;
	private final OrderItemRepository orderItems;
	private final ShopUserRepository users;
	private final ProductImageRepository productImages;

	public CustomerReturnRequestService(CustomerReturnRequestRepository requests, ReturnRequestHistoryRepository history,
			OrderItemRepository orderItems, ShopUserRepository users, ProductImageRepository productImages) {
		this.requests = requests;
		this.history = history;
		this.orderItems = orderItems;
		this.users = users;
		this.productImages = productImages;
	}

	@Transactional(readOnly = true)
	public List<EligibleReturnItemResponse> eligibleItems(Long customerId) {
		Instant now = Instant.now();
		return orderItems.findDeliveredItemsByCustomerId(customerId).stream()
				.map(item -> eligibleItem(item, customerId, now))
				.filter(java.util.Objects::nonNull)
				.toList();
	}

	@Transactional(readOnly = true)
	public List<ReturnRequestResponse> list(Long customerId) {
		return requests.findByCustomer_IdOrderByCreatedAtDescIdDesc(customerId).stream().map(this::response).toList();
	}

	@Transactional(readOnly = true)
	public ReturnRequestResponse get(Long customerId, Long requestId) {
		return response(requests.findByIdAndCustomer_Id(requestId, customerId).orElseThrow(this::notFound));
	}

	@Transactional
	public ReturnRequestResponse create(Long customerId, CreateReturnRequest body) {
		OrderItem item = orderItems.findByIdAndSellerOrder_Order_Customer_Id(body.orderItemId(), customerId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order item was not found."));
		Instant deliveredAt = deliveredAt(item);
		if (item.getSellerOrder().getOrderStatus() != OrderStatus.DELIVERED || deliveredAt == null
				|| Instant.now().isAfter(deliveredAt.plus(7, ChronoUnit.DAYS))) {
			throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "This item is outside the 7-day return window.");
		}
		if (requests.existsByCustomer_IdAndOrderItem_IdAndStatusIn(customerId, item.getId(), OPEN_STATUSES)) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "An active request already exists for this item.");
		}
		var customer = users.findById(customerId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer account was not found."));
		CustomerReturnRequest request = requests.save(new CustomerReturnRequest(customer, item, body.requestType(), body.reason().trim(), trim(body.description())));
		history.save(new ReturnRequestHistory(request, ReturnRequestStatus.REQUESTED, "CUSTOMER", "Request submitted."));
		return response(request);
	}

	@Transactional
	public ReturnRequestResponse cancel(Long customerId, Long requestId) {
		CustomerReturnRequest request = requests.findByIdAndCustomer_Id(requestId, customerId).orElseThrow(this::notFound);
		if (request.getStatus() != ReturnRequestStatus.REQUESTED) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Only a newly submitted request can be cancelled.");
		}
		request.transition(ReturnRequestStatus.CANCELLED, "Cancelled by customer.", null);
		history.save(new ReturnRequestHistory(request, ReturnRequestStatus.CANCELLED, "CUSTOMER", "Cancelled by customer."));
		return response(request);
	}

	@Transactional(readOnly = true)
	public List<ReturnRequestResponse> adminList() {
		return requests.findAllByOrderByCreatedAtDescIdDesc().stream().map(this::response).toList();
	}

	@Transactional
	public ReturnRequestResponse updateStatus(Long requestId, UpdateReturnRequestStatus update) {
		CustomerReturnRequest request = requests.findById(requestId).orElseThrow(this::notFound);
		validateAdminTransition(request, update.status());
		String note = trim(update.note());
		request.transition(update.status(), note, update.expectedPickupAt());
		history.save(new ReturnRequestHistory(request, update.status(), "ADMIN", note));
		return response(request);
	}

	private void validateAdminTransition(CustomerReturnRequest request, ReturnRequestStatus next) {
		boolean valid = switch (request.getStatus()) {
			case REQUESTED -> next == ReturnRequestStatus.APPROVED || next == ReturnRequestStatus.REJECTED;
			case APPROVED -> next == ReturnRequestStatus.PICKUP_SCHEDULED || next == ReturnRequestStatus.QUALITY_CHECK || next == ReturnRequestStatus.REJECTED;
			case PICKUP_SCHEDULED -> next == ReturnRequestStatus.QUALITY_CHECK || next == ReturnRequestStatus.REJECTED;
			case QUALITY_CHECK -> request.getRequestType() == com.shopsphere.common.enums.ReturnRequestType.REPLACEMENT
					? next == ReturnRequestStatus.REPLACEMENT_APPROVED || next == ReturnRequestStatus.REJECTED
					: next == ReturnRequestStatus.REFUND_PROCESSING || next == ReturnRequestStatus.REJECTED;
			case REFUND_PROCESSING -> next == ReturnRequestStatus.REFUND_COMPLETED;
			case REPLACEMENT_APPROVED -> next == ReturnRequestStatus.COMPLETED;
			default -> false;
		};
		if (!valid) throw new ResponseStatusException(HttpStatus.CONFLICT, "This request cannot move to the selected status.");
	}

	private EligibleReturnItemResponse eligibleItem(OrderItem item, Long customerId, Instant now) {
		Instant delivered = deliveredAt(item);
		if (delivered == null || now.isAfter(delivered.plus(7, ChronoUnit.DAYS))
				|| requests.existsByCustomer_IdAndOrderItem_IdAndStatusIn(customerId, item.getId(), OPEN_STATUSES)) return null;
		return new EligibleReturnItemResponse(item.getId(), item.getSellerOrder().getOrder().getId(), item.getProductName(), imageUrl(item),
				item.getSku(), item.getSellerOrder().getSellerName(), item.getQuantity(), item.getLineSubtotal(), delivered,
				delivered.plus(7, ChronoUnit.DAYS));
	}

	private Instant deliveredAt(OrderItem item) {
		return item.getSellerOrder().getStatusHistory().stream()
				.filter(event -> event.getStatus() == OrderStatus.DELIVERED)
				.map(OrderStatusHistory::getChangedAt).max(Instant::compareTo)
				.orElse(item.getSellerOrder().getOrderStatus() == OrderStatus.DELIVERED ? item.getSellerOrder().getUpdatedAt() : null);
	}

	private ReturnRequestResponse response(CustomerReturnRequest request) {
		var item = request.getOrderItem();
		return new ReturnRequestResponse(request.getId(), item.getSellerOrder().getOrder().getId(), item.getId(),
				item.getProductName(), imageUrl(item), item.getSku(), item.getSellerOrder().getSellerName(), item.getQuantity(), item.getLineSubtotal(),
				request.getRequestType(), request.getReason(), request.getDescription(), request.getStatus(), request.getRefundAmount(),
				request.getCreatedAt(), request.getExpectedPickupAt(), request.getResolutionNote(),
				history.findByRequest_IdOrderByChangedAtAscIdAsc(request.getId()).stream()
						.map(event -> new ReturnRequestHistoryResponse(event.getStatus(), event.getChangedAt(), event.getNote())).toList());
	}

	private String trim(String value) { return value == null || value.isBlank() ? null : value.trim(); }
	private String imageUrl(OrderItem item) {
		return productImages.findByProduct_IdOrderByDisplayOrderAscIdAsc(item.getProduct().getId()).stream()
				.sorted(java.util.Comparator.comparing(com.shopsphere.product.entity.ProductImage::isPrimary).reversed())
				.map(com.shopsphere.product.entity.ProductImage::getUrl).findFirst().orElse(null);
	}
	private org.springframework.web.server.ResponseStatusException notFound() { return new ResponseStatusException(HttpStatus.NOT_FOUND, "Return request was not found."); }
}
