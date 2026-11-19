package com.shopsphere.seller.entity;

import com.shopsphere.common.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "seller_documents")
public class SellerDocument extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "seller_id", nullable = false, foreignKey = @ForeignKey(name = "fk_seller_documents_seller"))
	private Seller seller;

	@Column(name = "document_type", nullable = false, length = 50)
	private String documentType;

	@Column(name = "original_file_name", nullable = false, length = 255)
	private String originalFileName;

	@Column(name = "file_url", nullable = false, length = 1000)
	private String fileUrl;

	@Column(name = "public_id", nullable = false, length = 255)
	private String publicId;

	protected SellerDocument() {
	}

	public SellerDocument(String documentType, String originalFileName, String fileUrl, String publicId) {
		this.documentType = documentType;
		this.originalFileName = originalFileName;
		this.fileUrl = fileUrl;
		this.publicId = publicId;
	}

	public String getDocumentType() {
		return documentType;
	}

	public String getOriginalFileName() {
		return originalFileName;
	}

	public String getFileUrl() {
		return fileUrl;
	}

	public String getPublicId() {
		return publicId;
	}

	public void setSeller(Seller seller) {
		this.seller = seller;
	}
}
