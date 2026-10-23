# Proposed verification, release and operations

State: SDLC Lite plan, not executed tests, working configuration or release approval.
See [ledger acceptance](features/ledger/ACCEPTANCE.md), [Architecture](ARCHITECTURE.md) and
[Decisions](DECISIONS.md). [CURRENT STATUS](WORK_STATUS.md) names the authorized
bounded backend implementation and synthetic/local checks; no live release is authorized.
Backend work must also follow [Backend rules](BACKEND_RULES.md); its selected
JPA/Flyway conventions still need PostgreSQL evidence. Bootstrap compilation and
isolated managed virtual-thread smoke are accepted only as recorded in CURRENT STATUS.

## Documentation checks

Read relevant branches and inspect exact-case relative links/anchors, one status
source, balanced Markdown fences, consistent scope/grammar/examples, and C4
boundaries. Check target-layout labels without reading local secrets. Record actual
findings/limits in [Status](WORK_STATUS.md); do not claim diagram rendering, skill
discovery or application behavior from a text/link check. No new checker scripts
or diagram toolchain are needed for this small documentation tree.

## Future verification layers

1. **Domain fixtures:** table-driven accepted/rejected/clarification cases, exact
   amount arithmetic, lower/upper bounds, decimal/suffix forms, Unicode/whitespace,
   known aliases, contradictory signs, unsupported free-form dates and multiple entries.
   Also cover real/invalid/reversed/inclusive ranges, single-day and empty results,
   Asia/Ho_Chi_Minh boundaries, and validated explicit financial Date edits.
   Fixtures must assert normalized values and no-write outcomes, not just wording.
2. **Ledger integration:** transactional entry/revision/dedupe behavior across
   retry, concurrency, crashes, stale callbacks and storage failures. Verify owner
   filters for every CRUD/query action, needed pending-action expiry, separate
   transaction/creation dates, confirmed reversible deletion and update/delete/
   restore impact on totals. Call the same application services from a non-Telegram
   test adapter to prove financial rules are not tied to chat handlers.
   Use the required JPA/Hibernate adapter and PostgreSQL Flyway-migrated schema,
   synchronous acknowledgement and actual pinned Java 25/Boot/provider/pgJDBC/
   Flyway PostgreSQL-module combination; H2/compatibility-mode or mock-only success
   is not PostgreSQL persistence proof. No test DB is installed/available by this plan.
   After implementation and relevant execution authority, use isolated synthetic
   PostgreSQL data; no real-data connection is needed to develop the bot. Collect
   fresh-schema and upgrade/validate evidence; no Hibernate-generated schema as a
   substitute. Check separate business revision/@Version, head lock/timeout and
   optimistic conflicts, operation uniqueness and atomic action/inbox/result rollback.
   Exercise separate connections, actual blocking row locks, deadlock/lock timeout,
   failed transaction cleanup, disconnected/uncertain commit and same-operation
   recovery. Verify DATE/TIMESTAMPTZ mapping, exact aggregate projections, sequence
   gaps without ID reuse, and Flyway-owned keyset/filter indexes on PostgreSQL.
   Test Save with a changed replacement patch rejected, canceled/replaced/expired
   previews, no-op revisions, original-outcome replay after later edits, confirmed
   rollback versus unknown commit, and two distinct equal purchases saving twice.
   Check same-snapshot page/summary plus cursor expiry/restart/generation refresh.
   Check DTO mapping/absent patch fields, service-contract errors and actual managed
   virtual-thread dispatch/context. Verify finite connection-pool behavior and
   keep-alive/shutdown; do not infer these from a configuration property alone.
3. **Telegram journey:** approved test identity and isolated data; clear immediate
   saves, saved IDs, read/update/date-edit/confirmed-delete/restore, inclusive
   overview/type totals and approved 10-entry pages with bounded entry navigation
   and a restart. Observe actual chat usability. No bot token, live message or user
   data is authorized by this plan.
4. **Recovery/security:** rehearse isolated backup restore and reconcile IDs,
   revisions and totals; check redacted logs, unauthorized callbacks, environment
   exclusion and secret handling. Verify the single personal identity is enforced
   on service calls, queries, cursors and confirmations, not just initial chat access.

Future interpreter validation is separate: [R18–R19](features/adapters/README.md#acceptance) compare both strategies against
the same typed [service/tool contracts](SERVICES.md), missing-fact behavior and
host-only delete proof. No LLM execution or external financial-text transmission
belongs in current release validation or this documentation task.

Future finance checks belong to the relevant [slice](features/README.md), not R1–R17:
classification override/explanation without changed money; budget recalculation
for financial-date, policy and transaction revisions; feasible/infeasible allocation
math and stale salary/rule snapshots. Exercise each through a usable Telegram path,
not an isolated architecture layer. A saved expense with unavailable budget feedback
must still report saved success, and a retry must not post it again. Advice/settings
must not alter ledger totals. These are future evidence requirements, not checks run.

## Candidate acceptance and release

An implementation handoff should identify an immutable candidate/base, changed
paths, stopped writers, environment and reproducible evidence for R1–R17. Record
passes, failures, unrun checks and limits separately. Lead inspects the candidate,
explicitly accepts/rejects it and updates status; an accepted design is not a tested
product. Fixes require a named scope/owner and a new candidate.

After explicit implementation authority, artifact/toolchain selection and isolated
synthetic development do not require deciding every live operational/future-feature
policy. Before separately authorized live release: select a host, resolve settings and
privacy policy, rehearse backup/restore, record a release checklist and rollback
route, then perform approved external changes. Technical acceptance does not itself
authorize push, merge, deployment or credentials. Avoid automated migrations until
data compatibility/recovery are demonstrated; a binary rollback may be unsafe after
a schema change. Freeze writes and reconcile rather than blindly overwrite a ledger.

## Operations proposal

- One bot process and one application database initially; record start/stop/health and failed-write/duplicate
  counts without tokens, message text, notes or amounts in routine logs.
  JPA/Hibernate + Flyway over approved PostgreSQL and long polling are selected
  design. No public DB/admin exposure; host/port, database/schema, credentials,
  access roles and TLS/certificate policy remain unconfigured. Prefer a restricted
  application role and separately controlled migration/restore authority; do not
  log connection secrets or silently downgrade transport protection.
  A missing/unavailable established database/schema must not create an empty ledger.
  Distinguish explicit first initialization from reopening; recovery is never an
  automatic overwrite or truncation. Keep backup/retention policy separate from
  ten-minute action/cursor usability expiry. Do not mistake request/connection
  timeout for confirmed rollback; recover uncertain results by the same operation ID.
  Finite connection limits, lock/query deadlines and PostgreSQL fsync/synchronous
  commit settings require later binding/evidence; virtual threads do not remove them.
- Bind the approved single personal identity before enabling access; authorize
  every action, not just initial `/start`. Do not expose backups or raw ledger
  content via logs.
- Recoverable storage is approved; backup cadence/destination/retention, encryption
  key custody and recovery/purge durations still require operational choices. Scope
  includes transactions, required revision/pending-action/idempotency state and settings.
  Destination/key custody and recovery point/time targets are unknown. Backup is
  internal recovery, not a product export feature.
  Confirm backup erasure/retention expectations without reopening the approved
  reversible Delete behavior; restoring an old backup must not silently resurrect
  transactions deleted after it. Recovery reconciliation is part of that policy,
  not approved now.
- PostgreSQL recovery proposal: a complete logical application-database dump and
  isolated restore, including transactions, heads/sequences, inbox/actions/results,
  settings and Flyway history. [Official pg_dump guidance](https://www.postgresql.org/docs/current/backup-dump.html)
  describes a consistent database snapshot, not an application-file copy. Restore
  rehearsal must reconcile IDs/sequences, revisions/generation, totals, deletion and
  pending/idempotent actions, schema history and required roles/grants. PostgreSQL
  roles are not all carried by a single database dump: bind their recovery explicitly.
  No pg_dump/pg_restore, connection or backup is run now. PITR/replication/managed
  backup is not approved or required by this proposal; destination, retention,
  tooling versions and recovery objectives remain operational choices.
- On suspected corruption/duplicate posting, stop affected writes, preserve
  evidence without private data exposure and reconcile authoritative records.
  Operator actions require actual runtime authority, absent in this task.

## Environment convention proposal

No private `.env`/`.env.dev` file was read, created or changed. The explicit
[backend dev launcher](../scripts/README.md) now loads **root `.env.dev` only** using
a strict literal grammar, never shell sourcing/evaluation. The one-command launcher
packages with Maven/tests before executing Java; file-loaded runtime values are
applied only to Java, not Maven. Its implemented
precedence is application defaults → `.env.dev` → exported process environment
(including explicit empty values); it selects only the existing `ledger` profile.
Direct Spring startup does not import dotenv files. Compose's fixed-value
[dev definition](../deploy/README.md#what-environment-files-actually-configure) is
separate and does not load the application's `.env.dev` in its documented commands.
No database/backend runtime was started. Verify existing ignore rules before any
commit containing runtime material; private env files must stay excluded.

| Target path | Intended role |
| --- | --- |
| `.env.example` | Optional committed documentation of variable names and nonsecret placeholders; not a runtime source and not created in this task. |
| `.env` | Proposed private local base; never commit or copy into docs. Not read by the implemented launcher. |
| `.env.dev` | Private explicit backend-development values, loaded only by the dev launcher; never commit. Not a production loader. |

Broader still-proposed precedence, low → high: nonsecret application defaults → local
`.env` → `.env.dev` only for explicitly selected development mode → explicit process
environment. The additional `.env` base merge remains unimplemented, **not automatic
Spring Boot/Vite behavior**. There are no implicit shell sourcing steps. Missing
required identity/secret settings must fail safely, not default to an open bot.
VND is fixed for this release; unsupported currency overrides must be rejected,
not treated as unresolved setup. Business timezone defaults to settled
`Asia/Ho_Chi_Minh`, not the host value; invalid overrides must fail safely.
Telegram's implemented opt-in binding uses `LONGNEST_TELEGRAM_ENABLED`,
`LONGNEST_TELEGRAM_TOKEN` and numeric `LONGNEST_TELEGRAM_USER`; the launcher passes
these file-loaded values only to Java with the documented inherited-env precedence.
Default networking is off. Future model/provider names remain to be chosen;
current variable names and activation limits are documented in the launcher.
No real values belong here, and configuration availability is not live acceptance.

Production should use a separately approved runtime secret-injection mechanism,
not development files. Later client configuration must contain public values only;
Telegram tokens and ledger secrets must never enter a browser bundle. No secret
store, production environment or deployment automation has been configured.
