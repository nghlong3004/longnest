# OOP application services and future tool contracts

Status: **selected release-one logical design; bounded create/get/recent local code
accepted, PostgreSQL runtime unverified**; exact evidence and limits are in
[CURRENT STATUS](WORK_STATUS.md). The opt-in Telegram SDK capture/get/recent and
pending-create recovery and recent Next/Previous/selection implementation has its
own bounded disposition there. Edit prompts/previews, authenticated Save/Cancel,
confirmed Delete and single-click Restore now have synthetic/local code acceptance;
inclusive range methods/navigation now also have synthetic/local code acceptance.
No durable or live acceptance follows from this code status. Lead technical
decisions complete the approved product direction; future finance extensions and
LLM activation remain proposals. [Domain](DOMAIN.md) owns input/money/date semantics;
[Decisions](DECISIONS.md) distinguishes Human approvals, Lead choices and proposals.
[Backend rules](BACKEND_RULES.md) is the mandatory Java/Spring mapping of this
logical design: service interfaces/implementations, DTOs, JPA/Flyway and execution.

## Both UX and service contracts

Telegram is an adapter rendering controls/results, not the financial business layer.
Use two cohesive application services; typed command/query records need no class-
per-command framework, command bus, generic repository hierarchy or tool SDK.

| Responsibility | Selected object / boundary |
| --- | --- |
| Transaction lifecycle | Transaction: stable ID/owner, income or expense, Money, category/note, financial date, immutable createdAt/messageAt, revision and active/deleted state. No Telegram/model dependencies. |
| Exact values | Money and DateRange enforce [Domain](DOMAIN.md) bounds, precision and inclusive ranges; sums are exact, net may be negative. |
| Commands | TransactionService.create, previewUpdate, update, requestDelete, confirmDelete, restore, cancel. Authorize every call; validate values, state, revision and proof; return business results. |
| Queries | LedgerQueryService.get, recent, aggregate, list. Owned reads with common active-state semantics; lists include a matching full-range summary. |
| Persistence | LedgerStore persistence port implemented by a Spring Data JPA/Hibernate adapter over approved PostgreSQL. Service-owned unit-of-work and coherent-read boundaries, no generic SQL/model-owned store tool. |
| Transport/interpretation | Deterministic interpreter now; Telegram authenticates input, supplies trusted context, maintains bounded edit/navigation state and formats results. Future interpreters produce the same typed requests. |

## Typed inputs and results

These are canonical **logical** version-1 payloads, not Java code, installed tools
or a published HTTP API. IDs, revisions, generations, money and counts use canonical
decimal strings at future JSON boundaries; positive ID/revision values are <=
9223372036854775807. Generation starts at 0 and advances on a changed transaction.
Reject overflow rather than wrap. Optional means absent; null/unknown fields/enums
are invalid. Date/amount/category/note limits are canonical in Domain.

| Payload | Required input / defaults |
| --- | --- |
| ApplicationContext (host only) | Authenticated principal, opaque operationId for commands, stable businessReference instant/date/timezone and opaque interactionScope. Host captures reference once from authenticated message time or received time if absent. Models cannot set actor, operation identity, time authority or proof. |
| TransactionInput | type, amountMinor, currency=VND; optional transactionDate defaults to captured business date, category to uncategorized, note to empty. No inferred amount/type. |
| TransactionPatch | At least one present allowlisted amountMinor/type/category/note/transactionDate field, validated as a complete after-state. Missing fields stay unchanged; empty note clears it. Domain defines visible category reset on incompatible type-only edits. No owner/ID/createdAt/messageAt patch. |
| PreviewUpdate | id, expectedRevision, patch. Stores immutable validated before/after state and returns an update action, not a financial mutation. |
| CommitUpdate / ConfirmDelete (host only) | actionId and trustedUserProof of the actual authenticated Save/Confirm interaction. Commit accepts no replacement patch/target. |
| RequestDelete / Restore | id and expectedRevision. Delete requests a preview; Restore is explicit user intent on a retained owned deleted record. |
| Get / Recent | get(id); recent(cursor?) with at most 10 active entries, ordered createdAt descending then ID descending. |
| Range queries | aggregate(range, filter); list(range, filter, cursor?). Both endpoints required; all/income/expense or FOOD (expense + Category.FOOD), default all. Range lists order financial date descending then ID descending. |

Use a tagged result: status=ok with resultKind/data; needs_clarification with code/
fields and supported resubmission example; or error with code/field?/retryable/
operationId?. No simultaneous success/error, unvalidated intent or free-form prose
may establish mutation success.

| Business result | Canonical information |
| --- | --- |
| TransactionView / DeletedTransactionView | ID, revision, type, exact amount/currency, category/note, transactionDate, createdAt, messageAt when an authenticated source timestamp exists, and state; deleted view offers Restore. No foreign identity or action proof. |
| MutationReceipt | operationId, operation create/update/delete/restore, changed boolean, resulting transaction view and ledger generation. Returned only after durable result commit. Replay is labeled original outcome, not a claim this old revision is still current; get retrieves current state. |
| ConfirmationRequired | actionId, kind update/delete, before view, after view for update, expectedRevision, expiresAt; application-owned immutable displayed content. No financial mutation. |
| Overview / TypeTotal | Echoed range/filter, VND, generation, income/expense totals and counts/net for overview; selected-type total/count for TypeTotal. Empty valid range is successful zero results. |
| TransactionPage | Scope recent or range/filter, entries, pageSize=10, generation and nextCursor when present. Range pages also include Overview/TypeTotal summary from the **same read snapshot**, never page subtotal. Recent omits summary; Telegram maintains Previous cursor history. |
| ActionCanceled | actionId and canceled state; no financial change. Cancel after a completed commit returns ACTION_COMPLETED, never rolls it back. |

Error codes: parser missing/conflicting/unsupported input are MISSING_AMOUNT,
MISSING_TYPE, AMBIGUOUS_AMOUNT, CONFLICTING_TYPE, UNKNOWN_PHRASE, UNSUPPORTED_INPUT. Service errors
are INVALID_AMOUNT, INVALID_DATE, INVALID_RANGE, INVALID_FIELD, NOT_FOUND_OR_FORBIDDEN,
INVALID_STATE, REVISION_CONFLICT, CONFIRMATION_REQUIRED, ACTION_EXPIRED,
ACTION_CANCELED, ACTION_COMPLETED, INVALID_ACTION_PROOF, INVALID_CURSOR, STALE_CURSOR,
IDEMPOTENCY_CONFLICT, LIMIT_REACHED, NOT_SAVED and OUTCOME_UNKNOWN.
Foreign/missing IDs/actions are indistinguishable. Invalid input/state/proof is
not retryable unchanged; stale revision/cursor needs fresh view. A confirmed
rollback is NOT_SAVED. An uncertain commit is OUTCOME_UNKNOWN: recover/retry the
**same operation**, never silently generate another purchase. Infrastructure failure
never becomes a successful model-narrated receipt.

## Action lifecycle and replay

**Selected PE1:** get/select #42 revision 1 → Edit → choose field → send new value
→ previewUpdate validates the entire after-state → displayed immutable before/
after → actual user Save → update(actionId, proof) commits exactly that after-state.
The adapter's edit-field context binds owner/interaction/ID/revision/field to the
displayed reply prompt for 10 minutes, persisted as control state with the inbox.
A reply to that prompt is field data, never an unrelated create; an expired/stale
edit reply asks for a fresh Edit and cannot fall through to purchase creation.
Invalid value stays in that explicit field prompt; Cancel exits without a financial
write. Unbound ordinary text uses transaction grammar; explicit slash controls are
routed before field interpretation. Multiple field changes create a replacement
preview for the same owned target; only the newest update preview can be saved.

Pending update/delete action uses a cryptographically random 128-bit opaque token,
owner/interaction scope, target/revision, kind, immutable payload and expiry
10 minutes after creation. Save/Confirm proof must match the authenticated actor,
interaction and displayed action; possession of a token or model confirmed=true is
not proof. Telegram callback tokens can carry kind plus this short token; large
patches never ride callback text. Model tools cannot invoke commit/confirm/cancel.

Cancel marks a pending action canceled; repeated Cancel is unchanged. Expiry,
replacement or stale target changes nothing. Another target revision invalidates
old previews, including stale Delete. Successful commit consumes the action and
stores its receipt atomically; repeated clicks return that receipt even after
expiry, following authorization, rather than applying it again. Pending actions/
consumed outcomes survive restart; expired pending actions require a fresh review.

Create starts revision 1; changed update/delete/restore increments revision and
owner ledger generation once. No-op update returns changed=false and consumes the
preview without bumping revision/generation. New requestDelete on deleted state is
INVALID_STATE; get still returns Restore. Restore on already active state with
current revision returns changed=false; stale expectedRevision still conflicts.
No permanent-purge job or event sourcing is needed/authorized.

**Selected PE2:** identical **event/action identity** replays its original outcome;
two distinct clear user messages are two purchases, even equal amounts/wording.
No semantic look-alike prompt or five-minute suppression. Adding one later would
change fast capture and needs a product decision, not a technical repair.

The Telegram adapter maps bot namespace/authenticated source event to stable
operationId; an action's durable identity makes repeated callbacks one completion
even when delivery IDs differ. Capture business reference and normalized ready
intent for a mutation in a small durable inbox before dispatch; retries reuse them
rather than reinterpret/default the date again. No permanent raw-message archive.
Unique (principal, operationId) binds operation kind/target and canonical payload
fingerprint; changed payload returns IDEMPOTENCY_CONFLICT. On each call authorize
first, then recover an existing outcome before checking an old expectedRevision.
Concurrent same-operation execution returns one receipt; no model tool_call_id can
mint a fresh mutation identity. Failed interpretation/clarification writes no
financial transaction. Corrections are new explicit actions, not edited Telegram
messages or replacement payloads for an old event.

The bounded capture binding uses TelegramBots longpolling/client 10.3.0, without
its Spring starter. Its synchronous SDK consumer waits for managed application
handling before another poll can confirm the batch. The SDK's early internal
update marker is not a durable application acknowledgment. Interrupted/failed
handoff stops or retries the same event; startup recovers frozen creates in the
authenticated owner's bot namespace. There is no exactly-once outbound queue:
reply delivery can duplicate or be absent after a committed result. Actual
PostgreSQL replay/reopen and Telegram execution remain unverified. See the
[server configuration](../apps/server/README.md)
and CURRENT STATUS for the implementation's current acceptance/limits.

Local L2 implementation stores controls and host-event bindings in additive V2
SQL (authored, not executed). Prompt and action display bindings are durable;
proofs are host-only opaque capabilities for the authenticated actor/scope/message/
action/target/revision. Save/Delete/Cancel are not model tools. Deleted views attach
a durable Restore action that executes on one actual click; the typed authorized
restore(id,expectedRevision) remains available independently. /delete ID freezes
the current revision once and recovers its original preview on retry. No extra
/restore command or second Restore confirmation is required. All runtime evidence
and the rejected/repaired candidate distinction stay in CURRENT STATUS.

## Persistence and query consistency

**Required persistence route:** Spring Data JPA/Hibernate over **PostgreSQL** for
development, integration tests and eventual operation. This replaces H2, not a
production-only variant; no H2 compatibility mode as repository/migration proof.
One bot-server instance initially, with a PostgreSQL database-server dependency;
host/location/version/credentials are unselected and no instance is provisioned.
No direct-JDBC primary repositories. Expected later bindings are pgJDBC
`org.postgresql:postgresql` and Flyway core plus its separate open-source
`org.flywaydb:flyway-database-postgresql` module. Official
[driver](https://jdbc.postgresql.org/documentation/use/) and
[Flyway PostgreSQL](https://documentation.red-gate.com/flyway/reference/database-driver-reference/postgresql-database)
docs support those bindings, not an installed Java 25/Boot/Hibernate/driver/Flyway
combination. Use the Boot-managed compatible dependency set when authorized;
do not copy DriverManager connection code into services.

**Version proposal, not selection:** supported PostgreSQL 18 with the current
minor at later binding time; official [version policy](https://www.postgresql.org/support/versioning/)
lists support through November 2030. Pin/recheck then; no new product permission
gate or permission to install/connect is implied. Network failure adds commit-
outcome uncertainty; preserve same-operation recovery, never guess success/repost.

TransactionServiceImpl owns proxied Spring transaction entrypoints; LedgerStore
and its Spring Data repositories participate, not a separate manual transaction
manager. LedgerQueryServiceImpl owns the read unit-of-work. No raw connections,
EntityManager, repository proxies or managed entities escape to interpreters or
transport DTOs. Map immutable results inside the unit-of-work; the adapter receives
success only after the transaction proxy commits. Do not hold transactions while
calling Telegram/LLM or waiting for human action. Configure rollback for any thrown
mutation failure; translate persistence/commit errors outside the failed boundary.

Logical durable records: transactions; owner head/ID sequence; operation
fingerprints/results; pending actions; minimal adapter inbox. Same database and
backup boundary, no second totals ledger. Inbox ingestion may commit before use
case dispatch; financial mutation, consumed action, completed inbox marker and
operation receipt must then commit together. Re-dispatch of a pending inbox uses
the frozen identity/reference/intent, never a fresh operation.

Flyway versioned SQL owns schema/PK/FK/index/unique/check/sequence definitions;
JPA mapping metadata mirrors them. Use Hibernate validation, never schema auto-
update/create/drop, including integration fixtures that claim migration evidence.
Initialize empty storage and its owner head only on explicit first initialization;
normal reopen fails if established storage/schema is missing or unsupported.
The bounded operator-only `ledger-init` profile now implements this procedure:
it includes ledger wiring, excludes Telegram even if enabled, checks durability,
locks/checks all ledger tables for emptiness, inserts a generation-zero head via
the proxied service, and closes the application. It is never a Telegram or future
model/tool capability. See the server README and CURRENT STATUS for the exact
candidate and synthetic-only evidence; no real database initialization is claimed.
No migration execution is authorized by this design. Applied migrations are not
rewritten; no automatic database creation/recreation/truncation. Missing database,
schema/head or failed authentication is unavailable/error, not empty-ledger success.
Use PostgreSQL SQL/types/constraints, not dual-engine scripts. Monetary amount is
BIGINT with the Domain positive bounds/check; aggregate projections must preserve
exact numeric totals and never narrow/float them (PostgreSQL SUM(BIGINT) is NUMERIC;
empty sums become exact zero). Use a NUMERIC projection within the JPA repository,
with a native query if JPQL typing would narrow it; convert the whole result
exactly to BigInteger, never long/double. DATE stores financial date; TIMESTAMPTZ stores
instants, with Asia/Ho_Chi_Minh applied explicitly by the domain. Normalize persisted
instants once to PostgreSQL microsecond precision; cursor keys use saved values,
not pre-persistence nanoseconds. See [aggregate types](https://www.postgresql.org/docs/current/functions-aggregate.html)
and [date/time types](https://www.postgresql.org/docs/current/datatype-datetime.html).
Map IDs/revisions/generation with bounded BIGINT semantics; use Flyway-owned PK/
sequence declarations consistent with JPA mapping. PostgreSQL sequences can have
rollback gaps: IDs need not be consecutive and committed IDs are never recycled.
See [numeric types](https://www.postgresql.org/docs/current/datatype-numeric.html)
and [sequence transaction behavior](https://www.postgresql.org/docs/current/transaction-iso.html).

Selected lock protocol for this small personal ledger:

1. Authorize, then take a short `PESSIMISTIC_WRITE` lock on the established owner
   head before loading mutable state. Consistent order: head, then target/action/
   operation records. All durable control and financial writers use this protocol.
2. Recover existing operation/action outcome before stale-revision checks; otherwise
   validate fingerprint, expected business revision, action/proof/state. A Flyway
   unique constraint on (owner, operationId) is the final race guard. A constraint/
   optimistic-lock/timeout failure rolls back; any recovery read is in a fresh
   transaction, never catch-and-continue in a rollback-only transaction.
   Confirmed stale state maps to REVISION_CONFLICT (refresh before another edit);
   a known rolled-back lock/constraint failure with no recovered outcome maps to
   NOT_SAVED. Commit uncertainty stays OUTCOME_UNKNOWN; retry the same operation.
3. Apply record + business revision + generation + action/result/inbox completion
   atomically. Provider-managed `@Version` rowVersion is an internal additional
   stale-write guard, **not** the public revision (which starts at 1). No manual
   provider-version increments. No-op receipts/control changes do not dirty the
   financial record or advance its business revision/generation.
4. Release at commit. Awaiting Save/Delete confirmation is durable pending state,
   not an open lock. Optimistic locking alone cannot make multiple action/result/
   head records atomic; the short head lock simplifies that consistency boundary.
   Do not add pessimistic locks to every entity or hold them across network work.

For coherent multi-query reads, retain PostgreSQL READ_COMMITTED plus that same
short head lock **before** loading generation, summary and page in a fresh unit-of-
work. PostgreSQL normally uses a new snapshot per statement; consistency here comes
from **all** relevant writers acquiring the same head, not magic default isolation.
`PESSIMISTIC_WRITE` must bind to a real blocking row lock (verify generated SQL,
timeouts and rollback); do not use SKIP LOCKED to hide contention or omit entries.
The read makes no durable financial/control change, but cannot be a database
read-only transaction because it locks FOR UPDATE. Do not share cached managed
entities across the lock boundary or use stale second-level/query-cache results.
This protocol blocks cooperating writes, not arbitrary out-of-band SQL, which is
outside application mutation authority. A later snapshot strategy must preserve
the same public contract. See official [isolation](https://www.postgresql.org/docs/current/transaction-iso.html)
and [row locking](https://www.postgresql.org/docs/current/explicit-locking.html).

Synchronous durability replaces H2 WRITE_DELAY: selected future PostgreSQL policy
is fsync=on and synchronous_commit=on, normal logged tables for financial/control/
receipt data, success only after database commit. No replica/failover durability
claim; hardware/configuration still matters. Official [WAL guidance](https://www.postgresql.org/docs/current/runtime-config-wal.html)
explains local WAL flush; do not downgrade it silently to reduce latency. Connection
loss during commit stays OUTCOME_UNKNOWN until the same operation is reconciled
after reconnection. A known rollback changes no record/action/contribution; deadlock,
constraint and connection failures need PostgreSQL evidence, not H2 assumptions.
No automatic pruning of deleted/idempotency/control records: action expiry is not
purge permission; retention remains unapproved.

Range list includes matching full-range summary; independent aggregate/list calls
may have different generations and must not be presented as one snapshot. UI
refreshes after changed mutation; no-op does not invalidate views. Queries never
mutate durable ledger state. Public pagination is transport-independent: future
HTTP uses these same cursor/filter/snapshot semantics, not Page/Pageable DTOs or a
new offset scheme; see [shared pagination rule](BACKEND_RULES.md#shared-pagination-first--requirement-9).

Selected cursors are 128-bit random tokens for bounded **in-memory** records:
owner, recent/range scope, filter/range, last order key, generation, expiresAt.
TTL 10 minutes, max 100 tokens for this personal user; evict oldest on capacity.
No durable writes or extra signing key; restart/eviction/tampering gives INVALID_CURSOR,
changed generation gives STALE_CURSOR, both refresh from page one. Next uses keyset
order; Previous reuses the adapter's prior page-token stack. Missing prior token
also refreshes. No snapshots across mutations or unbounded historic conversation
are promised. Entry selection always re-gets current revision before Edit/Delete.

The bounded recent adapter now retains immutable per-message navigation routes:
Next adds the current page cursor; Previous reuses the prior cursor or refreshes
page one when history is missing. At most 100 in-memory page contexts and 100 prior
cursors per context, with ten-minute expiry; controls bind authenticated private
chat and original sent-message ID. Selection carries only a displayed ID and
re-gets current state. Invalid/stale service cursors clear history and refresh.
LedgerCursors now supplies that same policy to recent and range methods; range
pages include the exact matching full-range summary under the same head lock.
Compact range/filter read metadata on Telegram buttons supports same-query refresh
after ephemeral state loss, with base-36 selection IDs fitting the 64-byte limit;
public IDs remain decimal. Known routes bind message/chat/query; unknown controls
revalidate the fallback as an authorized read query, never financial proof. There
is no extra signing key, durable snapshot or reporting database. Native aggregate
projections convert NUMERIC exactly to BigInteger; actual PostgreSQL execution,
mapping/query validation, locks and concurrent snapshot behavior remain unverified.

## Telegram → application → future LLM capability matrix

This is the logical service catalog; NATIVE-AI-1's installed bounded subset is
listed below, not every proposed tool spelling here is registered.
Every row uses host-trusted context, the same value/access/replay checks and typed
results. Model arguments cannot include actor, confirmed=true or persistence access.

| Telegram action | Application contract / required business input | Future LLM tool | Output / effect boundary |
| --- | --- | --- | --- |
| Clear phrase / signed entry | TransactionService.create(TransactionInput) | `create_transaction` | Immediate receipt after commit; ambiguity clarifies without write. |
| /recent and selection | LedgerQueryService.recent(cursor?) | `recent_transactions` | Owned bounded page; read-only. |
| /show ID | LedgerQueryService.get(id) | `get_transaction` | Owned active/deleted view or generic unavailable result. |
| Edit fields / Date | TransactionService.previewUpdate(id, expectedRevision, patch) | `update_transaction` | Immutable review action, no financial mutation. |
| User Save / Cancel | TransactionService.update(actionId, proof) / cancel(actionId, proof) | **Not exposed to the model** | Save exact stored preview once; Cancel changes nothing financially. |
| /overview x y, standalone type total | LedgerQueryService.aggregate(range, typeFilter) | `aggregate_transactions` | Full-range Overview/TypeTotal. |
| /income x y, /expenses x y, /transactions x y | LedgerQueryService.list(range, typeFilter, cursor?) | `list_transactions` | Page plus same-snapshot full-range summary; selectable entries/Next/Previous. |
| Delete selection / /delete ID | TransactionService.requestDelete(id, expectedRevision) | `request_delete_transaction` | ConfirmationRequired; **no deletion**. |
| Actual Confirm Delete | TransactionService.confirmDelete(actionId, proof) | **Not exposed to the model** | Target/action/revision-bound reversible deletion after user proof. |
| Restore selected deleted ID | TransactionService.restore(id, expectedRevision) | `restore_transaction` | Active view/receipt; restored contribution once. |

Commands shown here are selected UX spellings: capture, /start, /help, /show and
/recent and L2 controls plus the range commands have bounded local code; no live
execution is claimed. /show and /delete require
an ID; /overview, /income, /expenses and /transactions require both dates; /recent
needs none. Malformed controls return a correction example and never fall through
as purchase text. /start and /help show grammar, recent and range examples.
Get/recent/range entry buttons supply the actual selected ID/revision; no extra
free-text edit/delete command syntax or fuzzy record search is required.
A deleted receipt/get view exposes Restore. All state-changing controls follow
the same application boundary; there is no direct-update bypass for future tools.

## Native conversational interpretation

RULES-LLM-ROUTING-1 keeps rules-first success and routes ordinary recognition
failures to this same native path, not a bespoke interpreter. Problem.failureKind
is internal parser evidence; legacy REJECTED status alone no longer blocks native
interpretation. Recognized invalid financial facts remain local; unsupported
financial requests may clarify UNSUPPORTED, never silently select a partial capture.
All native callbacks still use the same validated value/mapper/service boundary.
Unbound amount-only input reaches native clarification; a guessed create cannot
borrow type/note from history and is host-converted to Missing.TYPE. Missing current
amount cannot be invented; foreign currency prefixes/suffixes and arithmetic fragments
cannot be discarded to pass Money. Existing ASCII sign/type assertions agree with
native type, while negative magnitudes stay invalid. Recovery/storage failures,
authorization and actual-user Save/Delete proofs never fall through to inference.
No new callback/capability, model loop, memory expansion or default-off change.

AMOUNT-CLARIFICATION-1 is a host-local branch within this binding, not an additional
native tool: exact local amountless phrase → validated short-lived source facts →
explicit unsigned amount reply → existing LedgerClient.create. The mapper builds
TransactionInput with the original Vietnam financial date and source description;
the authenticated completion event/message timestamp owns ingest/create identity.
No model call or ledger-result egress is involved. The source is consumed before
dispatch; frozen-event recovery takes precedence over ephemeral pending/attempt
state. A retry before ingest has frozen its input may reuse only that same event's
cached exact input, never another amount/event. All shared auth, Money, fingerprint,
revision and actual-user confirmation contracts remain. Existing memory TTL/reset/
identity/capacity bound this metadata, with a non-extending ten-minute source deadline
and at most eight source-event tombstones; no schema/persistent pending workflow.
See [lifecycle and limits](../apps/server/README.md#missing-amount-capture-clarification).

NATIVE-AI-1 supersedes DS-FALLBACK-1 and NL-QUERY-1 at the same default-off activation
boundary. Spring AI ChatClient and native callbacks replace bespoke JSON interpreters;
local supported capture/commands remain direct. See [configuration and limits](../apps/server/README.md#opt-in-deepseek-conversation)
and CURRENT STATUS for exact candidate/evidence. The capability matrix above remains
the service catalog; installed callbacks are list/recent/get/create/preview-update/
request-delete/restore/clarify. Aggregate information comes with the authoritative
list summary, not a separate model arithmetic tool. Save/Confirm/Cancel stay host-only.

The host freezes reference time from authenticated message or receipt, supplies
Vietnam business date and validated previous query metadata, and derives conversation
identity from principal + bot/user/private-chat scope. Model periods TODAY/YESTERDAY/
THIS_MONTH/LAST_MONTH resolve locally; explicit dates use DateRange. THIS_MONTH is
inclusive month-to-date, LAST_MONTH the whole previous month. FOOD is exactly active
EXPENSE + Category.FOOD; matching full-range totals and keyset pages share the existing
read snapshot and cursor bindings. KEEP reuses only validated dimensions; no context
means clarification, never guessed dates/filter. Unsupported categories/note search
must clarify, not silently post-filter a page.

One model request proposes exactly one native callback. Strict argument validation
precedes controlled ToolCallingManager execution; multi-call/unknown/malformed/
truncated responses never dispatch. Tools receive trusted context outside model
arguments. Create uses literal current amount and local exact Money/date/category/
note validation; get/action targets must be explicit or host-selected. Edit creates
an immutable preview and Delete only requests confirmation. Restore uses the ordinary
authorized revision/replay service. No model can supply proof, owner, event identity,
authoritative totals, or perform actual-user Save/Confirm/Cancel.

Before interpretation, TransactionService.recoverOperation recovers frozen create,
update/delete preview or completed direct Restore under the original event/scope.
It reuses existing inbox/control records, with no schema/raw-prose archive. Conflict/
unavailable storage is not absence. Recovery holds the short service transaction,
then releases it before inference. New dispatch still uses shared fingerprints and
revision checks. Returned previews can be stale/expired; durable display/proof checks
remain authoritative. Existing captured creates can finish recovery before a reply.

ChatMemory retains eight user-only utterances and bounded validated query/target
metadata, ten-minute idle expiry, sixteen contexts, explicit /reset and no restart
persistence. Ordinary reply-to conversation/page is distinct from durable field
prompts. Neither assistant/native call arguments nor ledger/tool results enter
history. Vietnamese receipts/pages/previews render directly from services; there is
no second provider narration or ledger-result egress. User/history egress is still
sensitive and unapproved for live use. No fuzzy selection, arbitrary filters or
unlimited agent loop is implemented.

Quick financial capture/questions, not extended social conversation, remain the
goal; existing memory bounds are unchanged. After numeric Telegram authorization,
available first/last display names pass only to the localized clarification
renderer: NFC, allowlisted Unicode letters/digits/marks/basic separators, bounded
to 64 code points, neutral fallback. Display text never enters HostContext, service
arguments, provider prompts, conversation identity or memory. No nickname storage.
Native output-token options are unset; the configured provider/model default is
variable/unknown, not unlimited or necessarily cheaper. Legacy DEEPSEEK_MAX_TOKENS
is not mapped. Single-tool, request/rate/concurrency/deadline and retry controls
remain unchanged; live/privacy/cost decisions are not waived.

Fixed host checks cannot establish arbitrary semantic correctness. Live Vietnamese
accuracy and intent/prompt-injection behavior require separately authorized model
trials. [Spring AI native tools](https://docs.spring.io/spring-ai/reference/api/tools.html)
and [memory](https://docs.spring.io/spring-ai/reference/api/chat-memory.html) document
the reused APIs; fake/SDK fixtures establish local integration, not provider quality.
Provider privacy, monetary cap, credential and live execution decisions remain open.

## Future finance capabilities

Future slices add cohesive capabilities **when authorized**, not a mandatory
foundation layer for the two-service current ledger. Shared value meanings are in
[Domain extensions](DOMAIN.md#future-finance-domain-contracts). Method names below
are proposals; no new tool is registered and no consumer bypasses the current
authorization, exact money/date, replay, revision or persistence boundaries.

| Future capability | Proposed input → business result | Integration / write boundary |
| --- | --- | --- |
| Classification via owned transaction metadata update (C1) | ID/revision plus necessity/pattern/eligibility override → saved classification and reason | Extend TransactionPatch only after Lead agrees shared contract; no new financial entry or guessed category semantics. |
| BudgetService.setDefault / status (B1) | Limit/effective date/settings revision → policy receipt; financial date → DailyBudgetStatus with reasons | Store settings separately, query active eligible ledger records; post-save expense feedback is a read, not another expense. Failed feedback leaves successful ledger save intact. |
| AllocationService.suggest / get / edit / keep / reject (S1) | Owned income ID/revision and explicit targets/rule snapshot → explained draft/plan or infeasibility | Advisory persistence only; recheck changed/deleted source before Keep; no spending mutation or funds movement. |
| AllocationService.getRules / setRules (S2) | Versioned user rule set → saved profile; same source contract → explained next suggestion | Profile edits do not silently rewrite prior plans. Percentage rounding/priority/base policy must be settled before this mode. |

Future Telegram controls and any later LLM tools wrap these same application
methods. A model cannot silently configure a spending policy, invent savings
percentages, keep stale advice or treat an allocation as a payment. Feature slices
own bounded outcomes, accepted-input requirements and proposed scopes; the single
[status source](WORK_STATUS.md) owns actual assignments. No shared file becomes
concurrently writable because two features have separate folders.
