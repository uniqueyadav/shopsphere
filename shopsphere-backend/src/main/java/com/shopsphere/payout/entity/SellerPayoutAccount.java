package com.shopsphere.payout.entity;

import com.shopsphere.common.entity.BaseEntity;
import com.shopsphere.seller.entity.Seller;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "seller_payout_accounts")
public class SellerPayoutAccount extends BaseEntity {
	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "seller_id", nullable = false, unique = true, foreignKey = @ForeignKey(name = "fk_payout_account_seller"))
	private Seller seller;
	@Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private PayoutMethod method;
	@Column(name = "account_holder", nullable = false, length = 120) private String accountHolder;
	@Column(name = "encrypted_destination", nullable = false, columnDefinition = "TEXT") private String encryptedDestination;
	@Column(name = "masked_destination", nullable = false, length = 40) private String maskedDestination;
	protected SellerPayoutAccount() {}
	public SellerPayoutAccount(Seller seller, PayoutMethod method, String accountHolder, String encryptedDestination, String maskedDestination) { this.seller=seller; this.method=method; this.accountHolder=accountHolder; this.encryptedDestination=encryptedDestination; this.maskedDestination=maskedDestination; }
	public void update(PayoutMethod method, String accountHolder, String encryptedDestination, String maskedDestination) { this.method=method; this.accountHolder=accountHolder; this.encryptedDestination=encryptedDestination; this.maskedDestination=maskedDestination; }
	public Seller getSeller(){return seller;} public PayoutMethod getMethod(){return method;} public String getAccountHolder(){return accountHolder;} public String getEncryptedDestination(){return encryptedDestination;} public String getMaskedDestination(){return maskedDestination;}
}
