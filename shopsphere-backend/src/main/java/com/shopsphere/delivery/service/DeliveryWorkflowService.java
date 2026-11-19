package com.shopsphere.delivery.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import com.shopsphere.common.enums.OrderStatus;
import com.shopsphere.delivery.dto.DeliveryWorkflowDtos.Assignment;
import com.shopsphere.delivery.dto.DeliveryWorkflowDtos.AssignmentDetails;
import com.shopsphere.delivery.dto.DeliveryWorkflowDtos.AssignmentItem;
import com.shopsphere.delivery.dto.DeliveryWorkflowDtos.Dashboard;
import com.shopsphere.delivery.dto.DeliveryWorkflowDtos.TimelineEntry;
import com.shopsphere.delivery.entity.DeliveryAssignment;
import com.shopsphere.delivery.entity.DeliveryPartner;
import com.shopsphere.delivery.entity.PartnerStatus;
import com.shopsphere.delivery.repository.DeliveryAssignmentRepository;
import com.shopsphere.delivery.repository.DeliveryPartnerRepository;
import com.shopsphere.order.entity.SellerOrder;
import com.shopsphere.product.repository.ProductImageRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class DeliveryWorkflowService {
    private static final ZoneId ZONE = ZoneId.of("Asia/Kolkata");
    private final DeliveryPartnerRepository partners;
    private final DeliveryAssignmentRepository assignments;
    private final EntityManager entityManager;
    private final ProductImageRepository productImages;
    public DeliveryWorkflowService(DeliveryPartnerRepository partners, DeliveryAssignmentRepository assignments, EntityManager entityManager, ProductImageRepository productImages) {
        this.partners = partners; this.assignments = assignments; this.entityManager = entityManager; this.productImages = productImages;
    }

    @Transactional(readOnly = true)
    public AssignmentDetails assignmentDetails(Long userId, Long assignmentId) {
        DeliveryAssignment assignment = assignments.findByIdAndPartner_User_Id(assignmentId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Delivery assignment not found."));
        SellerOrder shipment = assignment.getSellerOrder();
        var order = shipment.getOrder();
        var seller = shipment.getSeller();
        List<Long> productIds = shipment.getItems().stream().map(item -> item.getProduct().getId()).distinct().toList();
        java.util.Map<Long, String> imageByProduct = new java.util.HashMap<>();
        productImages.findByProduct_IdInOrderByDisplayOrderAscIdAsc(productIds)
                .forEach(image -> imageByProduct.putIfAbsent(image.getProduct().getId(), image.getUrl()));
        List<AssignmentItem> items = shipment.getItems().stream().map(item -> new AssignmentItem(
                item.getProduct().getId(), item.getProductName(), item.getSku(), item.getQuantity(),
                item.getUnitPrice(), item.getLineSubtotal(), imageByProduct.get(item.getProduct().getId()))).toList();
        List<TimelineEntry> timeline = shipment.getStatusHistory().stream()
                .map(event -> new TimelineEntry(event.getStatus().name(), event.getChangedAt(), event.getNote()))
                .collect(java.util.stream.Collectors.toCollection(java.util.ArrayList::new));
        if (assignment.getAcceptedAt() != null) timeline.add(new TimelineEntry("ACCEPTED", assignment.getAcceptedAt(), "Delivery partner accepted this assignment."));
        timeline.sort(java.util.Comparator.comparing(TimelineEntry::changedAt));
        String address = String.join(", ", java.util.Arrays.asList(order.getShippingHouseFlatNumber(), order.getShippingStreetArea(), order.getShippingLandmark(), order.getShippingCity(), order.getShippingState(), order.getShippingPinCode()).stream().filter(part -> part != null && !part.isBlank()).toList());
        return new AssignmentDetails(assignment.getId(), shipment.getId(), order.getId(), shipment.getOrderStatus().name(),
                assignment.getCreatedAt(), assignment.getAcceptedAt(), assignment.getPartner().getUser().getFullName(), assignment.getPartner().isOnline(), order.getShippingFullName(), order.getShippingMobileNumber(),
                address, order.getShippingDeliveryInstructions(), order.isShippingLeaveAtDoor(), order.isShippingCallBeforeDelivery(),
                order.getShippingLatitude(), order.getShippingLongitude(), seller.getStoreName(), seller.getBusinessAddress(),
                seller.getSellerPhone() == null ? seller.getUser().getPhone() : seller.getSellerPhone(),
                seller.getPickupLatitude(), seller.getPickupLongitude(), order.getPaymentMethod().name(), order.getPaymentStatus().name(),
                shipment.getSubtotal(), shipment.getDeliveryCharge(), shipment.getTotalAmount(), items, timeline);
    }

    @Transactional
    public Dashboard dashboard(Long userId) {
        DeliveryPartner partner = partner(userId);
        if (partner.isOnline()) dispatch(partner);
        List<DeliveryAssignment> records = assignments.findByPartner_User_IdOrderByCreatedAtDesc(userId, PageRequest.of(0, 100));
        var start = LocalDate.now(ZONE).atStartOfDay(ZONE).toInstant();
        long todayAssigned = entityManager.createQuery("select count(a) from DeliveryAssignment a where a.partner.user.id = :userId and a.createdAt >= :start", Long.class).setParameter("userId", userId).setParameter("start", start).getSingleResult();
        long todayDelivered = entityManager.createQuery("select count(a) from DeliveryAssignment a where a.partner.user.id = :userId and a.sellerOrder.orderStatus = :status and a.sellerOrder.updatedAt >= :start", Long.class).setParameter("userId", userId).setParameter("status", OrderStatus.DELIVERED).setParameter("start", start).getSingleResult();
        long pending = entityManager.createQuery("select count(a) from DeliveryAssignment a where a.partner.user.id = :userId and a.sellerOrder.orderStatus in :statuses", Long.class).setParameter("userId", userId).setParameter("statuses", List.of(OrderStatus.PLACED, OrderStatus.CONFIRMED, OrderStatus.PROCESSING, OrderStatus.SHIPPED, OrderStatus.OUT_FOR_DELIVERY)).getSingleResult();
        BigDecimal todayEarnings = entityManager.createQuery("select coalesce(sum(a.sellerOrder.deliveryCharge), 0) from DeliveryAssignment a where a.partner.user.id = :userId and a.sellerOrder.orderStatus = :status and a.sellerOrder.updatedAt >= :start", BigDecimal.class).setParameter("userId", userId).setParameter("status", OrderStatus.DELIVERED).setParameter("start", start).getSingleResult();
        BigDecimal totalEarnings = entityManager.createQuery("select coalesce(sum(a.sellerOrder.deliveryCharge), 0) from DeliveryAssignment a where a.partner.user.id = :userId and a.sellerOrder.orderStatus = :status", BigDecimal.class).setParameter("userId", userId).setParameter("status", OrderStatus.DELIVERED).getSingleResult();
        return new Dashboard(partner.getUser().getFullName(), partner.getUser().getEmail(), partner.getUser().getPhone(), partner.getCity(), partner.getVehicleType(), partner.getStatus(), partner.isOnline(), partner.getLastLatitude(), partner.getLastLongitude(), partner.getLocationUpdatedAt(), todayAssigned, todayDelivered, pending, todayEarnings, totalEarnings, records.stream().map(this::toAssignment).toList());
    }

    @Transactional
    public boolean setAvailability(Long userId, boolean online) {
        DeliveryPartner partner = partner(userId);
        try { partner.setOnline(online); } catch (IllegalStateException e) { throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Your partner profile must be approved before going online."); }
        if (online) dispatch(partner);
        return partner.isOnline();
    }

    @Transactional
    public void updateLocation(Long userId, double latitude, double longitude) {
        DeliveryPartner partner = partner(userId);
        if (!partner.isOnline()) throw new ResponseStatusException(HttpStatus.CONFLICT, "Go online before sharing your location.");
        partner.updateLocation(latitude, longitude);
    }

    @Transactional
    public Assignment acceptAssignment(Long userId, Long assignmentId) {
        DeliveryAssignment assignment = assignments.findByIdAndPartner_User_Id(assignmentId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Delivery assignment not found."));
        if (assignment.getPartner().getStatus() != PartnerStatus.APPROVED || !assignment.getPartner().isOnline()) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Go online with an approved partner account before accepting deliveries.");
        if (!List.of(OrderStatus.PLACED, OrderStatus.CONFIRMED, OrderStatus.PROCESSING, OrderStatus.SHIPPED).contains(assignment.getSellerOrder().getOrderStatus())) throw new ResponseStatusException(HttpStatus.CONFLICT, "This shipment is no longer available.");
        try { assignment.accept(); } catch (IllegalStateException e) { throw new ResponseStatusException(HttpStatus.CONFLICT, "This assignment has already been accepted."); }
        return toAssignment(assignment);
    }

    @Transactional
    public Assignment updateStatus(Long userId, Long assignmentId, String rawStatus) {
        return updateStatus(userId, assignmentId, rawStatus, null, null);
    }

    @Transactional
    public Assignment updateStatus(Long userId, Long assignmentId, String rawStatus, Double latitude, Double longitude) {
        DeliveryAssignment assignment = assignments.findByIdAndPartner_User_Id(assignmentId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Delivery assignment not found."));
        if (assignment.getPartner().getStatus() != PartnerStatus.APPROVED) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only an approved delivery partner can update deliveries.");
        if (assignment.getAcceptedAt() == null) throw new ResponseStatusException(HttpStatus.CONFLICT, "Accept this assignment before starting the delivery.");
        SellerOrder shipment = assignment.getSellerOrder();
        OrderStatus next;
        try { next = OrderStatus.valueOf(rawStatus.trim().toUpperCase(Locale.ROOT)); }
        catch (Exception e) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid delivery status."); }
        if (next != OrderStatus.OUT_FOR_DELIVERY && next != OrderStatus.DELIVERED) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only on-route and delivered statuses can be set by a delivery partner.");
        if ((latitude == null) != (longitude == null)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Both status location coordinates are required.");
        if (latitude != null) assignment.getPartner().updateLocation(latitude, longitude);
        Double eventLatitude = latitude == null ? assignment.getPartner().getLastLatitude() : latitude;
        Double eventLongitude = longitude == null ? assignment.getPartner().getLastLongitude() : longitude;
        try { shipment.transitionTo(next, "DELIVERY_PARTNER", "Delivery partner updated the shipment.", eventLatitude, eventLongitude); }
        catch (IllegalStateException e) { throw new ResponseStatusException(HttpStatus.CONFLICT, "This delivery status transition is not allowed."); }
        shipment.getOrder().refreshOrderStatus();
        return toAssignment(assignment);
    }

    private void dispatch(DeliveryPartner partner) {
        if (partner.getStatus() != PartnerStatus.APPROVED) return;
        if (partner.getCity() == null || partner.getCity().isBlank()) return;
        @SuppressWarnings("unchecked")
        List<SellerOrder> available = entityManager.createQuery("select so from SellerOrder so join fetch so.order o where so.orderStatus in :statuses and (lower(trim(o.shippingCity)) = :city or lower(trim(o.shippingCity)) like :cityPattern or :cityLikeShipping like concat('%', lower(trim(o.shippingCity)), '%')) and not exists (select a.id from DeliveryAssignment a where a.sellerOrder.id = so.id) order by so.createdAt asc")
                .setParameter("statuses", List.of(OrderStatus.PLACED, OrderStatus.CONFIRMED, OrderStatus.PROCESSING, OrderStatus.SHIPPED))
                .setParameter("city", partner.getCity().trim().toLowerCase(Locale.ROOT))
                .setParameter("cityPattern", "%" + partner.getCity().trim().toLowerCase(Locale.ROOT) + "%")
                .setParameter("cityLikeShipping", partner.getCity().trim().toLowerCase(Locale.ROOT))
                .setMaxResults(1000).setLockMode(LockModeType.PESSIMISTIC_WRITE).getResultList();
        for (SellerOrder shipment : available) assignments.save(new DeliveryAssignment(partner, shipment));
    }

    private DeliveryPartner partner(Long userId) { return partners.findByUser_Id(userId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Delivery partner profile not found.")); }
    private Assignment toAssignment(DeliveryAssignment a) {
        var shipment = a.getSellerOrder(); var order = shipment.getOrder(); var seller = shipment.getSeller();
        String drop = String.join(", ", List.of(order.getShippingHouseFlatNumber(), order.getShippingStreetArea(), order.getShippingCity(), order.getShippingState(), order.getShippingPinCode()).stream().filter(s -> s != null && !s.isBlank()).toList());
        return new Assignment(a.getId(), shipment.getId(), order.getId(), order.getShippingFullName(), order.getShippingMobileNumber(), seller.getStoreName(), seller.getBusinessAddress(), drop, shipment.getOrderStatus().name(), order.getPaymentMethod().name(), a.getCreatedAt(), a.getAcceptedAt(), shipment.getDeliveryCharge());
    }
}
