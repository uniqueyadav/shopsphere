package com.shopsphere.admin.service;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import com.shopsphere.admin.entity.MarketplaceSetting;
import com.shopsphere.admin.repository.MarketplaceSettingRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service public class MarketplaceSettingsService {
	private static final Set<String> ALLOWED=Set.of("siteName","siteDescription","contactEmail","contactPhone","supportEmail","businessAddress","websiteUrl","currency","timezone","dateFormat","timeFormat","country","language","registrationEnabled","sellerRegistrationEnabled","guestCheckoutEnabled","maintenanceMode","displayReviews","wishlistEnabled","productComparisonEnabled","primaryColor","secondaryColor","accentColor","logoPosition","themeMode","borderRadius");
	private final MarketplaceSettingRepository repository;
	public MarketplaceSettingsService(MarketplaceSettingRepository repository){this.repository=repository;}
	@Transactional(readOnly=true) public Map<String,String> get(){return repository.findAll().stream().collect(Collectors.toMap(MarketplaceSetting::getKey,MarketplaceSetting::getValue));}
	@Transactional public Map<String,String> update(Map<String,String> settings,Long adminId){for(var entry:settings.entrySet()){String key=entry.getKey(),value=entry.getValue();if(!ALLOWED.contains(key))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Unsupported setting: "+key);if(value==null||value.length()>2000)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid value for "+key);if(Set.of("registrationEnabled","sellerRegistrationEnabled","guestCheckoutEnabled","maintenanceMode","displayReviews","wishlistEnabled","productComparisonEnabled").contains(key)&&!Set.of("true","false").contains(value))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Expected boolean value for "+key);if(Set.of("primaryColor","secondaryColor","accentColor").contains(key)&&!value.matches("#[0-9a-fA-F]{6}"))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid color value for "+key);repository.findByKey(key).ifPresentOrElse(s->s.update(value,adminId),()->repository.save(new MarketplaceSetting(key,value,adminId)));}return get();}
}
