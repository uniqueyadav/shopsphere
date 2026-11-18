package com.shopsphere.delivery.entity;

import com.shopsphere.common.entity.BaseEntity;
import com.shopsphere.common.enums.Role;
import com.shopsphere.user.entity.User;
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
	protected DeliveryPartner(){}
	public DeliveryPartner(User user,VehicleType vehicleType,String city,String address,String vehicleRegistrationNumber){if(user.getRole()!=Role.DELIVERY_PARTNER)throw new IllegalArgumentException("User must have DELIVERY_PARTNER role.");this.user=user;this.vehicleType=vehicleType;this.city=city;this.address=address;this.vehicleRegistrationNumber=vehicleRegistrationNumber;}
	public User getUser(){return user;} public PartnerStatus getStatus(){return status;} public VehicleType getVehicleType(){return vehicleType;} public String getCity(){return city;} public String getAddress(){return address;} public String getVehicleRegistrationNumber(){return vehicleRegistrationNumber;} public String getRejectionReason(){return rejectionReason;} public String getSuspensionReason(){return suspensionReason;}
	public void approve(){status=PartnerStatus.APPROVED;rejectionReason=null;suspensionReason=null;}
	public void reject(String reason){status=PartnerStatus.REJECTED;rejectionReason=reason;}
	public void suspend(String reason){status=PartnerStatus.SUSPENDED;suspensionReason=reason;}
}
