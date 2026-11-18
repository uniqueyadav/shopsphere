CREATE TABLE customer_return_requests (
	id BIGINT NOT NULL AUTO_INCREMENT,
	customer_id BIGINT NOT NULL,
	order_item_id BIGINT NOT NULL,
	request_type VARCHAR(20) NOT NULL,
	reason VARCHAR(60) NOT NULL,
	description VARCHAR(1000) NULL,
	status VARCHAR(30) NOT NULL,
	refund_amount DECIMAL(12,2) NOT NULL,
	expected_pickup_at DATETIME(6) NULL,
	resolution_note VARCHAR(500) NULL,
	created_at DATETIME(6) NOT NULL,
	updated_at DATETIME(6) NOT NULL,
	CONSTRAINT pk_customer_return_requests PRIMARY KEY (id),
	CONSTRAINT fk_return_requests_customer FOREIGN KEY (customer_id) REFERENCES users (id),
	CONSTRAINT fk_return_requests_order_item FOREIGN KEY (order_item_id) REFERENCES order_items (id),
	INDEX idx_return_requests_customer_created (customer_id, created_at),
	INDEX idx_return_requests_status (status, updated_at)
) ENGINE=InnoDB;

CREATE TABLE return_request_history (
	id BIGINT NOT NULL AUTO_INCREMENT,
	return_request_id BIGINT NOT NULL,
	status VARCHAR(30) NOT NULL,
	changed_at DATETIME(6) NOT NULL,
	actor VARCHAR(20) NOT NULL,
	note VARCHAR(500) NULL,
	created_at DATETIME(6) NOT NULL,
	updated_at DATETIME(6) NOT NULL,
	CONSTRAINT pk_return_request_history PRIMARY KEY (id),
	CONSTRAINT fk_return_request_history_request FOREIGN KEY (return_request_id) REFERENCES customer_return_requests (id),
	INDEX idx_return_request_history_time (return_request_id, changed_at)
) ENGINE=InnoDB;
