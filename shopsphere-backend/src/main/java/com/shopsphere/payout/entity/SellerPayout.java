package com.shopsphere.payout.entity;

import java.math.BigDecimal;
import com.shopsphere.common.entity.BaseEntity;
import com.shopsphere.seller.entity.Seller;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name="seller_payouts", uniqueConstraints=@UniqueConstraint(name="uk_payout_seller_idempotency",columnNames={"seller_id","idempotency_key"}))
public class SellerPayout extends BaseEntity {
	@ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="seller_id",nullable=false,foreignKey=@ForeignKey(name="fk_payout_seller")) private Seller seller;
	@ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="payout_account_id",nullable=false,foreignKey=@ForeignKey(name="fk_payout_account")) private SellerPayoutAccount account;
	@Enumerated(EnumType.STRING) @Column(name="payment_method",nullable=false,length=20) private PayoutMethod method;
	@Column(name="account_holder",nullable=false,length=120) private String accountHolder;
	@Column(name="encrypted_destination",nullable=false,columnDefinition="TEXT") private String encryptedDestination;
	@Column(name="masked_destination",nullable=false,length=40) private String maskedDestination;
	@Column(nullable=false,precision=12,scale=2) private BigDecimal amount;
	@Column(name="idempotency_key",nullable=false,length=100) private String idempotencyKey;
	@Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private PayoutStatus status=PayoutStatus.REQUESTED;
	@Column(name="transfer_reference",length=120) private String transferReference;
	@Column(length=500) private String remarks;
	protected SellerPayout(){}
	public SellerPayout(Seller seller,SellerPayoutAccount account,BigDecimal amount,String idempotencyKey,PayoutMethod method,String accountHolder,String encryptedDestination,String maskedDestination){this.seller=seller;this.account=account;this.amount=amount;this.idempotencyKey=idempotencyKey;this.method=method;this.accountHolder=accountHolder;this.encryptedDestination=encryptedDestination;this.maskedDestination=maskedDestination;}
	public void process(){if(status!=PayoutStatus.REQUESTED)throw new IllegalStateException("Only requested payouts can be processed.");status=PayoutStatus.PROCESSING;}
	public void complete(String transferReference,String remarks){if(status!=PayoutStatus.PROCESSING)throw new IllegalStateException("Only processing payouts can be completed.");this.status=PayoutStatus.PAID;this.transferReference=transferReference;this.remarks=remarks;}
	public void reject(String remarks){if(status!=PayoutStatus.REQUESTED&&status!=PayoutStatus.PROCESSING)throw new IllegalStateException("Only open payouts can be rejected.");this.status=PayoutStatus.REJECTED;this.remarks=remarks;}
	public Seller getSeller(){return seller;} public SellerPayoutAccount getAccount(){return account;} public BigDecimal getAmount(){return amount;} public String getIdempotencyKey(){return idempotencyKey;} public PayoutStatus getStatus(){return status;} public String getTransferReference(){return transferReference;} public String getRemarks(){return remarks;} public PayoutMethod getMethod(){return method;} public String getAccountHolder(){return accountHolder;} public String getEncryptedDestination(){return encryptedDestination;} public String getMaskedDestination(){return maskedDestination;}
}
