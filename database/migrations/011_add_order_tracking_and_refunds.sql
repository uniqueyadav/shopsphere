CREATE TABLE IF NOT EXISTS order_status_history (
	id BIGINT NOT NULL AUTO_INCREMENT,
	seller_order_id BIGINT NOT NULL,
	status VARCHAR(20) NOT NULL,
	changed_at DATETIME(6) NOT NULL,
	changed_by VARCHAR(20) NOT NULL,
	note VARCHAR(255) NULL,
	created_at DATETIME(6) NOT NULL,
	updated_at DATETIME(6) NOT NULL,
	CONSTRAINT pk_order_status_history PRIMARY KEY (id),
	CONSTRAINT fk_order_status_history_seller_order
		FOREIGN KEY (seller_order_id) REFERENCES seller_orders (id),
	INDEX idx_order_status_history_seller_order (seller_order_id, changed_at)
) ENGINE=InnoDB;

INSERT INTO order_status_history (
	seller_order_id,
	status,
	changed_at,
	changed_by,
	note,
	created_at,
	updated_at
)
SELECT
	seller_order.id,
	seller_order.order_status,
	seller_order.updated_at,
	'SYSTEM',
	'Status history initialized from the current seller order status.',
	seller_order.created_at,
	seller_order.updated_at
FROM seller_orders seller_order
WHERE NOT EXISTS (
	SELECT 1
	FROM order_status_history history
	WHERE history.seller_order_id = seller_order.id
);

ALTER TABLE payment_transactions
	ADD COLUMN razorpay_refund_id VARCHAR(100) NULL,
	ADD CONSTRAINT uk_payment_razorpay_refund UNIQUE (razorpay_refund_id);

ALTER TABLE orders
	ADD COLUMN refund_status VARCHAR(20) NOT NULL DEFAULT 'NOT_REQUESTED';
