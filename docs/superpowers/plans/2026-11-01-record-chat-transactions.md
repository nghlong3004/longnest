# Record chat transactions

> Implementation follows the flow agreed in chat. Work in the current workspace;
> keep the change uncommitted until requested.

**Goal:** Parse model JSON into `ChatResponse`, validate it, persist `READY`
transactions, and confirm using the committed data.

**Architecture:** Keep `TelegramMessageHandler → ChatService → ChatClient`.
`ChatService` receives a channel-neutral message with trusted sender/source/time
metadata. A transactional `TransactionService` resolves `UserIdentity` and writes
through Spring Data JPA; MapStruct maps extracted fields to the existing entity.

**Constraints:** Java 25, Spring Boot 4, Jackson 3, Jakarta Validation,
PostgreSQL/Flyway. Preserve the applied V1 migration and support for multiple
rows referencing one source message. No draft storage/merge in this step.

**Decisions:**
- Invalid JSON, action/type/amount, unknown enums, and income with an expense
  category must fail before any write. Retain partial clarification data.
- `CHAT` and `ASK_CLARIFICATION` send `reply` and do not write to the ledger.
- Missing transaction date uses the original Telegram timestamp in the configured
  ledger time zone (default `Asia/Ho_Chi_Minh`). Give the same date to the model.
- Lock the resolved identity while checking for an existing transaction from this
  source. Reprocessing an already recorded message returns the existing row;
  separate messages with identical content still record separate transactions.
- Confirm only after the transactional service returns (commit succeeded).
  Format confirmation from stored type, amount, and description, never the LLM's
  claim. Provider, parsing, and DB failures use localized application error codes.
- Preserve disabled-model echo and Telegram authorization/plain-text splitting.

## Implementation

- [x] Write failing service tests for parsed replies and malformed JSON.
- [x] Add `ChatMessage`, strict parsing/action validation, and reference-date context.
- [x] Add repositories, MapStruct mapper, transactional save service, date default,
  and duplicate-source check under an identity lock.
- [x] Adapt Telegram and existing tests to pass trusted metadata.
- [x] Exercise real PostgreSQL writes/commit, identity reuse, date boundaries,
  replay, parallel replay, rejected JSON, non-saving actions, and rollback.
- [x] Update flow docs; run the full Maven suite and `git diff --check`.
- [x] Restart the existing development bot for the user's manual test.

Verification: `mvn -o -B -ntp test` passed 82 tests (no failures/errors/skips),
including Telegram typing renewal and cleanup. Pre-commit review simplified
chat ID reuse, provider-call boundaries, and action validation without adding
new abstractions; the full suite passed again after that cleanup.
Review identified numeric-enum coercion; four regression cases failed before
enabling `EnumFeature.FAIL_ON_NUMBERS_FOR_ENUMS` and passed afterward.
Native HTTP tests also verify both the default schema and runtime reference date
reach the configured DeepSeek client. The development bot started successfully;
real message classification and persistence are ready for the user's manual test.
