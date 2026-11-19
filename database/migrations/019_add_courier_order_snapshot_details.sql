ALTER TABLE orders
    ADD COLUMN shipping_latitude DECIMAL(10, 7) NULL,
    ADD COLUMN shipping_longitude DECIMAL(10, 7) NULL,
    ADD COLUMN shipping_delivery_instructions VARCHAR(500) NULL,
    ADD COLUMN shipping_leave_at_door BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN shipping_call_before_delivery BOOLEAN NOT NULL DEFAULT FALSE;
