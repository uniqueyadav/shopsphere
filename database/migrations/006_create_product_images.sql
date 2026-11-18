CREATE TABLE IF NOT EXISTS product_images (
	id BIGINT NOT NULL AUTO_INCREMENT,
	product_id BIGINT NOT NULL,
	url VARCHAR(2048) NOT NULL,
	public_id VARCHAR(255) NOT NULL,
	is_primary BOOLEAN NOT NULL DEFAULT FALSE,
	display_order INT NOT NULL,
	created_at DATETIME(6) NOT NULL,
	updated_at DATETIME(6) NOT NULL,
	CONSTRAINT pk_product_images PRIMARY KEY (id),
	CONSTRAINT uk_product_images_public_id UNIQUE (public_id),
	CONSTRAINT fk_product_images_product FOREIGN KEY (product_id) REFERENCES products (id),
	INDEX idx_product_images_order (product_id, display_order)
) ENGINE=InnoDB;
