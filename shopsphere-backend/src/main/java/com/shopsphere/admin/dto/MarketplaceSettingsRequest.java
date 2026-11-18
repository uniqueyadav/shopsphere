package com.shopsphere.admin.dto;
import java.util.Map;
import jakarta.validation.constraints.NotNull;
public record MarketplaceSettingsRequest(@NotNull Map<String,String> settings){}
