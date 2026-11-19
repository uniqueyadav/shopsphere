CREATE TABLE IF NOT EXISTS delivery_partners (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    vehicle_type VARCHAR(20) NOT NULL,
    city VARCHAR(100) NULL,
    address VARCHAR(500) NULL,
    vehicle_registration_number VARCHAR(30) NULL,
    rejection_reason VARCHAR(500) NULL,
    suspension_reason VARCHAR(500) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_delivery_partners PRIMARY KEY (id),
    CONSTRAINT uk_delivery_partner_user UNIQUE (user_id),
    CONSTRAINT fk_delivery_partner_user FOREIGN KEY (user_id) REFERENCES shop_users (id),
    INDEX idx_delivery_partner_status_city (status, city)
) ENGINE=InnoDB;

ALTER TABLE delivery_partners
    ADD COLUMN is_online BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN last_latitude DOUBLE NULL,
    ADD COLUMN last_longitude DOUBLE NULL,
    ADD COLUMN location_updated_at DATETIME(6) NULL;

CREATE TABLE IF NOT EXISTS delivery_assignments (
    id BIGINT NOT NULL AUTO_INCREMENT,
    partner_id BIGINT NOT NULL,
    seller_order_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_delivery_assignments PRIMARY KEY (id),
    CONSTRAINT uk_delivery_assignment_shipment UNIQUE (seller_order_id),
    CONSTRAINT fk_delivery_assignment_partner FOREIGN KEY (partner_id) REFERENCES delivery_partners (id),
    CONSTRAINT fk_delivery_assignment_shipment FOREIGN KEY (seller_order_id) REFERENCES seller_orders (id),
    INDEX idx_delivery_assignment_partner (partner_id, created_at)
) ENGINE=InnoDB;
