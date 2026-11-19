CREATE TABLE seller_documents (
	id BIGINT NOT NULL AUTO_INCREMENT,
	created_at TIMESTAMP NOT NULL,
	updated_at TIMESTAMP NOT NULL,
	seller_id BIGINT NOT NULL,
	document_type VARCHAR(50) NOT NULL,
	original_file_name VARCHAR(255) NOT NULL,
	file_url VARCHAR(1000) NOT NULL,
	public_id VARCHAR(255) NOT NULL,
	PRIMARY KEY (id),
	CONSTRAINT fk_seller_documents_seller FOREIGN KEY (seller_id) REFERENCES sellers (id)
);

