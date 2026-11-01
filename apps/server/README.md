# Server

Java 25 / Spring Boot 4.1.1 / PostgreSQL / Flyway.

## Telegram echo

Configure the repository-root `.env.dev` using the existing names:

```dotenv
LONGNEST_TELEGRAM_ENABLED=true
LONGNEST_TELEGRAM_TOKEN=REPLACE_WITH_BOTFATHER_TOKEN
LONGNEST_TELEGRAM_USER=REPLACE_WITH_POSITIVE_NUMERIC_USER_ID
```

From the repository root, run:

```sh
bash scripts/dev-server.sh
```

The launcher reads `.env.dev`; Spring does not automatically load `.env` files.
Database settings remain those required by the existing launcher.

Send text to the bot in a private chat. It echoes the exact text only for the
configured Telegram user. `/start` is echoed too. Group messages, media and other
senders are ignored. This step does not invoke an LLM or create financial records.

The SDK uses long polling. Run one instance per bot token. Shutdown stops polling
and closes the managed HTTP/executor resources. Logs omit tokens and message text.

Synthetic checks, run from `apps/server`:

```sh
mvn -o -B -ntp test
```

Tests use synthetic Telegram credentials and PostgreSQL Testcontainers.
