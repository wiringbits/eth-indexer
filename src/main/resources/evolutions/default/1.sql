
-- !Ups
CREATE TABLE blocks(
  number BIGINT NOT NULL,
  block_hash TEXT NOT NULL,
  parent_block_hash TEXT NOT NULL,
  nonce DECIMAL NOT NULL,
  sha3_uncles TEXT NOT NULL,
  transactions_root TEXT NOT NULL,
  state_root TEXT NOT NULL,
  receipt_root TEXT NOT NULL,
  author TEXT NULL,
  miner TEXT NOT NULL,
  mix_hash TEXT NOT NULL,
  difficulty BIGINT NOT NULL,
  total_difficulty DECIMAL NOT NULL,
  extra_data TEXT NOT NULL,
  size BIGINT NOT NULL,
  gas_limit BIGINT NOT NULL,
  gas_used BIGINT NOT NULL,
  time BIGINT NOT NULL,
  CONSTRAINT blocks_block_hash_pk PRIMARY KEY (block_hash),
  CONSTRAINT blocks_number_unique UNIQUE (number),
  CONSTRAINT blocks_parent_block_hash_unique UNIQUE (parent_block_hash)
);

CREATE TABLE transactions(
  hash TEXT NOT NULL,
  nonce BIGINT NOT NULL,
  block_hash TEXT NOT NULL,
  block_number BIGINT NOT NULL,
  transaction_index BIGINT NOT NULL,
  from_address TEXT NOT NULL,
  to_address TEXT NULL,
  value DECIMAL NOT NULL,
  gas_price BIGINT NOT NULL,
  gas BIGINT NOT NULL,
  input TEXT NOT NULL,
  creates TEXT NULL,
  public_key TEXT NULL,
  raw TEXT NULL,
  CONSTRAINT transactions_hash_pk PRIMARY KEY (hash),
  CONSTRAINT transactions_blocks_number_fk FOREIGN KEY (block_number) REFERENCES blocks(number),
  CONSTRAINT transactions_blocks_block_hash_fk FOREIGN KEY (block_hash) REFERENCES blocks(block_hash)
);

CREATE INDEX transactions_from_address_index ON transactions(from_address);
CREATE INDEX transactions_to_address_index ON transactions(to_address);

-- !Downs
DROP TABLE transactions;
DROP TABLE blocks;
