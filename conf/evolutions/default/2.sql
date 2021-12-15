
-- !Ups
ALTER TABLE transactions ADD COLUMN timestamp BIGINT NULL;

-- !Downs
ALTER TABLE transactions DROP COLUMN timestamp;
