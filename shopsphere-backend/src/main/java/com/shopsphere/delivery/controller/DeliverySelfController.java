package com.shopsphere.delivery.controller;

import com.shopsphere.common.response.ApiResponse;
import com.shopsphere.delivery.dto.DeliveryPartnerProfileResponse;
import com.shopsphere.delivery.repository.DeliveryPartnerRepository;
import com.shopsphere.security.CustomUserDetails;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/api/v1/delivery/me")
public class DeliverySelfController {
    private final DeliveryPartnerRepository partners;
    public DeliverySelfController(DeliveryPartnerRepository partners) { this.partners = partners; }

    @GetMapping
    public ApiResponse<DeliveryPartnerProfileResponse> me(@AuthenticationPrincipal CustomUserDetails principal) {
        var partner = partners.findByUser_Id(principal.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Delivery partner profile not found."));
        var user = partner.getUser();
        return ApiResponse.success("Delivery partner profile loaded.", new DeliveryPartnerProfileResponse(
                partner.getId(), user.getFullName(), user.getEmail(), user.getPhone(), partner.getStatus(),
                partner.getVehicleType(), partner.getCity(), partner.getCreatedAt()));
    }
}
