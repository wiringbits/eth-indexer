
-- !Ups
CREATE INDEX transactions_block_hash_index ON transactions USING BTREE(block_hash);
CREATE INDEX transactions_block_number_index ON transactions USING BTREE(block_number);


-- !Downs
DROP INDEX transactions_block_hash_index;
DROP INDEX transactions_block_number_index;