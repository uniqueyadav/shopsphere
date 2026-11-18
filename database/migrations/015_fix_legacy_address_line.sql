-- Older databases may still have a required address_line column alongside
-- street_area. Current Address inserts do not write address_line, so provide
-- an empty default while preserving any existing legacy values.
SET @schema_name = DATABASE();
SET @has_legacy_address_line = (
	SELECT COUNT(*)
	FROM information_schema.columns
	WHERE table_schema = @schema_name
		AND table_name = 'addresses'
		AND column_name = 'address_line'
);
SET @migration_sql = IF(
	@has_legacy_address_line > 0,
	"ALTER TABLE addresses ALTER COLUMN address_line SET DEFAULT ''",
	'SELECT 1'
);
PREPARE migration_statement FROM @migration_sql;
EXECUTE migration_statement;
DEALLOCATE PREPARE migration_statement;
