ALTER TABLE order_status_history
    ADD COLUMN location_latitude DECIMAL(10, 7) NULL,
    ADD COLUMN location_longitude DECIMAL(10, 7) NULL;
