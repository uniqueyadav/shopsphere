SET @schema_name = DATABASE();

SET @has_delivery_fee = (
	SELECT COUNT(*)
	FROM information_schema.columns
	WHERE table_schema = @schema_name AND table_name = 'sellers' AND column_name = 'delivery_fee'
);
SET @migration_sql = IF(
	@has_delivery_fee = 0,
	'ALTER TABLE sellers ADD COLUMN delivery_fee DECIMAL(12,2) NOT NULL DEFAULT 0.00',
	'SELECT 1'
);
PREPARE migration_statement FROM @migration_sql;
EXECUTE migration_statement;
DEALLOCATE PREPARE migration_statement;

CREATE TABLE IF NOT EXISTS orders (
	id BIGINT NOT NULL AUTO_INCREMENT,
	customer_id BIGINT NOT NULL,
	idempotency_key VARCHAR(100) NOT NULL,
	shipping_address_id BIGINT NOT NULL,
	shipping_full_name VARCHAR(100) NOT NULL,
	shipping_mobile_number VARCHAR(20) NOT NULL,
	shipping_house_flat_number VARCHAR(100) NOT NULL,
	shipping_street_area VARCHAR(255) NOT NULL,
	shipping_landmark VARCHAR(150) NULL,
	shipping_city VARCHAR(100) NOT NULL,
	shipping_state VARCHAR(100) NOT NULL,
	shipping_pin_code VARCHAR(20) NOT NULL,
	shipping_address_type VARCHAR(10) NOT NULL,
	payment_method VARCHAR(20) NOT NULL,
	payment_status VARCHAR(20) NOT NULL,
	order_status VARCHAR(20) NOT NULL,
	subtotal DECIMAL(12,2) NOT NULL,
	delivery_charges DECIMAL(12,2) NOT NULL,
	total_amount DECIMAL(12,2) NOT NULL,
	created_at DATETIME(6) NOT NULL,
	updated_at DATETIME(6) NOT NULL,
	CONSTRAINT pk_orders PRIMARY KEY (id),
	CONSTRAINT uk_orders_customer_idempotency UNIQUE (customer_id, idempotency_key),
	CONSTRAINT fk_orders_customer FOREIGN KEY (customer_id) REFERENCES shop_users (id),
	INDEX idx_orders_customer_created (customer_id, created_at)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS seller_orders (
	id BIGINT NOT NULL AUTO_INCREMENT,
	order_id BIGINT NOT NULL,
	seller_id BIGINT NOT NULL,
	seller_name VARCHAR(150) NOT NULL,
	order_status VARCHAR(20) NOT NULL,
	subtotal DECIMAL(12,2) NOT NULL,
	delivery_charge DECIMAL(12,2) NOT NULL,
	total_amount DECIMAL(12,2) NOT NULL,
	created_at DATETIME(6) NOT NULL,
	updated_at DATETIME(6) NOT NULL,
	CONSTRAINT pk_seller_orders PRIMARY KEY (id),
	CONSTRAINT uk_seller_orders_order_seller UNIQUE (order_id, seller_id),
	CONSTRAINT fk_seller_orders_order FOREIGN KEY (order_id) REFERENCES orders (id),
	CONSTRAINT fk_seller_orders_seller FOREIGN KEY (seller_id) REFERENCES sellers (id),
	INDEX idx_seller_orders_seller (seller_id, created_at)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS order_items (
	id BIGINT NOT NULL AUTO_INCREMENT,
	seller_order_id BIGINT NOT NULL,
	product_id BIGINT NOT NULL,
	product_name VARCHAR(150) NOT NULL,
	sku VARCHAR(100) NOT NULL,
	quantity INT NOT NULL,
	unit_price DECIMAL(12,2) NOT NULL,
	line_subtotal DECIMAL(12,2) NOT NULL,
	created_at DATETIME(6) NOT NULL,
	updated_at DATETIME(6) NOT NULL,
	CONSTRAINT pk_order_items PRIMARY KEY (id),
	CONSTRAINT fk_order_items_seller_order FOREIGN KEY (seller_order_id) REFERENCES seller_orders (id),
	CONSTRAINT fk_order_items_product FOREIGN KEY (product_id) REFERENCES products (id),
	INDEX idx_order_items_seller_order (seller_order_id)
) ENGINE=InnoDB;
