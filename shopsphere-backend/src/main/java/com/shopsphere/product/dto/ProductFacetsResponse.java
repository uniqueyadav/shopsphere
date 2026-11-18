package com.shopsphere.product.dto;

import java.util.List;

public record ProductFacetsResponse(List<ProductBrandFacet> brands, List<ProductCategoryFacet> categories) {}
