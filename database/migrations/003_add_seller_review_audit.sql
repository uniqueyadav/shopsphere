SET @schema_name = DATABASE();

SET @has_reviewed_by = (
	SELECT COUNT(*)
	FROM information_schema.columns
	WHERE table_schema = @schema_name
		AND table_name = 'sellers'
		AND column_name = 'reviewed_by'
);
SET @migration_sql = IF(
	@has_reviewed_by = 0,
	'ALTER TABLE sellers ADD COLUMN reviewed_by BIGINT NULL',
	'SELECT 1'
);
PREPARE migration_statement FROM @migration_sql;
EXECUTE migration_statement;
DEALLOCATE PREPARE migration_statement;

SET @has_reviewed_at = (
	SELECT COUNT(*)
	FROM information_schema.columns
	WHERE table_schema = @schema_name
		AND table_name = 'sellers'
		AND column_name = 'reviewed_at'
);
SET @migration_sql = IF(
	@has_reviewed_at = 0,
	'ALTER TABLE sellers ADD COLUMN reviewed_at DATETIME(6) NULL',
	'SELECT 1'
);
PREPARE migration_statement FROM @migration_sql;
EXECUTE migration_statement;
DEALLOCATE PREPARE migration_statement;

SET @has_rejection_reason = (
	SELECT COUNT(*)
	FROM information_schema.columns
	WHERE table_schema = @schema_name
		AND table_name = 'sellers'
		AND column_name = 'rejection_reason'
);
SET @migration_sql = IF(
	@has_rejection_reason = 0,
	'ALTER TABLE sellers ADD COLUMN rejection_reason VARCHAR(1000) NULL',
	'SELECT 1'
);
PREPARE migration_statement FROM @migration_sql;
EXECUTE migration_statement;
DEALLOCATE PREPARE migration_statement;

SET @has_reviewed_by_foreign_key = (
	SELECT COUNT(*)
	FROM information_schema.table_constraints
	WHERE constraint_schema = @schema_name
		AND table_name = 'sellers'
		AND constraint_name = 'fk_sellers_reviewed_by'
		AND constraint_type = 'FOREIGN KEY'
);
SET @migration_sql = IF(
	@has_reviewed_by_foreign_key = 0,
	'ALTER TABLE sellers ADD CONSTRAINT fk_sellers_reviewed_by FOREIGN KEY (reviewed_by) REFERENCES shop_users(id)',
	'SELECT 1'
);
PREPARE migration_statement FROM @migration_sql;
EXECUTE migration_statement;
DEALLOCATE PREPARE migration_statement;
