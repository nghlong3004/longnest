# Telegram interaction refactor implementation plan

> **For agentic workers:** use executing-plans for coordinated inline work and focused delegation for the independent field/presentation unit. The user subsequently requested commit + normal push after final verification.

**Goal:** Resolve the accepted review findings without changing ledger semantics.
**Architecture:** Typed edit state and bounded registry; sender Adapter; callback Command handlers/router; channel-independent action text.
**Tech Stack:** Existing Java 25 / Spring Boot 4.1.1 / Telegram SDK / JUnit / PostgreSQL Testcontainers.
**Spec:** docs/superpowers/specs/2026-11-04-telegram-refactor-design.md

## Global Constraints

Preserve all ownership/replay/draft/version checks and commit-before-delivery semantics. No new dependencies, migration changes, real external sends, runtime restarts. Commit + normal push were subsequently authorized by the user. Preserve the preceding package moves in this workspace. All Maven execution is coordinated by the lead, with synthetic providers and disposable PostgreSQL only.

## Review Focus

Failed menu/field delivery remains retryable; superseded preview Save is inert; evicted pagination is denied; refresh/deletion covers bounded duplicate cards; slow chat does not hold a global I/O monitor. Keep Unicode-safe chunking and controls on the final preview chunk.

### Task 1: Typed field and action text

Files: model/enums/TransactionField.java, service/TransactionMutationService.java, service/impl/TransactionMutationServiceImpl.java, presentation/TransactionActionPresentation.java (move TelegramTransactionPresentation), relevant presentation and mutation tests.
Interfaces: TransactionField constants AMOUNT, DATE, TYPE, CATEGORY, DESCRIPTION, LOCATION, TIME_OF_DAY; Lombok getValue() gives existing callback values; fromValue(String) rejects unknown values. preview(TransactionRevision, TransactionField, String). Presenter methods instruction(TransactionField), fieldLabel(TransactionField), previewText(UUID, TransactionResponse, TransactionField, TransactionResponse), plus existing formatter dependency. Use String for rendered selected-field values.
- [x] Keep existing mutation/presentation tests, add typed boundary/invalid-field coverage where meaningful.
- [x] Implement enum, update service and move presenter, removing repeated string field selection.
- [x] Verify focused tests; main integrates cross-cutting constructors/callers.

### Task 2: State, registry and sender

Files: TelegramTransactionActions plus TelegramEditSession, TelegramTransactionRegistry, TelegramTransactionKeyboard, TelegramMessageSender; TelegramTransactionMutationTests and new focused tests.
- [x] Add failing behavior tests for retry after menu/field delivery failure, stale preview after invalid replacement, registry expiry/eviction, bounded repeated refresh and chat lock isolation.
- [x] Implement sender with shared Unicode splitting, edit/delete/ack outcomes and optional keyboard on first/last chunk.
- [x] Encapsulate valid edit transitions; store/rotate tokens through registry. Keep successful transport required before menu/field activation and full preview delivery before Save activation.
- [x] Bound chat/views/card state and consume active views on refresh. Reject unknown pagination. Remove unbounded deleted/revoked sets.
- [x] Run focused interaction/state/sender tests and inspect failures.

### Task 3: Callback routing, integration and verification

Files: TelegramCallbackHandler/Router, TelegramQueryCallbackHandler, TelegramDraftCallbackHandler, TelegramMessageHandler and tests; README and Telegram flow docs.
- [x] Route authenticated callbacks to independent handlers; keep validation before routing. Use sender everywhere and injected Clock for synthetic callback sources.
- [x] Update constructor/typed-field callers and synthetic fixtures; exercise pagination with tracked active pages.
- [x] Run clean full offline Maven suite, diff checks and formatter if available. Perform independent final review, address actionable findings, document UI retention limits.

## Verification

Final `mvn -o -B -ntp clean test`: 382 tests, zero failures/errors/skips, BUILD SUCCESS.
Google Java Format and diff checks passed. Independent review identified two lifecycle
gaps; regression tests reproduced both before fixing receipt-failure preview retirement
and pagination authorization bound to the delivered card message ID.
No real Telegram/provider probes, private environment edits or development restart.
