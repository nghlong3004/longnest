# Vietnamese input and transaction contract

Status: selected domain specification; the bounded pure input/value implementation
is accepted as recorded in [CURRENT STATUS](WORK_STATUS.md), not a working ledger.
Single personal user, VND,
immediate saving of clear validated input, clarified ambiguity, confirmed reversible
deletion, editable financial date, inclusive ranges and paginated type views are
approved. Default business timezone: `Asia/Ho_Chi_Minh` (Vietnam UTC+7).
Grammar/limits below are selected Lead decisions; exact implemented scope and
verification remain in CURRENT STATUS, not blanket acceptance of this specification;
future finance policy remains proposed in [Decisions](DECISIONS.md).
[Application contracts](SERVICES.md) own the
OOP use cases and future tools; Telegram is an adapter, not the business layer.

## Selected first-release grammar

The grammar below is local asserted capture, not questions. Recognized read cues
route to clarification or default-off native conversation before capture.
NATIVE-AI-1 supersedes NL-QUERY-1's ISO-only source gate with trusted Vietnam-relative
periods or explicit validated dates and bounded follow-ups. THIS_MONTH means
month-to-date; FOOD means exactly EXPENSE + Category.FOOD. Unsupported filters or
missing usable context clarify. See
[adapter boundary](features/adapters/README.md) and CURRENT STATUS. This guard does
not prohibit punctuation in structured Note edits or typed transaction inputs;
authenticated field prompts retain their explicit edit → Save semantics.

One text message specifies at most one transaction. Reject line breaks/semicolons
before whitespace normalization. Normalize Unicode to NFC, trim ends, collapse
whitespace and match phrases/suffixes case-insensitively.
Do not globally remove accents or use fuzzy/substring matching. Only the explicit
accentless aliases below are supported. Preserve human wording for preview/note;
permanent raw-message retention is a separate privacy decision.

```text
message := phrase SPACE amount
         | sign [SPACE] amount SPACE description
sign := "+" | "-"                    (ASCII, leading only)
amount := digits [[SPACE] currency-label]
        | digits suffix
        | digits decimal-separator fraction suffix
digits := one or more ASCII digits
decimal-separator := "." | ","
fraction := one or two ASCII digits
suffix := "k" | "tr"                 (attached, case-insensitive)
currency-label := "đ" | "vnd"         (only for whole-dong integer form)
description := nonempty text with no digits, signs or transaction separators
```

Semicolons/newlines/multiple amount tokens or transactions are unsupported; do not
save just the first recognized amount. Length above 256 Unicode code points before
normalization is rejected (not UTF-16 code units). Date tokens, digits in notes and suffix-separated `45 k` are outside
this deliberately small grammar. Slash commands below are control actions, not
an alternate required transaction-input mode.

| Exact phrase / explicit alias | Type | Category |
| --- | --- | --- |
| `ăn sáng`, `an sang`, `ăn trưa`, `an trua`, `ăn tối`, `an toi` | expense | food |
| `cà phê`, `ca phe` | expense | food |
| `đi chợ`, `di cho` | expense | groceries |
| `xăng`, `xang` | expense | transport |
| `tiền nhà`, `tien nha` | expense | rent |
| `nhận lương`, `nhan luong`, `lương`, `luong` | income | salary |
| `thu nhập`, `thu nhap` | income | other_income |

Unsigned input must match a complete phrase followed by one amount; unknown
phrases need clarification. Signed descriptions may contain any other nonnumeric
text (category `uncategorized`), except explicit unsupported cues `hoàn
tiền`/`hoan tien`, `chuyển khoản`/`chuyen khoan`, `chuyển tiền`/`chuyen tien`,
`vay`, `nợ`/`no`, `trả nợ`/`tra no`: these need a scoped refund/transfer/debt flow.
Match those cues at whole-word/phrase boundaries, never arbitrary substrings.
This is not a guarantee to detect every financial concept in unrestricted text.

For signed descriptions, identify listed cues at whole-word/phrase boundaries.
If any cue conflicts with the sign, ask for clarification (`+45k ăn trưa`,
`-25tr lương`); neither cue silently overrides the other. Multiple known cues of
different types also clarify. Reuse a category only when the entire description
equals one known phrase; otherwise keep `uncategorized`. Later expansion must
extend this table and the [acceptance examples](features/ledger/ACCEPTANCE.md), not add guesses.

## Amount and date rules

- `k` multiplies by 1,000; `tr` by 1,000,000 base VND units. Parse with exact
  decimal/integer arithmetic; never binary floating point. Store a positive
  whole-dong integer magnitude plus a separate income/expense type.
- Accepted forms include `45000`, `45000đ`, `45000 vnd`, `45k`, `25tr`, `1.5tr`,
  `1,5tr`, `0.5k`. Suffix decimals allow one/two digits only; scaling must yield
  whole dong. Positive transaction values must be within 1–10^12 VND inclusive.
  Normalize any leading zeroes away in the canonical amount string; zero itself
  remains invalid. A currency label may attach or have one normalized space.
- Bare `45` means **45 VND**, never 45,000. Always show the full amount/currency
  in the saved receipt or required preview. The user can correct it, or cancel a
  pending action; do not silently rescale an implausible amount.
- No grouping separators in base amounts: `45.000`, `45,000`, `1 000 000` ask for
  an ungrouped integer or supported suffix; never guess decimal versus thousands.
  `1.234tr` also clarifies rather than accepting an unsupported precision.
- Reject zero, scientific notation, arithmetic, repeated signs, overflow, `NaN`,
  Unicode lookalike signs and mixed/unsupported currencies. `-` is a type marker
  only in the leading signed-message form, never a stored negative magnitude.
  At the amount/description boundary, reserve standalone ISO currency codes
  recognized by the bound JDK's `java.util.Currency` (case-insensitively) and
  Unicode currency-symbol tokens for currency interpretation, not note text.
  Thus `-45k USD đồ dùng` rejects rather than silently recording VND; a currency
  marker after a suffixed amount also rejects (`-45k vnd đồ dùng`). Only the
  integer form can consume one `đ`/`vnd` label. This is a bounded lexical rule,
  not general foreign-language currency detection or conversion; later ordinary
  note words/acronyms are not reinterpreted as currency codes.
- Transaction date and creation time are different: use `transactionDate` as
  an editable local calendar date, defaulting to the authenticated message date in
  `Asia/Ho_Chi_Minh`. `createdAt` is the immutable server commit instant; keep
  source `messageAt` separately. A delayed confirmation retains the displayed
  transaction date across midnight; never substitute host timezone or commit date.
  Explicit Edit Date accepts a valid `YYYY-MM-DD` for forgotten/backdated entries.
  Accept explicit real dates from 1900-01-01 through 9999-12-31 inclusive, including
  future financial dates. This is a date label, not scheduling or a reminder;
  no new future-date prohibition is inferred. Queries use the same supported bounds.
  Free-form date interpretation remains outside the current deterministic grammar;
  backdated dates do not invent an exact historical time of day.
  Current rules flag relative-date cues `hôm qua`/`hom qua`, `hôm kia`/`hom kia`,
  `hôm nay`/`hom nay`, `ngày mai`/`ngay mai` at word boundaries, including in signed
  descriptions, as unsupported language, not invalid Money/DateRange values. Future
  native interpretation can resolve supported periods when explicitly enabled;
  default rules do not infer dates. This small deterministic vocabulary is not a
  general date recognizer; model interpretation accuracy remains unverified.
- Summaries show income, expense and net flow, not a claimed bank balance. No
  opening balance or account-transfer model exists in this design.

## Interactions — design examples, not observed behavior

Selected field limits apply to **all callers**, not only text parsing: note is a
single-line NFC string of at most 256 code points; omitted/empty means no note.
Reject control characters. Parser notes preserve the normalized description, not
the whole message/amount. Structured Date/Note edits are not restricted by the
transaction-message grammar (digits in an edited note are allowed).
Categories are the table's fixed enums plus `uncategorized`: food/groceries/
transport/rent for expense, salary/other_income for income. Category is optional,
not mandatory/fixed-spend classification. Explicit incompatible category fails;
when type alone changes and the old category is incompatible, preview its reset to
uncategorized before Save. Amount edits accept the same unsigned amount forms;
Type uses income/expense choices, Date uses strict YYYY-MM-DD, and an empty Note
clears it. No new transaction is inferred from a field-edit response.

Per-entry amount bounds do not cap aggregate totals. Sum/count/net use exact
arbitrary-precision integer arithmetic; all money totals/counts and ID/revision/
generation values cross future JSON boundaries as canonical decimal strings.
Dates use ISO local-date strings, instants UTC ISO-8601. Do not return rounded or
overflowed totals. Query ranges may cover the entire supported date domain; no
hidden maximum-day restriction or guessed endpoint.

Selected end-user presentation is Vietnamese, externalized through Spring message
bundles; protocol commands, callback identities and machine error codes remain
unchanged. Project coordination, code identifiers and surrounding documentation
remain English. Copy examples below are illustrative, not observed live behavior. Positive
examples use the approved immediate-save path after authorization and validation.

| Input | Illustrative response / selected outcome |
| --- | --- |
| `ăn trưa 45k` | “Saved #42 · expense · food · 45,000 VND · [date]. Edit / Delete.” |
| `nhận lương 25tr` | Saved income · salary · 25,000,000 VND with stable ID. |
| `-45k ăn trưa` | Saved expense · food · 45,000 VND with stable ID. |
| `+25tr lương` | Saved income · salary · 25,000,000 VND with stable ID. |
| `-1,5tr đồ dùng` | Saved expense · uncategorized · 1,500,000 VND; note “đồ dùng”. |
| `ăn trưa` | Clarify: “How much? Example: ăn trưa 45k.” No ledger entry. |
| `45k` | Clarify type/description; offer an accepted signed example. No entry. |
| `mua sách 80k` | Unknown unsigned phrase; suggest `-80k mua sách`. No entry. |
| `+45k ăn trưa` | Clarify income versus expense; do not choose silently. |
| `ăn trưa 45.000` | Clarify amount: use `45000` or `45k`. No entry. |
| `ăn trưa 0`, `--45k ăn trưa`, `ăn trưa 1e6` | Reject invalid amount/grammar with a safe example. |
| `ăn trưa 45k; cà phê 20k` | Reject multiple entries; ask for separate messages. |
| `hoàn tiền 45k`, `+45k hoàn tiền` | Explain refund flow is not scoped; never relabel as ordinary income. |
| `ăn trưa 45k hôm qua` | No write in current rules; explain relative-date parsing is unsupported. Enter a supported transaction then use Edit Date, or use the explicit Date field. Future LLM interpretation is separate. |

Approved behavior: a fully valid, authorized, unambiguous entry creates
a transaction immediately and receives its complete receipt/ID. Missing or ambiguous
input never writes; default rules ask for complete resubmission. Opt-in conversation
may use bounded history/validated query scope, never unbounded context or proof.
Do not add mandatory preview to clear creations or execute an unresolved ambiguous
mutation. A future interpreter uses the same application policy; model confidence
does not resolve missing facts or authorize an action that the user did not request.

Selected update UX: Edit selects a field and shows its current value; send a new
amount/type/category/note/Date in that explicit edit context, then review the complete
before/after preview and click Save or Cancel. The complete service-owned flow is
in [Services](SERVICES.md#action-lifecycle-and-replay). No implicit message editing.
Approved delete protection means selecting Delete shows ID/type/amount/date and
requires explicit Confirm Delete or Cancel.
Actions bind to the authorized actor, opaque interaction scope, target ID and
expected revision; Telegram chat bindings stay in the adapter. Pending actions
expire after 10 minutes; changed/stale/expired actions cannot apply.
Public business revision starts at 1 and advances only on a changed financial
mutation. It is separate from Hibernate's provider-managed `@Version` rowVersion;
JPA mapping/entity lifecycle belongs to the persistence adapter, not Money or
transport records. [Services](SERVICES.md#persistence-and-query-consistency) owns
the atomic lock/transaction boundary; [Backend rules](BACKEND_RULES.md) governs
Java DTOs, mappers and service interfaces without changing these domain semantics.
Confirm succeeds once; Cancel changes nothing. An update may change amount, type,
category, note or transaction date, never owner/ID/creation time. Category/type must
remain compatible. Editing/deleting a Telegram message does not mutate the ledger.

Confirmed reversible deletion is approved: exclude the entry from normal reads and
totals, and support Restore for the retained deleted record. Restore re-adds its
contribution exactly once. A selected owned deleted record can show a deleted view
with Restore; normal lists exclude it. Purge/recovery duration and backup treatment
remain unselected; permanent deletion is not a release-one alternative to reopen.
Do not mandate an audit/event-sourcing system to provide personal CRUD.

Only report success after durable commit. A failed save says “not saved”; uncertain
delivery after commit must be recoverable by retry/review, never a second entry.
IDs are stable and never reused. An unknown or unauthorized ID produces a generic
not-found/unavailable response without exposing another user's transaction.

The same transport event/action and repeated confirmation return the existing
outcome. **Selected PE2:** event/action replay protection only; distinct user messages
are distinct purchases when valid and clear. No semantic five-minute duplicate
prompt interrupts capture. Adding that heuristic later needs an explicit product
decision because it would change fast-save behavior. Equal amounts are not proof
of duplication. See the rationale in the
[ledger slice](features/ledger/README.md#resolved-crud-completion-decisions). Required
pending/dedupe state survives restart. Repeated edit/delete/restore actions return
their existing outcome, not a second mutation. Raw text is not a required key.

## Bot capabilities and date ranges

These are reusable application contracts with Telegram UX and future typed tools,
not installed tool endpoints or permission to execute. The canonical
[capability matrix](SERVICES.md#telegram--application--future-llm-capability-matrix)
maps create/get/recent/update/date-edit/delete/restore, aggregates and lists to
application services and future tools. Trusted actor scope is supplied by the host,
never selected by input text or a model. Financial rules are transport-independent.

**Approved presentation:** overview shows totals only; income and
expense views show that type's total/count plus the first 10 selectable entries and
Next/Previous. A type-filtered summary (“expense total 90,000”) is distinct from an
individual list (“#42 45,000; #43 45,000”). Total-plus-list with 10-entry pages and
stable selection is now accepted, not an unresolved interpretation question.

Query rules (selected commands/cursor mechanics in Services; inclusive semantics approved):

- Strict real calendar dates in `YYYY-MM-DD`; both endpoints required, x <= y is
  binding, and x = y is one valid day. Reject a missing/invalid endpoint or reversed
  range with a correction example; never silently swap endpoints or run another
  period. Empty valid ranges return zero totals/counts and “no transactions”.
- Inclusive local dates: x through y includes both full business dates in
  `Asia/Ho_Chi_Minh`. Select by `transactionDate`, not `createdAt`. For default-dated
  timestamp reasoning, this is `[x 00:00, (y+1) 00:00)` locally, not end-of-day
  rounding; a backdated entry is selected by its chosen date even if created later.
- Example single-day boundary: Vietnam 2026-10-20 corresponds to default-message
  instants `[2026-10-19T17:00:00Z, 2026-10-20T17:00:00Z)`. Show the effective dates
  and timezone in replies so the user can see the selected period.
- Totals cover the entire filtered range, never just the visible page. Positive
  income/expense totals are separate; net flow = income minus expense may be
  negative. No currency conversion or bank-balance claim. Deleted entries and
  uncommitted actions are excluded; approved restores include the entry again.
- Use 10 entries per range page, ordered by transaction date descending then stable
  ID descending. Cursor is opaque and bound to owner/range/type and ledger generation;
  invalid, expired or stale cursors request refresh. After a mutation, refresh from page one; do not
  promise a historical snapshot or add a snapshot service. Show selected range/type
  on every page. Zero results and unknown IDs are ordinary clear outcomes.
  Recent uses creation time descending then ID so a newly captured backdated entry
  remains easy to find. Range-page summary and entries share one read snapshot.
- A committed amount/type/date update removes the old contribution and adds the
  new one in the affected type/date totals. Delete removes exactly that entry's
  contribution. No stale cached total or double application; query current ledger.
- Examples: `/overview 2026-10-31 2026-10-01` asks to fix the order; `/income
  2026-02-30 2026-03-01` asks for a real date. Neither mutates or substitutes a query.

## Input-to-normalized contract

Logical boundary, **not implemented APIs/tool schemas**. Design version 1 now
covers typed intents for all approved operations, not only create candidates.
See [Services](SERVICES.md#typed-inputs-and-results) for canonical payload/result
details and the capability matrix; do not duplicate business validation in parsers.

| Boundary object | Fields and provenance |
| --- | --- |
| Adapter envelope | Telegram update/chat/message IDs and received/message instants stay in the transport adapter. Map authenticated identity and source/action correlation to host ApplicationContext; these transport fields do not enter core domain objects. |
| ApplicationContext | Host-trusted personal principal, opaque operation identity, supplied business reference date/instant, VND and Asia/Ho_Chi_Minh. No model/utterance may choose authorization, idempotency grants or user-confirmation proof. |
| InterpretationResult | Design version 1, status one of ready, unsupported, needs_clarification, rejected; typed intent only when ready, stable reason codes and missing/conflicting fields otherwise. No unresolved mutation reaches execution. |
| Typed intent / tool request | A bounded create, get, recent, update, aggregate, list, request-delete or restore request, using the service contract's exact required fields. Dates may be explicit or grounded in supplied business reference by a future interpreter; no invented date or business fact. |
| Candidate create/update data | Positive canonical integer amount string, VND, income/expense type, optional valid transaction date, optional category/note. Category may default to uncategorized; it is not a mandatory missing fact. No owner, persistent ID for create, creation time, SQL or confirmation flag. |
| Validated action | Application-owned operation identity and target/revision if any; validate values/owner/current state. A pending confirmation binds exact action/target/content, actor, expected revision and expiry. Model tools cannot mint proof or confirm deletion. |
| Business result | Selected service-generated transaction/deleted view, durable mutation receipt, aggregate, page, clarification or failure. Results, not model prose, establish saved/deleted/restored outcomes. See Services for canonical payloads, errors and JPA/PostgreSQL persistence boundary. |

Reason codes distinguish parser coverage (`UNSUPPORTED_INPUT`, `UNKNOWN_PHRASE`)
from genuinely absent/conflicting facts (`MISSING_AMOUNT`, `MISSING_TYPE`,
`CONFLICTING_TYPE`, `AMBIGUOUS_AMOUNT`) and invalid values/operations. Unsupported
input in default rules asks for supported wording or explicit fields. Opt-in native
conversation may resolve coverage gaps, not invent missing facts.

All adapters/tools pass through application authorization and the same Money,
date/range, category, revision, action-proof, idempotency and persistence checks.
Unknown fields/enums fail validation. Core objects never contain Telegram prompts,
chat IDs or model sessions. Queries do not write. A future model may request
deletion but only the host's authenticated human action can confirm the displayed
target; an LLM's `confirmed: true` is not an accepted field or proof.

The selected [native route](SERVICES.md#native-conversational-interpretation) keeps
clear local capture and replaces bespoke LLM interpretation with bounded tools.
Relative dates use the host business reference/timezone; deterministic grammar
remains unchanged. No live LLM execution, provider/cost commitment or external
transmission of financial data is authorized now.

## Future finance domain contracts

These are **proposed extensions**, not current transaction requirements or accepted
implementations. Feature folders own their policy examples/choices; shared values
and invariants are defined here to prevent separate parsers/ledgers in each slice.

| Concept | Proposed canonical meaning / invariant | Owning future slice |
| --- | --- | --- |
| SpendingClassification | Independent necessity (mandatory/discretionary/unclassified), cost pattern (fixed/variable/unknown), optional explicit daily Include/Exclude override and visible provenance. Category is descriptive, not proof of either axis. No change to amount/type/date from relabeling. | [C1](features/spending-classification/README.md) |
| EligibilityPolicy | Deterministic, explainable include/exclude decision over owned active expenses, with user override and known policy version. Exact basis/unknown handling requires Human choice; no silent historical rewrite. | C1 + [B1](features/daily-budget/README.md) |
| DailyAllowancePolicy | Nonnegative whole-VND limit or explicit unset state, effective financial date and revision. Limit changes do not create money transactions. Rollover/proration/mid-day treatment remain proposals. | B1 |
| DailyBudgetStatus | Business date, applicable allowance/policy, eligible spent amount, included/excluded/unknown reasons, remaining or overspent. Calculate from authoritative active transactions by financial date; no second spending counter/bank balance. | B1 |
| AllocationRules | User-provided fixed/percentage/residual instructions with version and explanation basis; no shipped savings percentage or priority. Exact base/rounding/residual/shortfall behavior requires Human choice. | [S1/S2](features/salary-allocation/README.md) |
| AllocationSuggestion | Advisory plan ID/revision, source owned income ID/revision, rule snapshot, nonnegative envelope amounts, explicit unassigned amount or infeasibility, draft/kept/rejected/outdated semantics proposed. Plan operations never create ledger expenses, income or transfers. | S1/S2 |

Reuse Money, financial date, identity/revision and authorization boundaries. Zero
may be a budget limit/envelope amount; zero is still invalid as a real income/
expense transaction. Configuration and advisory records live apart from financial
entries even when stored in the same PostgreSQL database. No core policy is enforced
only in Telegram or prompts. Source edits/deletes/restores and relevant metadata
changes must be handled by consumers without duplicating financial effects.
