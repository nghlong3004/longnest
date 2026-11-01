# Server

Java 25 / Spring Boot 4.1.1 / PostgreSQL / Flyway.

## Telegram chatbot

Configure the repository-root `.env.dev` using the existing names:

```dotenv
LONGNEST_TELEGRAM_ENABLED=true
LONGNEST_TELEGRAM_TOKEN=REPLACE_WITH_BOTFATHER_TOKEN
LONGNEST_TELEGRAM_USER=REPLACE_WITH_POSITIVE_NUMERIC_USER_ID

LONGNEST_DEEPSEEK_ENABLED=true
LONGNEST_DEEPSEEK_API_KEY=REPLACE_WITH_PRIVATE_API_KEY
LONGNEST_DEEPSEEK_BASE_URL=https://api.deepseek.com
LONGNEST_DEEPSEEK_MODEL=deepseek-flash
LONGNEST_LEDGER_TIME_ZONE=Asia/Ho_Chi_Minh
```

From the repository root, run:

```sh
bash scripts/dev-server.sh
```

The launcher reads `.env.dev`; Spring does not automatically load `.env` files.
Database settings remain those required by the existing launcher.

Send text to the bot in a private chat. Only the configured Telegram user's text
is passed to DeepSeek. Its JSON is parsed into `ChatResponse` and validated.
`READY` resolves the sender's internal user and saves a transaction before sending
a confirmation built from the committed data. Other actions send only `reply`.
Group messages, media and other senders are ignored. When DeepSeek is disabled,
the previous echo behavior remains available.

Each model request contains the latest message and its reference date in the
configured ledger time zone. Missing transaction dates use that original message
date. Existing identities are reused; replaying a recorded source message returns
the stored transaction without another insert. Clarification persists a draft:
plain text continues the pending draft, and native Reply targets its linked question.
Known details survive multiple rounds and restarts. Each question has a Hủy button
that cancels the draft, removes the clicked question and sends one descriptive
confirmation replying to the original user message. Repeated cancellation does
not send another confirmation.
`/cancel` also cancels the current draft. Context is scoped to the draft, not a full
chat history.
Invalid output and provider/database failures produce a short error message.
All bot messages reply to the relevant user message, including questions, errors
and each part of a long answer. Long answers are split into Telegram-sized plain
text messages without breaking emoji. See [the flow](../../docs/telegram-chatbot.md).

Try `Ăn trưa 45k` or `Nhận lương 15 triệu` to save an expense or income.
`Ăn trưa` asks for an amount; reply `45k` to complete it or press Hủy to cancel.
`Xin chào` produces an ordinary reply while preserving any pending draft.

The SDK uses long polling. Run one instance per bot token. Shutdown stops polling
and closes the managed HTTP/executor resources. Logs omit tokens and message text.

Synthetic checks, run from `apps/server`:

```sh
mvn -o -B -ntp test
```

Tests use synthetic Telegram/DeepSeek credentials, mocked provider HTTP and
PostgreSQL Testcontainers. They do not call the real model or Telegram API.
