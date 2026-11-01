CREATE TYPE transaction_type AS ENUM ('INCOME', 'EXPENSE');

CREATE TYPE transaction_category AS ENUM (
    'FOOD',
    'HOUSING',
    'TRANSPORT',
    'HEALTH',
    'EDUCATION',
    'SHOPPING',
    'ENTERTAINMENT',
    'OTHER'
);

CREATE TYPE time_of_day AS ENUM ('MORNING', 'NOON', 'AFTERNOON', 'EVENING');

CREATE TYPE channel_type AS ENUM ('TELEGRAM');

CREATE TYPE draft_status AS ENUM ('PENDING', 'CONFIRMED', 'CANCELLED');

CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE user_identities (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users (id),
    channel channel_type NOT NULL,
    external_user_id TEXT NOT NULL CHECK (btrim(external_user_id) <> ''),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_user_identities_channel_external_user UNIQUE (channel, external_user_id)
);

CREATE INDEX idx_user_identities_user ON user_identities (user_id);

CREATE TABLE transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users (id),
    type transaction_type NOT NULL,
    amount NUMERIC(19, 2) NOT NULL CHECK (amount > 0 AND amount <> 'NaN'::NUMERIC),
    category transaction_category,
    description TEXT,
    location TEXT,
    date DATE NOT NULL,
    time_of_day time_of_day,
    channel channel_type NOT NULL,
    chat_id TEXT NOT NULL CHECK (btrim(chat_id) <> ''),
    message_id TEXT NOT NULL CHECK (btrim(message_id) <> ''),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- The composite key lets drafts enforce ownership when referencing a transaction.
    CONSTRAINT uq_transactions_id_user UNIQUE (id, user_id)
);

CREATE INDEX idx_transactions_user_date ON transactions (user_id, date DESC, id DESC);

CREATE TABLE transaction_drafts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users (id),
    type transaction_type,
    amount NUMERIC(19, 2) CHECK (amount > 0 AND amount <> 'NaN'::NUMERIC),
    category transaction_category,
    description TEXT,
    location TEXT,
    date DATE,
    time_of_day time_of_day,
    channel channel_type NOT NULL,
    chat_id TEXT NOT NULL CHECK (btrim(chat_id) <> ''),
    message_id TEXT NOT NULL CHECK (btrim(message_id) <> ''),
    original_text TEXT NOT NULL,
    question_text TEXT,
    question_message_id TEXT CHECK (btrim(question_message_id) <> ''),
    status draft_status NOT NULL DEFAULT 'PENDING',
    transaction_id UUID UNIQUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_transaction_drafts_transaction_owner
        FOREIGN KEY (transaction_id, user_id) REFERENCES transactions (id, user_id),
    CONSTRAINT ck_transaction_drafts_confirmation CHECK (
        (status = 'CONFIRMED' AND transaction_id IS NOT NULL)
        OR (status IN ('PENDING', 'CANCELLED') AND transaction_id IS NULL)
    )
);

CREATE INDEX idx_transaction_drafts_user_status
    ON transaction_drafts (user_id, status, updated_at DESC);

CREATE UNIQUE INDEX uq_transaction_drafts_question
    ON transaction_drafts (channel, chat_id, question_message_id)
    WHERE question_message_id IS NOT NULL;
