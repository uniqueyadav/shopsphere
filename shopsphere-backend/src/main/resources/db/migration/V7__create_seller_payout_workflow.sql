CREATE TABLE seller_payout_accounts (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    seller_id BIGINT NOT NULL,
    method VARCHAR(20) NOT NULL,
    account_holder VARCHAR(120) NOT NULL,
    encrypted_destination TEXT NOT NULL,
    masked_destination VARCHAR(40) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_seller_payout_account_seller (seller_id),
    CONSTRAINT fk_payout_account_seller FOREIGN KEY (seller_id) REFERENCES sellers (id)
);

CREATE TABLE seller_payouts (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    seller_id BIGINT NOT NULL,
    payout_account_id BIGINT NOT NULL,
    payment_method VARCHAR(20) NOT NULL,
    account_holder VARCHAR(120) NOT NULL,
    encrypted_destination TEXT NOT NULL,
    masked_destination VARCHAR(40) NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    idempotency_key VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL,
    transfer_reference VARCHAR(120) NULL,
    remarks VARCHAR(500) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_payout_seller_idempotency (seller_id, idempotency_key),
    INDEX idx_seller_payout_created (seller_id, created_at),
    INDEX idx_seller_payout_status (status, created_at),
    CONSTRAINT fk_payout_seller FOREIGN KEY (seller_id) REFERENCES sellers (id),
    CONSTRAINT fk_payout_account FOREIGN KEY (payout_account_id) REFERENCES seller_payout_accounts (id)
);
