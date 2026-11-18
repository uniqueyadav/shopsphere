ALTER TABLE categories
    ADD COLUMN parent_id BIGINT NULL,
    ADD COLUMN image_public_id VARCHAR(255) NULL,
    ADD CONSTRAINT fk_categories_parent
        FOREIGN KEY (parent_id) REFERENCES categories (id),
    ADD INDEX idx_categories_parent (parent_id);
