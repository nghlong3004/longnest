ALTER TABLE transaction_drafts
    ADD COLUMN original_sent_at TIMESTAMPTZ,
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

UPDATE transaction_drafts SET original_sent_at = created_at;
ALTER TABLE transaction_drafts
    ALTER COLUMN original_sent_at SET NOT NULL,
    ALTER COLUMN original_sent_at SET DEFAULT CURRENT_TIMESTAMP;

-- Routing and replay receipts only; message bodies remain on the draft.
CREATE TABLE draft_message_links (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    draft_id UUID NOT NULL REFERENCES transaction_drafts (id),
    channel channel_type NOT NULL,
    chat_id TEXT NOT NULL CHECK (btrim(chat_id) <> ''),
    message_id TEXT NOT NULL CHECK (btrim(message_id) <> ''),
    CONSTRAINT uq_draft_message_links_source UNIQUE (channel, chat_id, message_id)
);

CREATE INDEX idx_draft_message_links_draft ON draft_message_links (draft_id);

INSERT INTO draft_message_links (draft_id, channel, chat_id, message_id)
SELECT id, channel, chat_id, message_id FROM transaction_drafts
ON CONFLICT (channel, chat_id, message_id) DO NOTHING;

INSERT INTO draft_message_links (draft_id, channel, chat_id, message_id)
SELECT id, channel, chat_id, question_message_id FROM transaction_drafts
WHERE question_message_id IS NOT NULL
ON CONFLICT (channel, chat_id, message_id) DO NOTHING;
