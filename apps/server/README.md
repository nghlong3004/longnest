# Backend bootstrap

Java 25 / Spring Boot with pure input and opt-in L1/L2 ledger code, not a verified
durable ledger or live Telegram bot.
The existing dependencies, Maven wrapper and Dockerfile are retained; the SDK
binding adds TelegramBots longpolling/client 10.3.0, without a Spring starter.
MapStruct's
API dependency is now declared using the existing 1.6.3 version property; processor
and Lombok binding configuration is unchanged. No existing dependency version
changed. Docker/container execution is not part of this verification.

## Opt-in DeepSeek conversation

NATIVE-AI-1 replaces the former bespoke capture/range JSON interpreters with Spring
AI 2.0.1 ChatClient, native @Tool callbacks, ToolCallingManager and bounded
MessageWindowChatMemory. No dependency/POM or database schema is added. Default
operation remains local deterministic capture and explicit commands/buttons.
The historical [capture fallback](#opt-in-deepseek-capture-fallback) anchor below
redirects readers to this replacement, not an additional runtime layer.

### Opt-in DeepSeek capture fallback

This historical name is superseded by the conversation route above. The old
DeepSeekInterpreter/RangeQueryInterpreter and their JSON protocols are removed.

### Behavior and boundaries

This is quick personal-finance capture and questions, not extended social chat.
The existing short memory is sufficient; no nickname preferences or persistence
are added. For localized clarifications, Telegram may address the authenticated
user by their available first/last display name. The renderer keeps at most 64
Unicode code points, normalizes NFC and removes controls, markup and unsafe symbols;
missing/unsupported labels retain neutral wording. This is literal presentation
only: names never become principal, event/scope/proof, tool arguments, model prompt
or memory. Financial receipts/pages/confirmation controls remain authoritative.

When enabled, non-ready ordinary text goes through one native model request;
known invalid/ambiguous money, conflicting types and rejected local input stay
local. Clear supported captures, slash commands and authenticated edit prompts/
Save/Delete/Cancel controls still bypass inference. Native callbacks support
list/recent/get, clear create, edit preview, Delete request, Restore and clarification.
There is no model Save/Confirm/Cancel tool, SQL access or model-selected identity.

With frozen Vietnam business date 2026-10-23, synthetic interactions include:

- “hôm qua anh chi bao nhiêu?” → expense total and selectable entries for October 22.
- “tháng này thì sao?” → retain the expense scope, October 1 through October 23.
- “chỉ tiền ăn thôi” → retain dates, select EXPENSE + Category.FOOD only, not groceries.
- An ordinary reply to a conversational page can continue the conversation;
  replies to actual field prompts keep the immutable preview → user Save path.

The model chooses semantic TODAY/YESTERDAY/THIS_MONTH/LAST_MONTH or explicit ISO
endpoints; application code resolves calendar arithmetic from the authenticated
message reference in Asia/Ho_Chi_Minh. THIS_MONTH is month-to-date; LAST_MONTH is the
whole previous month. KEEP requires a validated previous query. Unsupported category/
note filters, fuzzy targets or missing facts must clarify. Literal current-message
amounts use exact local Money conversion, including nghìn/ngàn/triệu; bare 45 stays
45 VND. Selected or explicitly supplied IDs are authorized by shared services.
Semantic correctness still depends on the model; fake responses do not demonstrate
Vietnamese accuracy, arbitrary intent safety or prompt-injection resistance.

Only one completed, schema-validated native call may execute per turn. Duplicate/
foreign fields, unknown tools, multi-call responses, truncation or narrated success
cannot dispatch. Host-only context carries identity, event, reference and target
revision; callbacks call existing services, and Vietnamese rendering uses only
service results. No ledger contents or tool results return to the model, and no
second model call/automatic tool loop is made. Native tools are standard function
schemas, not DeepSeek beta strict-function/schema guarantees.

Before inference, recover any frozen create, preview/Delete request or completed
Restore for the same host event. Storage failure stops interpretation. Recovery
returns the original outcome, not a claim the record is still current; shared
services preserve fingerprints/revisions and actual-user displayed-target proofs.
No DB lock is held during inference. There is no exactly-once reply/charge promise;
read-only deliveries can be reinterpreted and re-read.

Conversation identity is host-derived from principal + bot/private-user scope.
Memory keeps at most eight bounded user utterances, no assistant/tool results;
validated query and selected target metadata expire after ten idle minutes.
Sixteen conversation contexts maximum; restart, eviction and /reset clear temporary
context, not ledger or durable confirmations. User utterances/history would leave
the host if enabled: this exposure needs explicit live privacy approval.

### Configuration and live limits

After separate live approval only, privately supply the existing names:

- LONGNEST_DEEPSEEK_ENABLED: default false; true opts into the code route.
- LONGNEST_DEEPSEEK_API_KEY: explicit private credential.
- LONGNEST_DEEPSEEK_BASE_URL: exactly https://api.deepseek.com, no redirects.
- LONGNEST_DEEPSEEK_MODEL: explicit model identifier, no default alias.

Init/smoke cannot construct the provider. Keep spring.ai.model.chat=none; the
opt-in factory owns this binding. Root .env.dev loading remains explicit through
the dev launcher; no private file was read/changed. Configuration is not authority
for egress, paid calls or live operation. Model/endpoint, permitted utterance/history
data, retention/region/terms, credential custody and monetary caps remain unresolved.

Requests disable thinking and leave max-output tokens unset, so the configured
provider/model default applies. Actual output length and cost are variable/unknown,
not guaranteed unlimited, shorter or cheaper. Legacy DEEPSEEK_MAX_TOKENS is not
mapped, and no replacement numeric cap is imposed. Requests still use one attempt/
no retry and retain connect/read/caller deadlines of 2/8/10 seconds. One active model request and
60 attempts per process-local one-hour window bound workload, not money; restart resets the
count and cancellation cannot prove an uncharged request. No token/spend accounting
or persistent quota exists. Spring AI logging is disabled because upstream errors
may include prompts; do not enable body/prompt/tool-content logging or tracing.

## Build and safe smoke

From `apps/server`, with Java 25 and Maven 3.10.0 available:

```sh
mvn -B -ntp -Dmaven.repo.local=/tmp/longnest-bootstrap-m2 package
mvn -o -B -ntp -Dmaven.repo.local=/tmp/longnest-bootstrap-m2 spring-boot:test-run \
  -Dspring-boot.run.main-class=me.nghlong3004.longnest.TestLongnestApplication
```

The first command needs Maven repository access on a cold cache. `-o` runs offline
after resolution. No global Maven configuration or cache changes are required.
The smoke launcher uses only classpath configuration, ignores host environment/
system properties and command-line arguments, and closes its context before exit.
It does not import the existing `TestcontainersConfiguration` or launch containers.

`application-smoke.yaml` exists only on the test classpath, never in the application
JAR. It explicitly excludes DataSource, Hibernate JPA, JPA repositories and Flyway
auto-configuration for this no-database check, and turns keep-alive off. The launcher
checks Boot's managed `applicationTaskExecutor` actually executes a virtual thread.

The two JUnit tests verify:

- Successful non-web Spring context startup, managed virtual-thread execution and
  context closure.
- No DataSource, EntityManagerFactory, Flyway, chat model or mail sender beans in
  smoke; retained Hibernate `validate` and SQL initialization `never` settings.
- Normal startup without database configuration fails rather than reporting an
  empty or successfully initialized ledger. The expected failure is printed during
  the passing test: `Failed to determine a suitable driver class`.

No application/provider requests are made by these checks. This is bean/lifecycle
evidence, not a network packet capture or PostgreSQL integration test.

## Normal runtime boundary

For explicitly authorized host development startup, use the
[root `.env.dev` launcher](../../scripts/README.md). One command automatically runs
Maven package with tests, then launches the resulting JAR with the existing
`ledger` profile and required DataSource/personal-owner configuration. Unlike the
isolated smoke below, it can contact PostgreSQL and run Flyway; it has not been
executed against a database. Direct `java -jar` does not itself load `.env.dev`.

Normal configuration retains PostgreSQL/JPA and Flyway auto-configuration. Hibernate
validates rather than creates/updates schema, and SQL initialization is disabled.
No database URL or credentials are supplied. The new profile-specific migration
and ledger wiring below have not been executed against a database.
Normal runtime is non-web, enables managed virtual threads and keeps the JVM alive;
the unused DeepSeek chat model is disabled with `spring.ai.model.chat=none`, without
dummy credentials. No mail host is configured; Telegram remains disabled by default.

### Explicit first-use owner initialization

After an operator has separately provisioned an authorized PostgreSQL database,
run the packaged JAR once with `ledger-init`. This profile includes `ledger`, so
Flyway must migrate and Hibernate must validate the real schema; it does not
disable persistence or seed on ordinary startup. Supply these values privately
in the caller's environment (shown with placeholders only):

```text
SPRING_DATASOURCE_URL=jdbc:postgresql://<authorized-host>:5432/<database>
SPRING_DATASOURCE_USERNAME=<database-user>
SPRING_DATASOURCE_PASSWORD=<private-password>
LONGNEST_LEDGER_OWNER=<personal-owner-id>
```

Then, from `apps/server`, invoke:

```sh
java -jar target/longnest-0.0.1-SNAPSHOT.jar --spring.profiles.active=ledger-init
```

Direct `java -jar` does not load `.env.dev`; this command does not use or weaken
the existing development launcher. Initialization checks PostgreSQL `fsync=on`
and `synchronous_commit=on`, locks the ledger tables, and commits the generation-0
owner head only if **every** head, entry, create-inbox, control and control-event
table is empty. It closes the
application after the proxied transaction returns. A nonempty/missing schema,
missing credentials/owner, downgraded durability or failed/uncertain commit must
not be treated as success; inspect storage before any retry. Normal `ledger`
startup never initializes a missing head. `ledger-init` excludes the Telegram
SDK even if `LONGNEST_TELEGRAM_ENABLED=true` is inherited. The initializer is an
operator-only command, never a Telegram action or future model/tool capability.
No database or live bot run has been performed for this candidate.

## Opt-in Telegram SDK boundary

### Vietnamese presentation

The personal bot's user-facing text is Vietnamese. Spring Boot's standard
MessageSource auto-configuration loads UTF-8 `messages_vi.properties`, with
`messages.properties` supplying a deliberate generic Vietnamese root fallback.
UiMessages always requests locale `vi`; system-locale fallback is disabled.
Missing keys produce the generic unavailable message, not an internal key or user
argument. Coverage tests require all current enum/status/field and UI keys.

UiMessages lives in the independent presentation package and has no Telegram
dependency; TelegramReplies injects it for text/button rendering. Later server
adapters may reuse it or compatible keys. A browser cannot automatically consume
Java properties: no frontend/API, browser localization tooling or language selector
is implemented. Commands, callback tokens, IDs, machine error codes, financial
arithmetic and typed contracts stay unchanged. Exact decimal/ISO strings are passed
as placeholder arguments; no locale grouping or rounding. Plain-text Telegram
messages still have no Markdown/HTML parse mode, so user notes remain literal.

Callback acknowledgments are still text-free; expired/stale navigation still
refreshes the now-localized first page. No new callback notification or expiry
policy was invented. Operator logs/exceptions are not sent to users and remain
English/redacted. Project docs/code identifiers/coordination remain English.
See [Spring Boot internationalization](https://docs.spring.io/spring-boot/reference/features/internationalization.html).

Example presentation (synthetic ID/operation):

```text
Đã lưu
#42 · Phiên bản 1 · Còn hiệu lực
Chi tiêu: 45000 VND
Ngày: 2026-10-20 (Asia/Ho_Chi_Minh)
Danh mục: Ăn uống
Ghi chú: ăn trưa
Mã thao tác: tg:123:9
```

Browse labels: `Trang sau`, `Trang trước`, `Xem #42`. Replayed receipts explicitly
say `Kết quả đã lưu ban đầu (phát lại)`; unknown outcomes warn not to resend as a
new purchase. New VI-UI-1 evidence: focused 95 tests and fresh full package 396
tests, zero failures/errors/skips; standalone isolated smoke passed. Logs under
`/tmp/longnest-vi-ui.6ZG4fY/`; no live Telegram/PostgreSQL execution.

### SDK activation

The server binds TelegramBots `telegrambots-longpolling` and `telegrambots-client`
10.3.0 without its Boot starter. Only the explicit `ledger` profile plus
`LONGNEST_TELEGRAM_ENABLED=true` activate it. Set private
`LONGNEST_TELEGRAM_TOKEN` and numeric `LONGNEST_TELEGRAM_USER` in the caller's
environment or root `.env.dev` via the documented launcher; inherited environment
wins over file values. Missing/invalid values fail bean creation. Do not put a real token
in source, logs, command examples or tests. The default/smoke path creates no SDK
session and makes no Telegram request.

Enabled startup checks SDK bot identity and `getWebhookInfo` to refuse an existing
webhook, then drains frozen owner/bot-namespace creates in ten-item batches before SDK registration
(which automatically calls `deleteWebhook`). No webhook is changed by disabled
startup. SDK requests a 30-second long poll with a 35-second HTTP read timeout
and 40-second whole-call timeout. Polling uses one supplied platform scheduler thread; each batch is
handled synchronously, with ledger/service work on Boot's managed executor and
an explicit trusted `HostContext`, not a copied transaction. An undurable result
keeps the callback blocked for same-event retry, or stops the session on
interruption/fatal failure. The next SDK poll cannot acknowledge that batch while
unfinished. Startup recovery failure stops registration and requires operator
action; it does not promise an automatic retry. Shutdown cancels the session,
scheduler and owned HTTP resources.

Telegram keeps updates for at most 24 hours. Frozen ledger operations and their
receipts, not Telegram delivery, are authoritative for financial replay. A reply
can be delivered twice after a send failure/interruption, and a committed result
can lack a reply after the update expires; this adapter does not claim an
exactly-once or durable outbound-message queue. Use `/show ID` or `/recent` to
review current records. TG-SDK-2 is accepted for implementation and synthetic/local
behavior only: Lead's fresh package passed 332 tests and standalone isolated smoke
passed. Logs: `/tmp/longnest-tg-sdk-lead.E3WK5k/package.log` and `smoke.log`;
writer reports are preserved in that directory's `peer-target/`. Exact hashes,
paths and limits are in [CURRENT STATUS](../../docs/WORK_STATUS.md).
No real Telegram or PostgreSQL run is evidenced here. A second operator can race
the webhook preflight; only one process/operator may own the bot at a time.
Recent text supplies Next tokens and `/show ID`; L1-BROWSE-1 also supplies
authenticated Next/Previous and entry-selection buttons, as described below.
L2 controls and L3 range/summary controls are implemented with synthetic boundaries
as described below. L1–L3 and R1–R17 are not accepted end to end.

### L2 review, mutation and restore (synthetic-only)

`/show ID`, a selected entry or a fresh receipt opens an entry with Sửa (Edit),
Xóa (Delete), or Khôi phục (Restore). Edit chooses amount, income/expense type,
category, note or financial Date. The displayed prompt names the selected field,
current proposed value and full proposed state; replies must target that exact
bot message. Type/category have Vietnamese-labeled buttons. An invalid value
keeps the prompt; Xóa ghi chú clears the note. A second field from an unsaved
preview carries its entire validated afterstate (for example 45k→40k and
Date→2026-10-19) and replaces the old Save action. Direct typed
`TransactionService.previewUpdate` accepts multiple sparse fields in one patch.
An ordinary unbound clear purchase still creates immediately.

Save uses only the immutable persisted preview and a signed host-only proof
created after private numeric SDK callback authentication. An opaque action ID
or user/model text saying `confirmed=true` is insufficient. `/delete ID` and
Xóa display exact target details but do not delete; `/delete ID` freezes its
target revision before display so retrying that command reuses the same review.
Only the displayed Xác nhận xóa callback can commit. A deleted view/receipt
offers one durable display-bound Khôi phục button; its first click restores and
later clicks, even with different callback IDs or after restart, replay the
original receipt. There is no `/restore ID` command or second Save gate. The
reusable `TransactionService.restore(context,id,revision)` remains available
to a future authenticated typed adapter with stable operation identity.
Active/current-revision Restore and unchanged Update return
`changed=false` without advancing revision/generation. Changed mutations
advance both once, never purge an entry, and replay the original receipt even
after later changes. `/show ID` remains the current-state read.

Flyway V2 adds retained control and callback-event bindings. Prompts/actions
expire after ten minutes, with at most 100 live controls per owner; expired,
replaced, canceled and completed bindings/results are retained to prevent old
reply fallthrough after restart. Owner-head serialization covers control
preparation/consumption and entry/head mutation in short service transactions;
no DB transaction spans a Telegram wait. The global host operation identity
conflicts if reused with a changed scope, kind, target or patch, including
create versus a later control. Different SDK callback delivery IDs on one
action recover the same original result. An unbound/uncertain outgoing display
cannot authorize Save; users may need to reopen the review. Telegram replies
can duplicate, and no exactly-once outbound delivery is promised.

The V2 migration/JPA behavior, commit/reconnect durability, actual private-chat
ergonomics, backup restore and live PostgreSQL/Telegram are **unverified**;
synthetic service/SDK tests do not establish those outcomes. The operator-only
`ledger-init` command is not an L2 control or future tool.

### Inclusive range review (synthetic-only)

All four commands require both strict real dates; equal endpoints mean one day:

```text
/overview 2026-10-01 2026-10-31
/income 2026-10-01 2026-10-31
/expenses 2026-10-01 2026-10-31
/transactions 2026-10-01 2026-10-31
```

Overview shows income/expense totals and counts plus net flow, not entries or a
bank balance. Type views show that type's full-range total/count and selectable
entries; transactions shows the all-type summary and list. Every list has at most
ten rows with Next/Previous; selecting a row re-gets its current state and L2
controls. Vietnamese replies echo inclusive dates, VND and Asia/Ho_Chi_Minh.
Invalid/missing/reversed/out-of-bounds ranges clarify without swapping/defaulting.
Supported dates remain 1900-01-01 through 9999-12-31, including future labels.

LedgerQueryService.aggregate/list authorize and lock the established owner head
before generation, full-range aggregates and keyset rows in one service unit.
Only owned active entries selected by financial date contribute. Range order is
date/ID descending; recent retains creation-time/ID descending. Native PostgreSQL
SUM(BIGINT) and numeric-cast counts project to BigDecimal, converted exactly to
BigInteger/canonical strings; totals never narrow to long/double or page subtotal.
The new domain/DTO/store values are records with explicit validation, not Lombok
builders that bypass bounds. No dependency or migration was added for L3.

One LedgerCursors helper replaces RecentCursors for both scopes: opaque 128-bit
tokens bind owner/range/filter/generation, expire at ten minutes, max 100 with
oldest eviction. TelegramNavigation retains bounded Previous history, chat/message
bindings and only IDs/revisions, never financial snapshots. Range buttons also
carry compact dates/type and base-36 selection IDs (public IDs stay decimal),
within 64 bytes even at maximum ID/date. These are **untrusted read-query metadata**,
not financial proofs. After expiry/eviction/restart they can refresh the same
authorized range/type's first page instead of silently switching to recent.
Known copied-message or changed-query controls are rejected. Changed generation
also refreshes page one and clears history; no historical snapshot is promised.

Synthetic checks exercise actual query service and SDK routing over port state,
not PostgreSQL query execution/isolation or real Telegram UX. Runtime Flyway/JPA,
SQL aggregate/locking behavior, process durability and live interaction evidence
remain outstanding in the sole WORK_STATUS; do not activate a real launcher/init
or Telegram session from these examples without separate operational scope.

### Recent browsing

`/recent` → **Next** → **Previous** → **Show #42** retrieves the current complete
record using the existing get service. The first page has no Previous button,
the last has no Next, and an empty first page has no dead controls. Each navigation
step sends a fresh page message; older buttons retain their own immutable route,
so repeated Next never skips another page. `/recent <token>` and `/show ID` still
work; direct-token navigation without prior history returns to page one on Previous.

The SDK requests both message and callback_query updates. Callback sender, non-bot
identity and private chat are authenticated before querying; opaque page tokens
also bind the original sent-message ID. Copied-message controls are ignored;
unknown/tampered, expired, evicted or restarted page state refreshes page one.
Service INVALID_CURSOR/STALE_CURSOR also refreshes with cleared history. Selection
is restricted to displayed IDs and calls get again, never cached record contents.

One `TelegramNavigation` class encapsulates at most 100 page contexts with
10-minute expiry and at most 100 prior cursors per context. Only IDs and cursor
history are retained, not financial views or mutation proof. When prior history
is unavailable, Previous returns to page one. No formal design pattern or command
hierarchy was necessary: immutable routes provide repeat-click behavior directly.
No schema, dependency or financial-service change was needed.

Authorized callbacks attempt SDK AnswerCallbackQuery before handling; an old query
that cannot be answered does not strand read-only processing. Uncertain page sends
remain unbound rather than authorizing controls from an unconfirmed response.
This follows the [Bot API callback constraints](https://core.telegram.org/bots/api#callbackquery)
and [button data limit](https://core.telegram.org/bots/api#inlinekeyboardbutton),
checked against the actual installed SDK APIs, not a new transport implementation.
Delivery can duplicate as before; there is no exactly-once outgoing UI claim.

New local evidence: full fresh package **353 tests**, zero failures/errors/skips;
21 new synthetic browse cases, plus the standalone isolated smoke. Logs:
`/tmp/longnest-browse.66h2pn/package.log` and `smoke.log`; final reports are in
`target/surefire-reports/`. The initial focused run passed 50 tests before two
additional cases were added. No live Telegram/DB or private env was used; actual
chat ergonomics, database commits/reopen and full first-release CRUD remain unverified.

The following safe negative check isolates the process from inherited credentials
and external Spring configuration. It is expected to exit **1** for a missing
DataSource URL (use the installed Java 25 executable):

```sh
env -i PATH=/usr/bin:/bin /absolute/path/to/java25/bin/java \
  -jar target/longnest-0.0.1-SNAPSHOT.jar \
  --spring.config.location=classpath:/application.yaml --spring.main.keep-alive=false
```

Do not enable integrations, supply real credentials or run the dormant
`TestcontainersConfiguration` as part of this smoke. That unchanged starter helper
still names `postgres:latest`; pin/review it in a separately authorized PostgreSQL
integration assignment before use. A supplied live database configuration can cause
normal Flyway/JPA startup to contact that database; this smoke authorizes no such run.

## Recorded bootstrap verification

Verified on Linux/WSL2 amd64, Temurin 25.0.4.1 and Maven 3.10.0:

- `package`: exit 0, executable JAR produced; 2 tests, 0 failures/errors/skips.
- Offline `spring-boot:test-run` above: exit 0, virtual-thread check passed and JVM
  exited cleanly.
- Isolated normal executable JAR command above: expected exit 1, missing DataSource
  URL, with Flyway and Hibernate still in the initialization path.
- JAR inspection: only `BOOT-INF/classes/application.yaml`; smoke configuration and
  test launchers are not packaged.

Generated test evidence is under `target/surefire-reports/` (text summary and XML
with startup/failure output). The prior generated target, including bootstrap
process logs, is preserved at
`/tmp/longnest-package.pRbKq9/target-preserved/`.
Build output, logs and JAR are disposable generated files.
An initial sandboxed build failed on Maven Central DNS; permission-granted
dependency resolution succeeded. One intermediate test compilation failed on a
Boot 4 web-context import and was corrected. Downloads emitted nonfatal 401
metadata warnings from transitive Flyway repositories; compilation emitted a
Lombok `sun.misc.Unsafe` deprecation warning. Neither required a dependency change.
An attempted RTK Maven summary selected the unchanged non-executable `mvnw`
(mode 0644) and failed before running Maven; direct `mvn` was used instead, without
changing wrapper permissions.

Actual PostgreSQL migrations, schema validation, persistence, concurrency, restart
and recovery remain **blocked/unverified**: no authorized isolated PostgreSQL
instance is available and Docker socket access is unavailable. No H2 fallback,
installation, container, live messaging, model call or deployment was used.

Framework references checked for this bootstrap:
[Boot requirements](https://docs.spring.io/spring-boot/system-requirements.html),
[managed task execution](https://docs.spring.io/spring-boot/reference/features/task-execution-and-scheduling.html),
[database initialization](https://docs.spring.io/spring-boot/how-to/data-initialization.html),
[DeepSeek model disable property](https://docs.spring.io/spring-ai/reference/api/chat/deepseek-chat.html).

## Pure input/domain increment

`domain` contains immutable `Money`, `Note`, `TransactionType`, `Category` and
`TransactionDraft` values. `Money` is a positive whole-dong magnitude within
1..10^12 VND, parsed exactly without floating point or rounding. Canonical amounts
have no leading zeroes. Draft construction requires every value and rejects
incompatible type/category combinations. Structured notes allow digits and
punctuation, normalize to NFC and enforce a 256-code-point canonical note limit.

`input.TransactionInterpreter` is a stateless, framework-free finite-grammar
interpreter. Call `interpret(text)` to get either `InterpretationResult.Ready`
with a validated draft, or `Problem` with a non-ready status, canonical reason
code, affected field and safe resubmission example. Ready is not saved: no date,
owner, persistent ID, receipt, authorization or replay behavior is invented.
Host date/authentication and persistence are supplied separately by the ledger/
Telegram boundary, not these pure classes. These are internal values, not an HTTP
DTO/service contract.

The interpreter enforces raw message length before NFC/whitespace normalization,
rejects controls/line breaks/semicolons, preserves normalized phrase/description
wording as the note and uses only the 20 explicit phrase aliases. Signed cue
matching uses Unicode letter/mark/number/underscore word boundaries; a category is
reused only for a complete known description. Refund/debt/transfer and the listed
relative-date cues are unsupported, not guessed. Unlisted date language remains
ordinary note text, never a parsed date.

At the amount/description boundary only, a standalone currency code recognized by
the bound JDK's `java.util.Currency`, or a currency-symbol token, cannot become a
note. Whole integers may consume one `đ`/`vnd` label; suffixed amounts cannot.
`-45k USD đồ dùng` and `-45k vnd đồ dùng` reject; `-45000 vnd đồ dùng` is valid.
Later ordinary note acronyms such as `-45k sách USD` remain literal note text.
This is finite lexical handling, not general currency/language recognition.

Reproduce on the bootstrap toolchain/cache, from `apps/server`:

```sh
mvn -o -B -ntp -Dmaven.repo.local=/tmp/longnest-bootstrap-m2 \
  -Dtest=DomainValuesTests,TransactionInterpreterTests test
mvn -o -B -ntp -Dmaven.repo.local=/tmp/longnest-bootstrap-m2 package
```

Focused verification covers 240 new parameterized/unit tests, including all aliases
and both sign directions, canonical examples, exact amount bounds, decimal/grouping/
scientific/Unicode lookalikes, unsupported cues and word-boundary false positives,
all JDK currency codes at the boundary, raw versus NFC-expanded length and controls.
One test also exercises 2,000 seeded synthetic Unicode strings. The L1-INPUT-2 package
passed 242 tests including the two unchanged bootstrap tests; no failures/errors/
skips. Evidence is in `target/surefire-reports/` for `DomainValuesTests`,
`TransactionInterpreterTests` and `LongnestApplicationTests`; generated final build
output is retained in
`/tmp/longnest-package.pRbKq9/target-preserved/l1-input-2-package.log`.

L1-INPUT-2 repairs two reproduced inspection defects: the shared sign predicate
now rejects U+FB29 (Hebrew alternative plus) without NFKC expansion, and supported
decimal-suffix syntax with zero/out-of-bounds magnitude returns
`REJECTED/INVALID_AMOUNT`, not ambiguity. Grouping (`45.000`, `45,000`) and excess
precision (`1.234tr`) still return `NEEDS_CLARIFICATION/AMBIGUOUS_AMOUNT`.
The nine new regression fixtures failed before the repair (focused test exit 1,
169 tests/9 failures); evidence is in
`/tmp/longnest-package.pRbKq9/target-preserved/l1-input-2-regressions-before.log`.

That input increment added no dependencies, bootstrap changes, database, transport
or service framework. It verifies only pure input/domain
behavior and preserved bootstrap checks, not ledger R1–R17 or end-to-end L1.
PostgreSQL integration remains blocked as recorded above.

## L1 store wiring — code and local unit verification only

### Ledger package layout

Ledger code is organized by feature responsibility under
`me.nghlong3004.longnest.ledger`: `model` holds validated intent, host context,
owner policy and business exceptions; `model.entity` holds JPA entities and
embeddables; `dto` holds caller inputs, views, pages, receipts and tagged results;
`service` and `service.impl` hold application contracts and transactional
implementations; `repository` contains the store port, Spring Data repositories
and JPA adapter; `mapper` translates caller/domain/persistence representations;
`pagination` owns recent cursors; `config` provides explicit-profile beans; and
`adapter` contains the non-network caller. Reusable `domain` and deterministic
`input` remain separate and unchanged.

New runtime components require the explicit `ledger` profile and a nonblank
`longnest.ledger.owner` property (no default identity). This is not permission to
activate that profile against any database. No database, migration, container or
real message has been executed for this increment. Parser/domain/bootstrap source
remains unchanged. The later MapStruct increment adds the missing API dependency.

### MapStruct representation mapping

The starter already configured `mapstruct-processor` and `lombok-mapstruct-binding`
but omitted the `org.mapstruct:mapstruct` API dependency. Earlier store work froze
the POM and used a handwritten mapper, as its original comment explained; no
incompatibility was demonstrated. MAPPING-1 adds that API at `${mapstruct.version}`
(1.6.3), preserving both compile/test processor configurations without duplication.

`TransactionMapper` constructor-injects the Spring-generated
`TransactionRepresentationMapper`. MapStruct constructs input/view/page/receipt
records with unmapped-target errors. Exact amount expressions use Money's canonical
integer string; generated ID/revision/generation conversions use exact long strings.
Explicit Optional and ACTIVE/DELETED expressions preserve representation semantics.
Validation/error translation, captured Vietnam business dates, NFC/value factories,
framed fingerprints, microsecond normalization and original-receipt reconstruction
remain handwritten helpers: these are financial/provenance rules, not bean copying.
Authorization and commit ownership stay in the existing services/caller.

Both facade and generated implementation are `ledger`-profile gated. MapStruct's
`@AnnotateWith` emits the implementation's `@Profile`; it is not assumed inherited
from an interface. Inspect generated code at
`target/generated-sources/annotations/me/nghlong3004/longnest/ledger/mapper/TransactionRepresentationMapperImpl.java`.
Never hand-edit or commit generated output.

Focused no-DB checks (after dependency resolution):

```sh
mvn -o -B -ntp -Dmaven.repo.local=/tmp/longnest-bootstrap-m2 \
  -Dtest=TransactionMapperTests test
```

These six cases cover exact bounds/all fields, absent source time, deleted state,
original replay, immutable bounded pages, and real Spring scanning/injection with
active/inactive profiles. Existing service/proxy tests use the actual generated
mapper, not a mock. Current full-build, smoke and expected no-DB failure logs are
`target/mapping-1-package.log`, `target/mapping-1-smoke.log` and
`target/mapping-1-normal-no-db.log`. PACKAGE-1A generated evidence formerly at
`target/` is preserved at `/tmp/longnest-mapstruct-base.FQjobd/target-before-mapping/`;
older archives below remain unchanged. Exact MAPPING-1 candidate/results/limits
are recorded in [CURRENT STATUS](../../docs/WORK_STATUS.md).

The non-network `ledger.adapter.LedgerClient` is the local caller boundary for `create`,
`get` and `recent`. It authorizes before opening transactional service calls and
returns exactly `LedgerResult.Ok<T>` or a sanitized `Failure<T>`. Host identity,
operation ID and source instants live in `HostContext`, never `TransactionInput`.
Request optionals must be non-null `Optional` containers: empty is absent, not an
explicit-null field. `TransactionMapper.input(readyDraft)` adapts an accepted
interpreter draft; service validation independently enforces canonical VND,
type/category/note and strict 1900..9999 dates for structured callers.

The host must capture `businessReference` once from authenticated message time (or
received time when absent), and supply the original stable operation identity.
Default financial date uses Asia/Ho_Chi_Minh, not commit date/host timezone.
For a future authorized synthetic invocation, dispatch the local client through
Boot's managed `applicationTaskExecutor`, passing `HostContext` explicitly; service
transactions remain on that execution thread. The ledger package itself has no
transport worker; the opt-in Telegram lifecycle above owns that execution bridge.

`TransactionService.ingest(context, input)` and `create(context)` are separate,
proxied READ_COMMITTED units. Each owns its physical transaction (`REQUIRES_NEW`);
the local client refuses an already-active caller transaction rather than suspending
its locks. Ingestion locks the established owner head, validates/freezes normalized
intent/reference and a framed SHA-256 fingerprint in a durable inbox. A redelivery
uses the frozen reference before applying omitted-date defaults. A different valid
payload under the same identity conflicts; equal distinct events remain independent.

Create locks the head before reading the inbox, then either recovers the original
receipt or inserts one entry, advances generation and completes the inbox/receipt
in **one** transaction. There is no separate financial/receipt commit. Pending
recovery can call the proxied `TransactionService.create` with trusted principal
and operation ID and null source references; only frozen inbox data is used.
Replay reconstructs the original revision-1 ACTIVE view from frozen intent and
receipt fields, never a current-entry lookup. The adapter returns success only
after the service proxy commits. Known rollback failures map to NOT_SAVED;
unclassified mutation/commit failures conservatively map to OUTCOME_UNKNOWN with
the same operation ID. Never retry uncertainty with a fresh operation identity.

`LedgerQueryService` authorizes and takes the same short head lock before reads.
Get includes owned active/deleted state; foreign/missing IDs are generic. Recent
selects eleven rows to return at most ten active records, ordered by saved
createdAt/ID descending. The last visible key makes the next 128-bit opaque token.
Tokens are in memory, recent-scope/owner/generation-bound, expire at ten minutes,
and cap at 100 (oldest evicted). Restart/tampering/expiry gives INVALID_CURSOR;
generation changes give STALE_CURSOR. The caller refreshes page one on either.
No managed entity or Spring Data pagination type leaves the persistence adapter.

Actual Spring Data repositories/JPA entities and `JpaLedgerStore` are supplied.
The profile points Flyway to additive V1 and V2 migrations under `db/ledger`; the normal
Hibernate `validate` and SQL-init `never` settings are unchanged. SQL owns sequences,
keys, constraints and the recent partial index; JPA mirrors them. Provider row
versions are separate from business revision/generation. Saved source/creation
instants are truncated to microseconds before persistence/cursor use. The creation
instant is captured in the server write unit, not a measured PostgreSQL commit time.

There is deliberately **no owner-head seed or automatic recreation**. An operator
or separately authorized isolated fixture must explicitly establish the head with
generation zero on first initialization. Missing head always fails, never an empty
ledger. The adapter also fails if the database reports fsync or synchronous_commit
other than `on`; it does not change server settings. Actual lock SQL, settings,
constraints and migration compatibility still need PostgreSQL execution evidence.
Technical host identifiers are bounded to 200 Java string units, never truncated.
L2 mutation code and L3 ranges are described above. The Telegram
authorization adapter exists, but no live API/model/bot execution is established.

### Reproduce the local checks

```sh
cd apps/server
mvn -o -B -ntp -Dmaven.repo.local=/tmp/longnest-bootstrap-m2 package
```

The package refactor was verified from an absent build-output directory because
the offline cache has no Maven Clean Plugin. The pre-existing generated `target`
was preserved at `/tmp/longnest-package.pRbKq9/target-preserved`; the package
command then compiled into a newly created `target`. To reproduce a fresh build
while retaining any existing generated evidence, run from `apps/server`:

```sh
package_archive=$(mktemp -d /tmp/longnest-package.XXXXXX)
if test -d target; then mv target "$package_archive/target-preserved"; fi
mvn -o -B -ntp -Dmaven.repo.local=/tmp/longnest-bootstrap-m2 package
mvn -o -B -ntp -Dmaven.repo.local=/tmp/longnest-bootstrap-m2 spring-boot:test-run \
  -Dspring-boot.run.main-class=me.nghlong3004.longnest.TestLongnestApplication
```

Historical `target/` evidence references elsewhere in this README now resolve
under `/tmp/longnest-package.pRbKq9/target-preserved/`. Lead's independent refactor
verification preserved the submitted generated output separately at
`/tmp/longnest-package-lead.Xsol9U/peer-target/`, then rebuilt into an absent
`apps/server/target`. Current package/smoke/no-DB logs are
`target/package-1a-package.log`, `target/package-1a-smoke.log` and
`target/package-1a-normal-no-db.log`; the exact source snapshot and disposition
belong in [CURRENT STATUS](../../docs/WORK_STATUS.md).

Historical package-refactor environment: Temurin 25.0.4.1, Maven 3.10.0,
Linux/WSL2 amd64. That earlier package passed 291 tests, no failures/errors/skips:
all 242 prior checks plus 49 new checks. For the package refactor, that result came from a newly created `target` after
the prior generated directory was preserved outside the project. Its exact
candidate manifest is recorded in the handoff.
New evidence consists of real mapper/validator/service/cursor/entity code with
mocked persistence, metadata/SQL-source assertions, and real Spring proxy mechanics
with a test-only transaction manager. **None proves PostgreSQL transactions,
row locks, query execution or durable replay.** No database test was skipped or
counted as a pass, and no database test/container fixture was launched.

The package contains the moved feature packages and only `application.yaml` plus
`application-ledger.yaml` under `BOOT-INF/classes`; it contains no former root or
`ledger.persistence` class paths, test launcher or smoke configuration. The
documented standalone no-database smoke exited 0 and reported managed
virtual-thread execution. The isolated normal JAR start exited 1 as expected for
the missing DataSource URL (`Failed to determine a suitable driver class`), with
Flyway/Hibernate startup retained. These checks do not activate the `ledger`
profile or contact a database.

Reports: `target/surefire-reports/` (`LedgerUnitTests`, `LedgerProxyTests`,
`RecentCursorsTests`, `LedgerConfigurationTests`, `JpaLedgerStoreUnitTests`), and
`/tmp/longnest-package.pRbKq9/target-preserved/l1-store-package.log`. An initial focused run failed with 27 mock-maker
initialization errors because JVM agent attachment was unavailable; retained in
`/tmp/longnest-package.pRbKq9/target-preserved/l1-store-mockito-before.txt`. A test-only `mock-maker-subclass` resource
resolves the errors without POM/host permissions changes. Mockito still emits a
self-attachment warning; inherited Lombok Unsafe warnings remain. An initial
compile/package-only check skipped tests and is not used as behavioral proof.

Unrun PostgreSQL checkpoints: authorized isolated UTF8 instance and connection
scope; Flyway fresh migration plus explicit fixture head initialization; Hibernate
validation; actual owner locks/timeouts; rollback/constraint/concurrent same-op and
distinct-op commits; uncertain-commit same-ID reconciliation; process reopen and
pending-inbox/receipt replay; saved-value keyset pagination; backup/restore. These
remain blocked, not product passes. Live authentication/Telegram remain unverified;
L2/L3 remain synthetic-only. Existing dormant Testcontainers helper must not be used
as approval.

References checked for the implementation (not runtime proof):
[transaction propagation](https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/tx-propagation.html),
[Spring Data locking](https://docs.spring.io/spring-data/jpa/reference/jpa/locking.html),
[PostgreSQL NFC/character functions](https://www.postgresql.org/docs/18/functions-string.html).
