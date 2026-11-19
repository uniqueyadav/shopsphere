package com.shopsphere.delivery.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PartnerServiceCityRequest(@NotBlank @Size(max = 100) String city) {}
