SET @schema_name = DATABASE();

SET @has_created_at = (
	SELECT COUNT(*)
	FROM information_schema.columns
	WHERE table_schema = @schema_name
		AND table_name = 'shop_users'
		AND column_name = 'created_at'
);
SET @migration_sql = IF(
	@has_created_at = 0,
	'ALTER TABLE shop_users ADD COLUMN created_at DATETIME(6) NULL',
	'SELECT 1'
);
PREPARE migration_statement FROM @migration_sql;
EXECUTE migration_statement;
DEALLOCATE PREPARE migration_statement;

SET @has_updated_at = (
	SELECT COUNT(*)
	FROM information_schema.columns
	WHERE table_schema = @schema_name
		AND table_name = 'shop_users'
		AND column_name = 'updated_at'
);
SET @migration_sql = IF(
	@has_updated_at = 0,
	'ALTER TABLE shop_users ADD COLUMN updated_at DATETIME(6) NULL',
	'SELECT 1'
);
PREPARE migration_statement FROM @migration_sql;
EXECUTE migration_statement;
DEALLOCATE PREPARE migration_statement;

UPDATE shop_users
SET created_at = COALESCE(created_at, CURRENT_TIMESTAMP(6)),
	updated_at = COALESCE(updated_at, created_at, CURRENT_TIMESTAMP(6))
WHERE created_at IS NULL OR updated_at IS NULL;

ALTER TABLE shop_users
	MODIFY COLUMN created_at DATETIME(6) NOT NULL,
	MODIFY COLUMN updated_at DATETIME(6) NOT NULL;
