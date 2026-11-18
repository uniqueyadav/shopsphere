package com.shopsphere.admin.repository;
import java.util.Optional;
import com.shopsphere.admin.entity.MarketplaceSetting;
import org.springframework.data.jpa.repository.JpaRepository;
public interface MarketplaceSettingRepository extends JpaRepository<MarketplaceSetting,Long>{Optional<MarketplaceSetting> findByKey(String key);}
