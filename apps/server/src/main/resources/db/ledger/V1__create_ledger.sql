-- PostgreSQL/UTF8 only. Schema objects, never an automatically seeded personal owner.
CREATE SEQUENCE ledger_entry_id_seq AS BIGINT MINVALUE 1 NO MAXVALUE NO CYCLE;

CREATE TABLE ledger_owner_head (
  owner_id VARCHAR(200) PRIMARY KEY CHECK (owner_id ~ '[^[:space:]]'),
  generation BIGINT NOT NULL CHECK (generation >= 0),
  row_version BIGINT NOT NULL DEFAULT 0 CHECK (row_version >= 0)
);

CREATE TABLE ledger_entry (
  id BIGINT PRIMARY KEY CHECK (id > 0),
  owner_id VARCHAR(200) NOT NULL REFERENCES ledger_owner_head(owner_id),
  transaction_type VARCHAR(16) NOT NULL CHECK (transaction_type IN ('INCOME', 'EXPENSE')),
  amount_minor BIGINT NOT NULL CHECK (amount_minor BETWEEN 1 AND 1000000000000),
  category VARCHAR(32) NOT NULL,
  note VARCHAR(1024) NOT NULL CHECK (char_length(note) <= 256 AND note IS NFC NORMALIZED
    AND note !~ U&'[\0001-\001F\007F-\009F\2028\2029]'),
  transaction_date DATE NOT NULL CHECK (transaction_date BETWEEN DATE '1900-01-01' AND DATE '9999-12-31'),
  business_revision BIGINT NOT NULL CHECK (business_revision >= 1),
  created_at TIMESTAMPTZ(6) NOT NULL,
  message_at TIMESTAMPTZ(6),
  deleted BOOLEAN NOT NULL DEFAULT FALSE,
  row_version BIGINT NOT NULL DEFAULT 0 CHECK (row_version >= 0),
  UNIQUE (owner_id, id),
  CHECK (category = 'UNCATEGORIZED'
    OR (transaction_type = 'EXPENSE' AND category IN ('FOOD', 'GROCERIES', 'TRANSPORT', 'RENT'))
    OR (transaction_type = 'INCOME' AND category IN ('SALARY', 'OTHER_INCOME')))
);

CREATE INDEX ledger_recent_idx ON ledger_entry (owner_id, created_at DESC, id DESC) WHERE deleted = FALSE;

CREATE TABLE ledger_create_inbox (
  owner_id VARCHAR(200) NOT NULL REFERENCES ledger_owner_head(owner_id),
  operation_id VARCHAR(200) NOT NULL CHECK (operation_id ~ '[^[:space:]]'),
  fingerprint VARCHAR(64) NOT NULL CHECK (fingerprint ~ '^[0-9a-f]{64}$'),
  transaction_type VARCHAR(16) NOT NULL CHECK (transaction_type IN ('INCOME', 'EXPENSE')),
  amount_minor BIGINT NOT NULL CHECK (amount_minor BETWEEN 1 AND 1000000000000),
  category VARCHAR(32) NOT NULL,
  note VARCHAR(1024) NOT NULL CHECK (char_length(note) <= 256 AND note IS NFC NORMALIZED
    AND note !~ U&'[\0001-\001F\007F-\009F\2028\2029]'),
  transaction_date DATE NOT NULL CHECK (transaction_date BETWEEN DATE '1900-01-01' AND DATE '9999-12-31'),
  business_reference TIMESTAMPTZ(6) NOT NULL,
  message_at TIMESTAMPTZ(6),
  receipt_entry_id BIGINT,
  receipt_generation BIGINT,
  receipt_created_at TIMESTAMPTZ(6),
  row_version BIGINT NOT NULL DEFAULT 0 CHECK (row_version >= 0),
  PRIMARY KEY (owner_id, operation_id),
  FOREIGN KEY (owner_id, receipt_entry_id) REFERENCES ledger_entry(owner_id, id),
  CHECK ((receipt_entry_id IS NULL AND receipt_generation IS NULL AND receipt_created_at IS NULL)
    OR (receipt_entry_id IS NOT NULL AND receipt_generation IS NOT NULL
      AND receipt_entry_id > 0 AND receipt_generation > 0 AND receipt_created_at IS NOT NULL)),
  CHECK (category = 'UNCATEGORIZED'
    OR (transaction_type = 'EXPENSE' AND category IN ('FOOD', 'GROCERIES', 'TRANSPORT', 'RENT'))
    OR (transaction_type = 'INCOME' AND category IN ('SALARY', 'OTHER_INCOME')))
);
