
-- !Ups
ALTER TABLE transactions ADD COLUMN token_transfer_recipient TEXT NULL;

CREATE INDEX transactions_token_transfer_recipient_index ON transactions(token_transfer_recipient);
