package com.shopsphere.admin.entity;

import com.shopsphere.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity @Table(name="marketplace_settings",uniqueConstraints=@UniqueConstraint(name="uk_marketplace_setting_key",columnNames="setting_key"))
public class MarketplaceSetting extends BaseEntity {
	@Column(name="setting_key",nullable=false,length=100) private String key;
	@Column(name="setting_value",nullable=false,length=2000) private String value;
	@Column(name="updated_by",nullable=false) private Long updatedBy;
	protected MarketplaceSetting(){}
	public MarketplaceSetting(String key,String value,Long updatedBy){this.key=key;this.value=value;this.updatedBy=updatedBy;}
	public String getKey(){return key;} public String getValue(){return value;} public Long getUpdatedBy(){return updatedBy;} public void update(String value,Long adminId){this.value=value;this.updatedBy=adminId;}
}
