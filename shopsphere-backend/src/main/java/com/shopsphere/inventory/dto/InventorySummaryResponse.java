package com.shopsphere.inventory.dto;

public record InventorySummaryResponse(long totalProducts, long inStock, long lowStock, long outOfStock) {}
