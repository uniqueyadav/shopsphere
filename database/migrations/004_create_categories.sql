CREATE TABLE IF NOT EXISTS categories (
	id BIGINT NOT NULL AUTO_INCREMENT,
	name VARCHAR(100) NOT NULL,
	slug VARCHAR(120) NOT NULL,
	description VARCHAR(1000) NULL,
	image_url VARCHAR(2048) NULL,
	is_active BOOLEAN NOT NULL DEFAULT TRUE,
	created_at DATETIME(6) NOT NULL,
	updated_at DATETIME(6) NOT NULL,
	CONSTRAINT pk_categories PRIMARY KEY (id),
	CONSTRAINT uk_categories_name UNIQUE (name),
	CONSTRAINT uk_categories_slug UNIQUE (slug)
) ENGINE=InnoDB;
