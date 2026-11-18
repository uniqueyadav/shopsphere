ALTER TABLE addresses
	ADD COLUMN delivery_instructions VARCHAR(500) NULL,
	ADD COLUMN leave_package_at_door BOOLEAN NOT NULL DEFAULT FALSE,
	ADD COLUMN call_before_delivery BOOLEAN NOT NULL DEFAULT FALSE;
