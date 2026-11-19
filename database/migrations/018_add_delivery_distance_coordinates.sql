ALTER TABLE addresses
    ADD COLUMN latitude DECIMAL(10, 7) NULL,
    ADD COLUMN longitude DECIMAL(10, 7) NULL;

ALTER TABLE sellers
    ADD COLUMN pickup_latitude DECIMAL(10, 7) NULL,
    ADD COLUMN pickup_longitude DECIMAL(10, 7) NULL;
