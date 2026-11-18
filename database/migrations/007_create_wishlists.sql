CREATE TABLE IF NOT EXISTS wishlists (
	id BIGINT NOT NULL AUTO_INCREMENT,
	customer_id BIGINT NOT NULL,
	created_at DATETIME(6) NOT NULL,
	updated_at DATETIME(6) NOT NULL,
	CONSTRAINT pk_wishlists PRIMARY KEY (id),
	CONSTRAINT uk_wishlists_customer UNIQUE (customer_id),
	CONSTRAINT fk_wishlists_customer FOREIGN KEY (customer_id) REFERENCES shop_users (id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS wishlist_items (
	id BIGINT NOT NULL AUTO_INCREMENT,
	wishlist_id BIGINT NOT NULL,
	product_id BIGINT NOT NULL,
	created_at DATETIME(6) NOT NULL,
	updated_at DATETIME(6) NOT NULL,
	CONSTRAINT pk_wishlist_items PRIMARY KEY (id),
	CONSTRAINT uk_wishlist_items_wishlist_product UNIQUE (wishlist_id, product_id),
	CONSTRAINT fk_wishlist_items_wishlist FOREIGN KEY (wishlist_id) REFERENCES wishlists (id),
	CONSTRAINT fk_wishlist_items_product FOREIGN KEY (product_id) REFERENCES products (id),
	INDEX idx_wishlist_items_product (product_id)
) ENGINE=InnoDB;
