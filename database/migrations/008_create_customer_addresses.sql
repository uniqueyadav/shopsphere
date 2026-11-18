CREATE TABLE IF NOT EXISTS addresses (
	id BIGINT NOT NULL AUTO_INCREMENT,
	user_id BIGINT NOT NULL,
	full_name VARCHAR(100) NOT NULL,
	mobile_number VARCHAR(20) NOT NULL,
	house_flat_number VARCHAR(100) NOT NULL,
	street_area VARCHAR(255) NOT NULL,
	landmark VARCHAR(150) NULL,
	city VARCHAR(100) NOT NULL,
	state VARCHAR(100) NOT NULL,
	pin_code VARCHAR(20) NOT NULL,
	address_type VARCHAR(10) NOT NULL DEFAULT 'OTHER',
	default_address BOOLEAN NOT NULL DEFAULT FALSE,
	created_at DATETIME(6) NOT NULL,
	updated_at DATETIME(6) NOT NULL,
	CONSTRAINT pk_addresses PRIMARY KEY (id),
	CONSTRAINT fk_addresses_user FOREIGN KEY (user_id) REFERENCES shop_users (id),
	INDEX idx_addresses_user_default (user_id, default_address)
) ENGINE=InnoDB;

SET @schema_name = DATABASE();

SET @has_mobile_number = (
	SELECT COUNT(*)
	FROM information_schema.columns
	WHERE table_schema = @schema_name AND table_name = 'addresses' AND column_name = 'mobile_number'
);
SET @has_phone = (
	SELECT COUNT(*)
	FROM information_schema.columns
	WHERE table_schema = @schema_name AND table_name = 'addresses' AND column_name = 'phone'
);
SET @migration_sql = IF(
	@has_mobile_number = 0 AND @has_phone > 0,
	'ALTER TABLE addresses CHANGE COLUMN phone mobile_number VARCHAR(20) NOT NULL',
	'SELECT 1'
);
PREPARE migration_statement FROM @migration_sql;
EXECUTE migration_statement;
DEALLOCATE PREPARE migration_statement;

SET @has_street_area = (
	SELECT COUNT(*)
	FROM information_schema.columns
	WHERE table_schema = @schema_name AND table_name = 'addresses' AND column_name = 'street_area'
);
SET @has_address_line = (
	SELECT COUNT(*)
	FROM information_schema.columns
	WHERE table_schema = @schema_name AND table_name = 'addresses' AND column_name = 'address_line'
);
SET @migration_sql = IF(
	@has_street_area = 0 AND @has_address_line > 0,
	'ALTER TABLE addresses CHANGE COLUMN address_line street_area VARCHAR(255) NOT NULL',
	'SELECT 1'
);
PREPARE migration_statement FROM @migration_sql;
EXECUTE migration_statement;
DEALLOCATE PREPARE migration_statement;

SET @has_pin_code = (
	SELECT COUNT(*)
	FROM information_schema.columns
	WHERE table_schema = @schema_name AND table_name = 'addresses' AND column_name = 'pin_code'
);
SET @has_postal_code = (
	SELECT COUNT(*)
	FROM information_schema.columns
	WHERE table_schema = @schema_name AND table_name = 'addresses' AND column_name = 'postal_code'
);
SET @migration_sql = IF(
	@has_pin_code = 0 AND @has_postal_code > 0,
	'ALTER TABLE addresses CHANGE COLUMN postal_code pin_code VARCHAR(20) NOT NULL',
	'SELECT 1'
);
PREPARE migration_statement FROM @migration_sql;
EXECUTE migration_statement;
DEALLOCATE PREPARE migration_statement;

SET @has_house_flat_number = (
	SELECT COUNT(*)
	FROM information_schema.columns
	WHERE table_schema = @schema_name AND table_name = 'addresses' AND column_name = 'house_flat_number'
);
SET @migration_sql = IF(
	@has_house_flat_number = 0,
	CONCAT(
		'ALTER TABLE addresses ADD COLUMN house_flat_number VARCHAR(100) NOT NULL DEFAULT ',
		CHAR(39),
		CHAR(39)
	),
	'SELECT 1'
);
PREPARE migration_statement FROM @migration_sql;
EXECUTE migration_statement;
DEALLOCATE PREPARE migration_statement;

SET @has_landmark = (
	SELECT COUNT(*)
	FROM information_schema.columns
	WHERE table_schema = @schema_name AND table_name = 'addresses' AND column_name = 'landmark'
);
SET @migration_sql = IF(
	@has_landmark = 0,
	'ALTER TABLE addresses ADD COLUMN landmark VARCHAR(150) NULL',
	'SELECT 1'
);
PREPARE migration_statement FROM @migration_sql;
EXECUTE migration_statement;
DEALLOCATE PREPARE migration_statement;

SET @has_address_type = (
	SELECT COUNT(*)
	FROM information_schema.columns
	WHERE table_schema = @schema_name AND table_name = 'addresses' AND column_name = 'address_type'
);
SET @migration_sql = IF(
	@has_address_type = 0,
	'ALTER TABLE addresses ADD COLUMN address_type VARCHAR(10) NOT NULL DEFAULT ''OTHER''',
	'SELECT 1'
);
PREPARE migration_statement FROM @migration_sql;
EXECUTE migration_statement;
DEALLOCATE PREPARE migration_statement;
