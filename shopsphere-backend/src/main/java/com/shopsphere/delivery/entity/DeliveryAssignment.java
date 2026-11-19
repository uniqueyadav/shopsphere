package com.shopsphere.delivery.entity;

import com.shopsphere.common.entity.BaseEntity;
import com.shopsphere.order.entity.SellerOrder;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "delivery_assignments", uniqueConstraints = @UniqueConstraint(name = "uk_delivery_assignment_shipment", columnNames = "seller_order_id"), indexes = @Index(name = "idx_delivery_assignment_partner", columnList = "partner_id,created_at"))
public class DeliveryAssignment extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "partner_id", nullable = false, foreignKey = @ForeignKey(name = "fk_delivery_assignment_partner"))
    private DeliveryPartner partner;
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seller_order_id", nullable = false, unique = true, foreignKey = @ForeignKey(name = "fk_delivery_assignment_shipment"))
    private SellerOrder sellerOrder;
    @Column(name = "accepted_at") private Instant acceptedAt;
    protected DeliveryAssignment() {}
    public DeliveryAssignment(DeliveryPartner partner, SellerOrder sellerOrder) { this.partner = partner; this.sellerOrder = sellerOrder; }
    public DeliveryPartner getPartner() { return partner; }
    public SellerOrder getSellerOrder() { return sellerOrder; }
    public Instant getAcceptedAt() { return acceptedAt; }
    public void accept() { if (acceptedAt != null) throw new IllegalStateException("Assignment has already been accepted."); acceptedAt = Instant.now(); }
}
