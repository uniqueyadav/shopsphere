package com.shopsphere.commission.entity;

import java.math.BigDecimal;
import java.time.Instant;
import com.shopsphere.category.entity.Category;
import com.shopsphere.common.entity.BaseEntity;
import com.shopsphere.user.entity.User;
import jakarta.persistence.*;

@Entity
@Table(name="commission_rules", indexes={@Index(name="idx_commission_scope_effective",columnList="scope,category_id,effective_from,effective_to")})
public class CommissionRule extends BaseEntity {
	public enum Scope { PLATFORM, CATEGORY }
	@Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private Scope scope;
	@ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="category_id",foreignKey=@ForeignKey(name="fk_commission_category")) private Category category;
	@Column(name="rate_percent",nullable=false,precision=5,scale=2) private BigDecimal ratePercent;
	@Column(name="effective_from",nullable=false) private Instant effectiveFrom;
	@Column(name="effective_to") private Instant effectiveTo;
	@Column(nullable=false) private boolean active=true;
	@Column(length=500) private String reason;
	@ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="updated_by",nullable=false,foreignKey=@ForeignKey(name="fk_commission_admin")) private User updatedBy;
	protected CommissionRule(){}
	public CommissionRule(Scope scope,Category category,BigDecimal ratePercent,Instant from,Instant to,String reason,User admin){this.scope=scope;this.category=category;this.ratePercent=ratePercent;this.effectiveFrom=from;this.effectiveTo=to;this.reason=reason;this.updatedBy=admin;}
	public Scope getScope(){return scope;} public Category getCategory(){return category;} public BigDecimal getRatePercent(){return ratePercent;} public Instant getEffectiveFrom(){return effectiveFrom;} public Instant getEffectiveTo(){return effectiveTo;} public boolean isActive(){return active;} public String getReason(){return reason;} public User getUpdatedBy(){return updatedBy;}
}
