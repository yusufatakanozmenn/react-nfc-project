-- Run once using a migration/admin account. Existing cards retain their IDs and ownership.
ALTER TABLE nfc_cards ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
