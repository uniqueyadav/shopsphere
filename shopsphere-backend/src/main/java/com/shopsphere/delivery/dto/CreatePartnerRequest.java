package com.shopsphere.delivery.dto;

import com.shopsphere.delivery.entity.VehicleType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreatePartnerRequest(@NotNull Long userId,@NotNull VehicleType vehicleType,@Size(max=100) String city,@Size(max=500) String address,@Size(max=30) String vehicleRegistrationNumber) {}
