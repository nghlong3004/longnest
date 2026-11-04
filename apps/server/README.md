# Server

Java 25 / Spring Boot 4.1.1 / PostgreSQL / Flyway.

## Java packages

Under `me.nghlong3004.longnest`:

- `service`: business service interfaces; `service.impl` contains their implementations.
- `conversation`: conversation history, model interpretation and Vietnamese query recovery.
- `presentation`: channel-independent plain-text formatting, action previews and display-name presentation.
- `integration`: Telegram and DeepSeek configuration, clients and channel-specific behavior.
  Telegram uses a shared sender adapter, callback command handlers/router, typed edit
  sessions, a bounded UI registry and a separate keyboard renderer.
- `configuration`, `mapper`, `model`, `repository`, `exception`: shared bean configuration,
  object mapping, DTOs/entities/enums, persistence and error handling.

## Telegram chatbot

Configure the repository-root `.env.dev` using the existing names:

```dotenv
LONGNEST_TELEGRAM_ENABLED=true
LONGNEST_TELEGRAM_TOKEN=REPLACE_WITH_BOTFATHER_TOKEN
LONGNEST_TELEGRAM_USERS=REPLACE_WITH_COMMA_SEPARATED_NUMERIC_USER_IDS

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

Send text to the bot in a private chat. Only configured Telegram users' text
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
`/cancel` also cancels the current draft. Interpretation additionally receives the
last six completed user/bot exchanges (12 role-labelled messages), capped at 1000
Unicode code points per historical side. The current input is never truncated.
History is isolated by channel, trusted sender and private chat, source-replay
deduplicated, and limited to 128 least-recently-used conversations. It is process-local:
restart or eviction loses conversational context, not persisted drafts or receipts.
Old turns are interpretation context only, not fresh recording instructions.
Acknowledgments, amount-only inputs without an active draft, and viewing requests
cannot reuse an old READY instruction to create another transaction.
Backend safeguards deny known acknowledgments, query/range follow-ups and ambiguous
standalone number/date continuations, not an allowed vocabulary of financial purposes.
Valid current `Cà phê 35k`, `Tiền điện 500k`, `Taxi 50k` and spelled-out amount wording
keep the existing structured-model extraction path, with or without recent history.
An active persisted draft may receive a spelled-out amount or a date answer such as
`hôm nay`; only established query context makes a bare relative range a query follow-up.
There is one model call per interpreted input, with no history-free second check.
Outside the targeted denials, current-input/draft extraction keeps the baseline model
trust boundary; the guards do not independently prove every proposed field's meaning.
A standalone READY must also preserve a slash/ISO date explicitly introduced by `ngày`
rather than omit it and silently use the incoming message date. Amount expressions
such as `50/2` are not treated as date answers in an amount draft. No extra call is used.

Telegram first/last display names come only from authorized sender metadata. Control
and bidi/format characters are removed, whitespace collapsed, and the label capped
at 80 Unicode code points. Names are optional untrusted display data in a separate
user-role JSON block, never system instructions, identity or financial authority.
The bot speaks as “em” and prioritizes a usable trusted name in greetings instead of
generic “bạn”, without inferring gender. Pure whole-message greetings (`Xin chào`,
`Chào`, `Chào em`, `Xin chào em`, `hello`/`hi` optionally followed by `em`, `/start`,
with optional final punctuation) use a deterministic
name-aware reply after receipt checks, without a model call. Missing/unusable names
get a natural omitted-address greeting. Mixed greetings plus financial/query text
still use the existing interpretation path; recorded or consumed source receipts
take precedence. Echo mode when no chat client exists remains unchanged.
Recording confirmations can use the current trusted name once, retaining the exact
committed amount and literal description. Cards, summaries and repeated action/error
templates omit unnecessary address rather than repeating “bạn” or a name every line.
Presentation additionally drops lone UTF-16 surrogates from labels (also before model
metadata forwarding); valid emoji pairs and literal markup remain intact. Identity,
stored transaction text and the DTO's control/bidi/80-code-point bounds are unchanged.
All outgoing messages are plain text without parse mode, including markup-like names.
Invalid output and provider/database failures produce a short error message.
All bot messages reply to the relevant user message, including questions, errors
and each part of a long answer. Long answers are split into Telegram-sized plain
text messages without breaking emoji. See [the flow](../../docs/telegram-chatbot.md).

Try `Ăn trưa 45k` or `Nhận lương 15 triệu` to save an expense or income.
`Ăn trưa` asks for an amount; reply `45k` to complete it or press Hủy to cancel.
`Xin chào` produces an ordinary reply while preserving any pending draft.

Query income/expenses with `Xem khoản chi hôm nay`. Each result card has **⋯**:
**Sửa → field → value → before/after → Lưu/Hủy**, or **Xóa → transaction-specific
permanent confirmation**. Amount input accepts strict decimals and `50k` = 50000;
dates use `YYYY-MM-DD`. This path never calls the model. While editing, `/cancel`
closes only the edit, leaving creation drafts unchanged. New text fields are capped
at 500 characters; untouched stored long text is preserved.

`Xem chi tiêu gần đây của a đi em` may ask which range, but retains expense intent.
Then `từ tháng trước tới tháng này` (or the full request, including the demonstrated
`Anh muốn hết tất cả khoản chi ...` typo) uses `SINCE_LAST_MONTH`: the first of the
previous calendar month through the new message's local date inclusive. On
2026-11-02 in the ledger zone, the summary shows **01/10/2026 → 02/11/2026**.
A narrow read-only recovery covers these requests even when the model returns a
generic clarification. Other language and explicit dates still use structured model
interpretation and backend validation; this is not a general Vietnamese parser.

The preview shows complete old/new values for the selected field, including long
legacy text, plus compact identity/context and any derived category change. Other
fields are explicitly unchanged, not expanded into a full transaction viewer.
Long previews split at Telegram-safe Unicode boundaries; Save/Cancel appear only
on the final chunk and are bound to that message after complete delivery. Partial
delivery never enables Save; retry input or cancel safely without changing the ledger.

Flyway V3 enables hard ledger deletion for direct and confirmed-draft transactions
without changing V1/V2. Source identifiers and terminal draft receipts prevent
replay from resurrecting erased rows. After commit, Telegram removal is best
effort; failed removal falls back to a deleted/inert card. If all Telegram updates
fail, the DB result still stands and stale actions are rejected server-side.
Tracked query views reset to fresh first pages and totals after a mutation.
Source-only edit-input receipts also prevent consumed field messages and edit
`/cancel` messages from replaying into creation, even after restart.
Edit sessions expire after 15 minutes and do not survive restart; old callbacks
then require a new query. View tracking is process-local and bounded: at most 128 chats, 20 active query
pages per chat for one hour, and 200 recent card references per chat for deletion.
Expired, evicted or pre-restart pagination/actions require a fresh query; cards
outside the retained references cannot be removed automatically. Evicting a chat
also discards its edit session; new, previously unseen text then follows ordinary
chat handling. Financial rows and durable replay receipts are unaffected. User-facing text reports ledger success separately
from any actual failure to refresh/remove displayed messages, without platform caveats.

The SDK uses long polling. Run one instance per bot token. Shutdown stops polling
and closes the managed HTTP/executor resources. Logs omit tokens and message text.

Synthetic checks, run from `apps/server`:

```sh
mvn -o -B -ntp test
```

Tests use synthetic Telegram/DeepSeek credentials, mocked provider HTTP and
PostgreSQL Testcontainers. They do not call the real model or Telegram API.
