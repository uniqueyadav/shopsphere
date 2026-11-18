SET @schema_name = DATABASE();

SET @has_suspension_reason = (
	SELECT COUNT(*)
	FROM information_schema.columns
	WHERE table_schema = @schema_name
		AND table_name = 'sellers'
		AND column_name = 'suspension_reason'
);
SET @migration_sql = IF(
	@has_suspension_reason = 0,
	'ALTER TABLE sellers ADD COLUMN suspension_reason VARCHAR(1000) NULL',
	'SELECT 1'
);
PREPARE migration_statement FROM @migration_sql;
EXECUTE migration_statement;
DEALLOCATE PREPARE migration_statement;
