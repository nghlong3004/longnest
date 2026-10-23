-- Durable, bounded-live confirmation controls. Completed/expired bindings are never purged.
CREATE TABLE ledger_control (
  id VARCHAR(32) PRIMARY KEY CHECK (id ~ '^[0-9a-f]{32}$'),
  owner_id VARCHAR(200) NOT NULL REFERENCES ledger_owner_head(owner_id),
  interaction_scope VARCHAR(100) NOT NULL,
  request_id VARCHAR(200) NOT NULL,
  request_fingerprint VARCHAR(64) NOT NULL CHECK (request_fingerprint ~ '^[0-9a-f]{64}$'),
  kind VARCHAR(16) NOT NULL CHECK (kind IN ('PROMPT', 'UPDATE', 'DELETE', 'RESTORE')),
  status VARCHAR(16) NOT NULL CHECK (status IN ('PENDING', 'FILLED', 'REPLACED', 'CANCELED', 'COMPLETED')),
  field VARCHAR(16),
  origin_id VARCHAR(32),
  successor_id VARCHAR(32),
  target_id BIGINT NOT NULL CHECK (target_id > 0),
  expected_revision BIGINT NOT NULL CHECK (expected_revision > 0),
  created_at TIMESTAMPTZ(6) NOT NULL,
  message_at TIMESTAMPTZ(6),
  before_deleted BOOLEAN NOT NULL,
  after_deleted BOOLEAN NOT NULL,
  before_type VARCHAR(16) NOT NULL CHECK (before_type IN ('INCOME', 'EXPENSE')),
  before_amount BIGINT NOT NULL CHECK (before_amount BETWEEN 1 AND 1000000000000),
  before_category VARCHAR(32) NOT NULL,
  before_note VARCHAR(1024) NOT NULL CHECK (char_length(before_note) <= 256 AND before_note IS NFC NORMALIZED
    AND before_note !~ U&'[\0001-\001F\007F-\009F\2028\2029]'),
  before_date DATE NOT NULL CHECK (before_date BETWEEN DATE '1900-01-01' AND DATE '9999-12-31'),
  after_type VARCHAR(16) NOT NULL CHECK (after_type IN ('INCOME', 'EXPENSE')),
  after_amount BIGINT NOT NULL CHECK (after_amount BETWEEN 1 AND 1000000000000),
  after_category VARCHAR(32) NOT NULL,
  after_note VARCHAR(1024) NOT NULL CHECK (char_length(after_note) <= 256 AND after_note IS NFC NORMALIZED
    AND after_note !~ U&'[\0001-\001F\007F-\009F\2028\2029]'),
  after_date DATE NOT NULL CHECK (after_date BETWEEN DATE '1900-01-01' AND DATE '9999-12-31'),
  expires_at TIMESTAMPTZ(6) NOT NULL,
  display_message BIGINT CHECK (display_message > 0),
  receipt_generation BIGINT CHECK (receipt_generation >= 0),
  receipt_changed BOOLEAN,
  row_version BIGINT NOT NULL DEFAULT 0 CHECK (row_version >= 0),
  UNIQUE (owner_id, request_id),
  FOREIGN KEY (owner_id, target_id) REFERENCES ledger_entry(owner_id, id),
  FOREIGN KEY (origin_id) REFERENCES ledger_control(id),
  FOREIGN KEY (successor_id) REFERENCES ledger_control(id),
  CHECK ((kind = 'PROMPT' AND field IS NOT NULL
      AND field IN ('AMOUNT','TYPE','CATEGORY','NOTE','DATE'))
    OR (kind <> 'PROMPT' AND field IS NULL)),
  CHECK (before_category = 'UNCATEGORIZED'
    OR (before_type = 'EXPENSE' AND before_category IN ('FOOD','GROCERIES','TRANSPORT','RENT'))
    OR (before_type = 'INCOME' AND before_category IN ('SALARY','OTHER_INCOME'))),
  CHECK (after_category = 'UNCATEGORIZED'
    OR (after_type = 'EXPENSE' AND after_category IN ('FOOD','GROCERIES','TRANSPORT','RENT'))
    OR (after_type = 'INCOME' AND after_category IN ('SALARY','OTHER_INCOME'))),
  CHECK ((status = 'COMPLETED' AND receipt_generation IS NOT NULL AND receipt_changed IS NOT NULL)
    OR (status <> 'COMPLETED' AND receipt_generation IS NULL AND receipt_changed IS NULL))
);
CREATE INDEX ledger_control_reply_idx ON ledger_control(owner_id, interaction_scope, display_message)
  WHERE kind = 'PROMPT';
CREATE INDEX ledger_control_live_idx ON ledger_control(owner_id, status, expires_at);

-- One callback delivery event can select only one action and verb, across all scopes.
CREATE TABLE ledger_control_event (
  owner_id VARCHAR(200) NOT NULL REFERENCES ledger_owner_head(owner_id),
  operation_id VARCHAR(200) NOT NULL,
  fingerprint VARCHAR(64) NOT NULL CHECK (fingerprint ~ '^[0-9a-f]{64}$'),
  control_id VARCHAR(32) NOT NULL REFERENCES ledger_control(id),
  PRIMARY KEY (owner_id, operation_id)
);
