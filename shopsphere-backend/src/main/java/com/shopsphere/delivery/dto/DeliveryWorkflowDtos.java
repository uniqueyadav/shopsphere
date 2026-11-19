package com.shopsphere.delivery.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import com.shopsphere.delivery.entity.PartnerStatus;
import com.shopsphere.delivery.entity.VehicleType;

public final class DeliveryWorkflowDtos {
    private DeliveryWorkflowDtos() {}
    public record AvailabilityRequest(boolean online) {}
    public record StatusRequest(String status,
            @jakarta.validation.constraints.DecimalMin("-90.0") @jakarta.validation.constraints.DecimalMax("90.0") Double latitude,
            @jakarta.validation.constraints.DecimalMin("-180.0") @jakarta.validation.constraints.DecimalMax("180.0") Double longitude) {
        public StatusRequest(String status) { this(status, null, null); }
    }
    public record LocationRequest(@jakarta.validation.constraints.DecimalMin("-90.0") @jakarta.validation.constraints.DecimalMax("90.0") double latitude,
            @jakarta.validation.constraints.DecimalMin("-180.0") @jakarta.validation.constraints.DecimalMax("180.0") double longitude) {}
    public record Dashboard(String fullName, String email, String phone, String city, VehicleType vehicleType,
            PartnerStatus approvalStatus, boolean online, Double latitude, Double longitude, java.time.Instant locationUpdatedAt, long assignedToday, long deliveredToday,
            long pendingDeliveries, BigDecimal earningsToday, BigDecimal earningsTotal,
            List<Assignment> assignments) {}
    public record Assignment(Long assignmentId, Long sellerOrderId, Long orderId, String customerName,
            String customerPhone, String pickupName, String pickupAddress, String dropAddress,
            String status, String paymentMethod, Instant createdAt, Instant acceptedAt, BigDecimal earning) {}
    public record AssignmentItem(Long productId, String name, String sku, int quantity, BigDecimal unitPrice,
            BigDecimal lineSubtotal, String imageUrl) {}
    public record TimelineEntry(String status, Instant changedAt, String note) {}
    public record AssignmentDetails(Long assignmentId, Long sellerOrderId, Long orderId, String status,
            Instant createdAt, Instant acceptedAt, String partnerName, boolean partnerOnline, String customerName, String customerPhone,
            String customerAddress, String deliveryInstructions, boolean leaveAtDoor, boolean callBeforeDelivery,
            Double dropLatitude, Double dropLongitude, String pickupName, String pickupAddress, String pickupPhone,
            Double pickupLatitude, Double pickupLongitude, String paymentMethod, String paymentStatus,
            BigDecimal subtotal, BigDecimal deliveryFee, BigDecimal totalAmount, List<AssignmentItem> items,
            List<TimelineEntry> timeline) {}
}
