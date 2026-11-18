SET @schema_name = DATABASE();

SET @has_seller_full_name = (
	SELECT COUNT(*)
	FROM information_schema.columns
	WHERE table_schema = @schema_name
		AND table_name = 'sellers'
		AND column_name = 'seller_full_name'
);
SET @migration_sql = IF(
	@has_seller_full_name = 0,
	'ALTER TABLE sellers ADD COLUMN seller_full_name VARCHAR(100) NULL',
	'SELECT 1'
);
PREPARE migration_statement FROM @migration_sql;
EXECUTE migration_statement;
DEALLOCATE PREPARE migration_statement;

SET @has_seller_email = (
	SELECT COUNT(*)
	FROM information_schema.columns
	WHERE table_schema = @schema_name
		AND table_name = 'sellers'
		AND column_name = 'seller_email'
);
SET @migration_sql = IF(
	@has_seller_email = 0,
	'ALTER TABLE sellers ADD COLUMN seller_email VARCHAR(254) NULL',
	'SELECT 1'
);
PREPARE migration_statement FROM @migration_sql;
EXECUTE migration_statement;
DEALLOCATE PREPARE migration_statement;

SET @has_seller_phone = (
	SELECT COUNT(*)
	FROM information_schema.columns
	WHERE table_schema = @schema_name
		AND table_name = 'sellers'
		AND column_name = 'seller_phone'
);
SET @migration_sql = IF(
	@has_seller_phone = 0,
	'ALTER TABLE sellers ADD COLUMN seller_phone VARCHAR(20) NULL',
	'SELECT 1'
);
PREPARE migration_statement FROM @migration_sql;
EXECUTE migration_statement;
DEALLOCATE PREPARE migration_statement;

SET @has_business_type = (
	SELECT COUNT(*)
	FROM information_schema.columns
	WHERE table_schema = @schema_name
		AND table_name = 'sellers'
		AND column_name = 'business_type'
);
SET @migration_sql = IF(
	@has_business_type = 0,
	'ALTER TABLE sellers ADD COLUMN business_type ENUM(''INDIVIDUAL'',''LIMITED_LIABILITY_PARTNERSHIP'',''OTHER'',''PARTNERSHIP'',''PRIVATE_LIMITED'',''PUBLIC_LIMITED'',''SOLE_PROPRIETORSHIP'') NULL',
	'SELECT 1'
);
PREPARE migration_statement FROM @migration_sql;
EXECUTE migration_statement;
DEALLOCATE PREPARE migration_statement;

SET @has_gst_number = (
	SELECT COUNT(*)
	FROM information_schema.columns
	WHERE table_schema = @schema_name
		AND table_name = 'sellers'
		AND column_name = 'gst_number'
);
SET @migration_sql = IF(
	@has_gst_number = 0,
	'ALTER TABLE sellers ADD COLUMN gst_number VARCHAR(15) NULL',
	'SELECT 1'
);
PREPARE migration_statement FROM @migration_sql;
EXECUTE migration_statement;
DEALLOCATE PREPARE migration_statement;
