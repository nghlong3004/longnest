# Ledger — current first-release slices

Approved product behavior is in [Product](../../PRODUCT.md); exact shared input,
money/date and OOP contracts stay in [Domain](../../DOMAIN.md) and
[Services](../../SERVICES.md). [R1–R17](ACCEPTANCE.md) is the canonical ledger
acceptance source, moved here from the former root requirements document.
The full slices remain plans, not standalone implementation grants. Bootstrap and
pure input/domain handling and local create/get/recent JPA/Flyway code are accepted,
as are bounded local L2 edit/confirmation/delete/restore, L3 inclusive range/query
navigation code and Vietnamese UI;
no PostgreSQL-verified durable ledger base exists. Actual
owners/dispositions
belong in [CURRENT STATUS](../../WORK_STATUS.md).

## L1 — capture and retrieve a durable entry

**Outcome/example:** personal user sends `ăn trưa 45k` → saved ID/45,000 VND receipt
→ recent selection/get shows that entry after restart. Salary input saves income,
not an allocation plan. Ambiguous input writes nothing; duplicate transport retry
does not create another entry.

**Inputs/dependencies:** approved personal/VND/Vietnam/fast-save direction and
selected grammar/payload/JPA-Flyway/polling design contracts and mandatory
[backend conventions](../../BACKEND_RULES.md); PostgreSQL is approved, with no H2
default for dev/tests. Implementation authority and artifact/toolchain binding are
governed by CURRENT STATUS; integration evidence requires an authorized isolated
PostgreSQL instance.
Private identity/credentials are needed for a real Telegram run, not a new product
question. Local create/get/recent code is accepted for unit/proxy behavior only;
migration, real transaction/replay and reopen acceptance remains outstanding.

**Provisional scope:** capture/recent/get application methods, their Telegram path,
minimal durable JPA/PostgreSQL transaction/idempotency storage, Flyway SQL schema and focused tests. No budget,
classification, allocation, LLM or range-report work. Actual paths/base/owner must
be named on dispatch; Lead sequences shared service/domain/routing edits.

**Evidence/invariants:** collect real Telegram receipts, direct service assertions,
saved/reopened records, rejected unauthorized/ambiguous input and replay/restart
results (R1–R8/R16 as relevant). Money exact; success only after commit. **Open:**
only exact artifact binding/compatibility evidence and runtime identity/custody;
no open grammar/replay or future-finance policy choice blocks this slice.

## L2 — correct a financial date, safely delete and restore

**Outcome/example:** select #42, preview 45,000 → 40,000 and Date → `2026-10-19`,
Save → same ID/creation time; Cancel → unchanged. Request Delete → displayed target
→ actual user's Confirm → deleted view/Restore → active again exactly once.

**Inputs/dependencies:** exact accepted L1 candidate with IDs/get/recent,
revision/idempotency and ownership contracts, plus the selected PE1 contract below.
Durable integration acceptance still requires real PostgreSQL evidence. The
current explicit continuation authority permits bounded synthetic-only L2 code
over inspected stable local contracts without claiming that evidence; CURRENT
STATUS identifies the exact base and owner. Docs alone are not an implementation.

**Provisional scope:** transaction update/date/delete/restore methods, bounded
pending-action state, their UI and tests; no query UI overhaul or retention/purge
job. Shared Transaction/Services/adapter changes require a scoped Lead brief.

**Evidence/invariants:** R5–R8/R10–R12/R17, before/after/Cancel/stale action examples,
authentic target-bound delete proof and restore/replay/restart evidence. Financial
date is not creation time; active-state contributions change once. Ten-minute
actions and preview/Save are selected. Operational recovery/purge duration is not
chosen, so no purge job belongs in this slice; it does not block retained Restore.

## L3 — review an inclusive range without losing selectable records

**Outcome/example:** `/overview 2026-10-01 2026-10-31` shows full-range totals;
`/expenses` over that range shows expense total plus 10 selectable entries and
navigation. Edits/date changes/deletes/restores are reflected. Bad ranges clarify.

**Inputs/dependencies:** accepted local L2 candidate and frozen shared read/mutation
contracts for authorized synthetic-only code; runtime integration acceptance still
needs the unmet durable evidence. **Provisional scope:** aggregate/list queries, cursor validation,
Telegram range views and tests. Reuse get/recent and the same store; no separate
reporting database, Excel or web. Sequence shared LedgerQueryService/router changes.

**Evidence/invariants:** R9–R16; empty/equal/reversed/invalid ranges, Vietnam midnight,
multi-page totals, foreign cursor rejection and updated/deleted/restored records.
Totals cover all pages, not visible rows; summary and page share one snapshot.
Cursor expiry/restart/stale generation refreshes are selected in Services, not
open product choices. Package/query implementation details may follow the contract.

## Resolved CRUD completion decisions

**PE1 / T2 — selected review/save:** application-owned previewUpdate → immutable
before/after action → actual user's Save → update(actionId, proof). Commit never
accepts a new patch. Cancel/expiry/replacement/stale revision makes no financial
change; successful replay returns the original receipt. The future update tool
only previews. This is a Lead completion choice within Edit intent, not Human
approval of a new creation gate. [Services](../../SERVICES.md#action-lifecycle-and-replay)
owns exact payload/proof/expiry/no-op/error rules; do not reimplement them here.

**PE2 / T3 — selected retry identity:** same event/action replays one outcome;
distinct clear messages create distinct purchases. Two identical coffees can be
real. No semantic duplicate heuristic is enabled. Adding a five-minute look-alike
prompt would alter fast capture and requires a future product decision; do not
treat it as pending implementation work.

**Ready boundary:** these design inputs are complete enough to guide L1, then L2
and L3 under the named current authority. No durable ledger base or live evidence is
accepted yet. Artifact compatibility and actual UI/recovery must be demonstrated
on the real candidate. The slice folder is not assignment or release permission.
