package com.shopsphere.delivery.dto;

import java.time.Instant;
import com.shopsphere.delivery.entity.PartnerStatus;
import com.shopsphere.delivery.entity.VehicleType;

public record DeliveryPartnerProfileResponse(Long id, String fullName, String email, String phone,
        PartnerStatus status, VehicleType vehicleType, String city, Instant createdAt) {}
