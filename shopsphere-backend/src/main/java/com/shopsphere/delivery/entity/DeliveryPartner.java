package com.shopsphere.delivery.entity;

import com.shopsphere.common.entity.BaseEntity;
import com.shopsphere.common.enums.Role;
import com.shopsphere.user.entity.User;
import java.time.Instant;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

@Entity
@Table(name="delivery_partners", uniqueConstraints=@UniqueConstraint(name="uk_delivery_partner_user",columnNames="user_id"), indexes=@Index(name="idx_delivery_partner_status_city",columnList="status,city"))
public class DeliveryPartner extends BaseEntity {
	@OneToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="user_id",nullable=false,unique=true,foreignKey=@ForeignKey(name="fk_delivery_partner_user")) private User user;
	@Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private PartnerStatus status=PartnerStatus.PENDING;
	@Enumerated(EnumType.STRING) @Column(name="vehicle_type",nullable=false,length=20) private VehicleType vehicleType;
	@Column(length=100) private String city;
	@Column(length=500) private String address;
	@Column(name="vehicle_registration_number",length=30) private String vehicleRegistrationNumber;
	@Column(name="rejection_reason",length=500) private String rejectionReason;
	@Column(name="suspension_reason",length=500) private String suspensionReason;
	@Column(name="is_online",nullable=false) private boolean online;
	@Column(name="last_latitude") private Double lastLatitude;
	@Column(name="last_longitude") private Double lastLongitude;
	@Column(name="location_updated_at") private Instant locationUpdatedAt;
	protected DeliveryPartner(){}
	public DeliveryPartner(User user,VehicleType vehicleType,String city,String address,String vehicleRegistrationNumber){if(user.getRole()!=Role.DELIVERY_PARTNER)throw new IllegalArgumentException("User must have DELIVERY_PARTNER role.");this.user=user;this.vehicleType=vehicleType;this.city=city;this.address=address;this.vehicleRegistrationNumber=vehicleRegistrationNumber;}
	public User getUser(){return user;} public PartnerStatus getStatus(){return status;} public VehicleType getVehicleType(){return vehicleType;} public String getCity(){return city;} public String getAddress(){return address;} public String getVehicleRegistrationNumber(){return vehicleRegistrationNumber;} public String getRejectionReason(){return rejectionReason;} public String getSuspensionReason(){return suspensionReason;}
	public boolean isOnline(){return online;} public void setOnline(boolean online){if(online&&status!=PartnerStatus.APPROVED)throw new IllegalStateException("Partner must be approved before going online.");this.online=online;}
	public Double getLastLatitude(){return lastLatitude;} public Double getLastLongitude(){return lastLongitude;} public Instant getLocationUpdatedAt(){return locationUpdatedAt;}
	public void updateServiceCity(String city){this.city=city.trim();}
	public void updateLocation(double latitude,double longitude){this.lastLatitude=latitude;this.lastLongitude=longitude;this.locationUpdatedAt=Instant.now();}
	public void approve(){status=PartnerStatus.APPROVED;rejectionReason=null;suspensionReason=null;}
	public void reject(String reason){status=PartnerStatus.REJECTED;online=false;rejectionReason=reason;}
	public void suspend(String reason){status=PartnerStatus.SUSPENDED;online=false;suspensionReason=reason;}
}
