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
```

From the repository root, run:

```sh
bash scripts/dev-server.sh
```

The launcher reads `.env.dev`; Spring does not automatically load `.env` files.
Database settings remain those required by the existing launcher.

Send text to the bot in a private chat. Only the configured Telegram user's text
is passed to DeepSeek, and the model's answer is returned to the same chat.
Group messages, media and other senders are ignored. When DeepSeek is disabled,
the previous echo behavior remains available.

Each model request currently contains the latest message; conversation memory,
classification and financial persistence are not wired yet. Provider failures
produce a short retry message. Long answers are split into Telegram-sized plain
text messages without breaking emoji. See [the flow](../../docs/telegram-chatbot.md).

The SDK uses long polling. Run one instance per bot token. Shutdown stops polling
and closes the managed HTTP/executor resources. Logs omit tokens and message text.

Synthetic checks, run from `apps/server`:

```sh
mvn -o -B -ntp test
```

Tests use synthetic Telegram/DeepSeek credentials, mocked provider HTTP and
PostgreSQL Testcontainers. They do not call the real model or Telegram API.
