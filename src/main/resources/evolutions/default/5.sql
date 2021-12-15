
-- !Ups
ALTER TABLE transactions ALTER COLUMN status DROP NOT NULL;

ALTER TABLE transactions ALTER COLUMN gas_price type DECIMAL;