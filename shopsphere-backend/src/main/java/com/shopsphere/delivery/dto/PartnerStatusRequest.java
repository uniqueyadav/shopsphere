package com.shopsphere.delivery.dto;

import com.shopsphere.delivery.entity.PartnerStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PartnerStatusRequest(@NotNull PartnerStatus status,@Size(max=500) String reason) {}
