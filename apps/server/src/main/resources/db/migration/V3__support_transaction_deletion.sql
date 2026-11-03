ALTER TABLE transactions ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TYPE draft_status ADD VALUE 'DELETED';
ALTER TABLE transaction_drafts DROP CONSTRAINT ck_transaction_drafts_confirmation;
ALTER TABLE transaction_drafts ADD CONSTRAINT ck_transaction_drafts_confirmation CHECK (
    (status::text = 'CONFIRMED' AND transaction_id IS NOT NULL)
    OR (status::text IN ('PENDING', 'CANCELLED', 'DELETED') AND transaction_id IS NULL)
);

-- Only source identifiers are retained: replay must not recreate erased ledger rows.
CREATE TABLE deleted_transaction_sources (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id),
    channel channel_type NOT NULL,
    chat_id TEXT NOT NULL,
    message_id TEXT NOT NULL,
    UNIQUE(user_id, channel, chat_id, message_id)
);

-- Edit field messages and /cancel must never replay into the creation interpreter.
CREATE TABLE transaction_action_inputs (
    id UUID PRIMARY KEY,
    channel channel_type NOT NULL,
    external_user_id TEXT NOT NULL,
    chat_id TEXT NOT NULL,
    message_id TEXT NOT NULL,
    UNIQUE(channel, external_user_id, chat_id, message_id)
);
