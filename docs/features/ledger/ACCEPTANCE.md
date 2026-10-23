# Ledger requirements and observable acceptance

State: **design/planned checks, not completed tests**. D1–D17 in
[Decisions](../../DECISIONS.md) contain approved direction. Selected technical
contracts live in [Domain](../../DOMAIN.md) and [Services](../../SERVICES.md).
No runtime, tool SDK, external transmission or implementation is authorized here.

## First-release behavior and evidence

| ID | Requirement | Observable future acceptance example |
| --- | --- | --- |
| R1 | Vietnamese input creates transactions immediately when clear | For the authorized personal user, `ăn trưa 45k` saves expense 45,000 VND/food; `nhận lương 25tr` saves income 25,000,000 VND/salary. Full receipt/stable ID follows durable save, without a blanket preview gate. |
| R2 | Bounded signed shorthand | Under the selected grammar, `-45k ăn trưa` and `+25tr lương` match unsigned entries. Conflicting `+45k ăn trưa` clarifies, no write. |
| R3 | Exact VND amounts and value invariants | Supported decimals/suffixes normalize to exact integer dong; bare 45 is 45 VND, not 45k. Reject zero/invalid/out-of-bounds values without rounding. Transaction magnitude positive; aggregate net may be negative. Validate directly through the application service too. |
| R4 | Missing/ambiguous/unsupported input does not mutate | Missing amount, missing type, ambiguous separators, multiple entries or unsupported current phrasing returns clarification/error. Today's rules do not silently accept relative dates. Optional category can remain uncategorized. |
| R5 | Personal identity authorized for every operation | An unapproved actor cannot create/get/recent/update/delete/restore, aggregate, list or confirm an action. Foreign IDs/cursors/actions disclose nothing. Test service entry points without relying only on Telegram's initial access gate. |
| R6 | Complete review/completion boundaries | Edit field → validated before/after preview → actual user Save commits that stored action once; no replacement patch at Save. Cancel/10-minute expiry/replacement/stale/wrong-target changes nothing. Delete displays exact ID/type/amount/date and requires authentic target-bound Confirm. Clear creation still saves immediately. |
| R7 | Durable outcome and truthful receipt | JPA/PostgreSQL-backed store restart/reconnection retains records/actions/idempotency. Confirmed rollback returns NOT_SAVED; uncertain commit returns OUTCOME_UNKNOWN with same-operation recovery, not a fresh purchase. Retry after commit returns original receipt, labeled as original if the entry has since changed. Model narration is not success. |
| R8 | Retry-safe mutations without purchase suppression | Same-event create and repeated Save/Delete/Restore callbacks, including concurrent/restart cases, have one financial effect. Changed payload under the same operation conflicts. Two distinct clear identical coffee messages create two IDs immediately; no semantic duplicate prompt. |
| R9 | Inclusive overview for valid dates | After income 25,000,000 and expense 45,000 in October, overview Oct 1–31 shows totals/counts and net 24,955,000 for the full range. Reply identifies VND, dates and Asia/Ho_Chi_Minh; not a bank balance. |
| R10 | CRUD plus reversible delete/restore for both types | Create → get → update amount/type/date/note → confirmed delete → get deleted view/restore. 45,000 corrected to 40,000 contributes 40,000; deleted contributes zero; restored contributes 40,000 once. Stable ID/creation time unchanged; stale revision conflicts. |
| R11 | Financial date independent of creation time | Vietnam 2026-10-20 spans default-message instants [2026-10-19T17:00Z, 2026-10-20T17:00Z). Explicit Date edit moves contribution to the selected business day, not creation day. Delayed confirmation preserves displayed date; type/date changes update both affected totals. |
| R12 | Recoverability/private storage | Under separately selected operational policies, restore an isolated backup and reconcile records, deletion/restore state, action/idempotency state and totals. Check redacted logs and private-file exclusion. This approves neither a host nor a retention/purge period. |
| R13 | Real inclusive date ranges with x <= y | Equal endpoints query one day. Invalid date 2026-02-30, missing endpoint or reversed range asks for correction, never silently swaps/defaults. Valid empty range returns zeros and an empty list. |
| R14 | Type totals plus selectable filtered lists | Income/expense views show the selected type's full-range total/count plus its paginated entries. Every entry matches owner/type/date and active-state rules; total is not the page subtotal. Overview remains a different query result. |
| R15 | Bounded recent/list/get selection | At most 10 entries per page. Recent uses creation time/ID descending; range lists financial date/ID descending. Tokens bind owner/filter/generation, expire at 10 minutes, and are invalid after restart/eviction. Reject foreign/stale tokens and refresh. Each range page includes matching full-range summary from one snapshot; selected IDs re-get current revision. |
| R16 | Reusable OOP capabilities, not chat-only rules | Call TransactionService/LedgerQueryService from an isolated non-Telegram test adapter: identical money/date/access/idempotency/lifecycle/query behavior and typed results. Core domain values/objects contain no Telegram/model dependencies. No separate agent platform needed. |
| R17 | Deletion request is not deletion authority | requestDelete returns ConfirmationRequired without removing #42. Only confirmDelete with host-authenticated proof for that displayed action/owner/ID/revision commits. Model-like `confirmed:true`, old proof, wrong target or missing proof cannot delete. Restore uses the same authorized service boundary. |

Command/method/tool names, tagged payload/error contracts, update completion,
limits/actions/cursors and JPA/Flyway boundary are selected design, not executed APIs.
Approved product defaults are unchanged. All criteria need implementation evidence.
The [backend rules](../../BACKEND_RULES.md) add implementation-convention evidence,
not new product behavior: migration-owned constraints, schema validation, @Version
versus business revision, short owner-head serialization and concurrent/replayed
mutation rollback. Page+summary must remain coherent during concurrent writes;
queries must not change generation or durable ledger state. Future HTTP retains
the same public cursor contract. Collect this evidence on PostgreSQL, including
fresh/upgrade Flyway migrations, real concurrent connections, timeout/deadlock/
uncertain-commit recovery, page/summary coherence and isolated dump/restore.
No H2 substitute establishes it. None of these checks has been executed.

Additional boundary fixtures: accept amount 1 and 10^12, reject 0 and 10^12+1;
input/note over 256 code points fails, edited note may contain digits; explicit
dates outside 1900-01-01–9999-12-31 fail, valid future financial dates are labels
not scheduled execution. No-op update consumes its action with changed=false,
unchanged revision/generation/totals. Type-only change exposes incompatible
category reset in preview; unknown fields and model proof/actor injection fail.
An edit-prompt reply after expiry/restart is resolved against retained context;
never reinterpret an expired field reply as a new purchase. Separately sent valid
purchase text remains an ordinary capture. Explicit commands route as controls.

## Smallest end-to-end acceptance journey

For the personal user/VND/Vietnam timezone, record income 25,000,000 and expense
45,000, retrieve by ID, correct expense to 40,000 and edit its financial date.
Restart and query the inclusive overview and both type total-plus-list views.
For a range containing both entries expect net 24,960,000. Request expense deletion:
no change until the target-bound human confirmation; afterward expense is zero.
Restore it: expense returns to 40,000 exactly once. Replay actions and exercise
income CRUD too; reconcile old/new periods after date edits. Rehearse isolated
backup restore under the later operational policy. Observe actual Telegram UX AND
callable service invariants; parser fixtures alone do not prove this outcome.
No web, Excel/export or LLM integration is needed for this journey.

One isolated fixture, range Oct 1–31, after income 25,000,000 (#41) and expense
45,000 (#42 revision 1):

| Action | Expected expense revision/state | Expense total / net VND |
| --- | --- | --- |
| Preview amount 40,000 and Date Oct 19, then Cancel | 1, unchanged active | 45,000 / 24,955,000 |
| Fresh preview then actual Save | 2, active; creation time unchanged | 40,000 / 24,960,000 |
| Request Delete only | 2, active | 40,000 / 24,960,000 |
| Actual Confirm Delete | 3, deleted; get offers Restore | 0 / 25,000,000 |
| Explicit Restore | 4, active | 40,000 / 24,960,000 |
| Replay completed Save/Delete/Restore | Each returns original receipt; current get still revision 4 | 40,000 / 24,960,000 |

Changing the financial date to a different queried month removes the contribution
from the old month and adds it to the new month, without changing createdAt.
Exercise income correction/delete/restore through the same contracts, not only expenses.

## Later acceptance

R18–R19 belong to the [future adapter slice](../adapters/README.md#acceptance),
not ledger release gates. Budget, classification and allocation evidence lives in
the owning feature slice linked from the [feature map](../README.md). This avoids
turning all future features into one first-release checklist.

## Evidence disposition

All ledger R1–R17 and future adapter R18–R19 checks are unrun, not product passes/failures. Documentation checks and
limits live in [CURRENT STATUS](../../WORK_STATUS.md). A future implementation handoff
must identify exact candidate/base, stopped writers, changed paths, environment,
reproducible checks/results and residual limits. Lead accepts the exact candidate;
passing tests or a diagram alone are not product acceptance.
