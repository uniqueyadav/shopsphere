CREATE TABLE inventory_history (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    product_id BIGINT NOT NULL,
    changed_by_user_id BIGINT NULL,
    previous_quantity INT NOT NULL,
    new_quantity INT NOT NULL,
    change_type VARCHAR(32) NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_inventory_history_product_created (product_id, created_at),
    CONSTRAINT fk_inventory_history_product FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT fk_inventory_history_user FOREIGN KEY (changed_by_user_id) REFERENCES shop_users (id)
);
