# Longnest — current work status

Updated: 2026-10-23. Sole status source: `docs/WORK_STATUS.md`.
Reading entrypoint: [README](../README.md); [agent routing](../AGENTS.md).
**Bootstrap, pure input/domain and local service/store code accepted; PostgreSQL
runtime unverified, no working ledger/bot yet.**

## Current operation — deterministic L1–L3 code accepted locally; runtime evidence outstanding

### DEV-FIRST-USE-1 — ACCEPT guarded single-command startup

Publication is now explicitly assigned for this accepted candidate only. Same
Lead reconciled released/idle Peers and unchanged ownership; no source writer is
active. Fresh manifest/archive digests and all22 hashes reproduce, with exactly23
related public paths and no unrelated/untracked outgoing content. Reuse recorded
648 Java tests,63 synthetic launcher tests and isolated smoke; none rerun for Git
publication. Author/committer Long, branchmain/upstreamorigin-main and authorized
origin git@github.com:nghlong3004/longnest.git were inspected; independent remote
main still equals base3048e11. Private env/generated/private state remain excluded.
A separate pre-existing stopped Java job was observed in apps/server; no arguments/
private env were inspected and no process was started/resumed/stopped by this Git
task. Leave unrelated operator runtime state untouched; frozen source hashes match.
Normal commit/push outcome follows without a postcommit status-only commit loop.

Final Lead disposition: ACCEPT repaired22-path source/docs manifest
`apps/server/target/dev-first-use-evidence.4WTpty/candidate-final.sha256`, SHA-256
`404164165f93a1fa9b8ed1a4c708ab64d1aa1184418e0b7065af4bd8189d3224`.
Archive `candidate-final.tar.gz` in that directory, SHA-256
`9e4a237a2cd9bd9b76d6cb33d82c651deb8fd0441da29f7007f6b38f083d3144`.
Base remains3048e118634d6929a096a69a34c440e5b1ea4b65; WORK_STATUS is the23rd
changed public path, excluded from the frozen manifest for status updates. All
writers/builds/fixtures are stopped. Peer handoff is closed with no remaining
assignment; Lead owns the stopped source. No stage/commit/push is requested/taken.
The following progress/rejected-candidate notes are historical checkpoints.

Usable operator path: `bash scripts/dev-server.sh` only. Literal root .env.dev and
inherited precedence are retained. Builds/tests first, ensures local Docker/Compose
PostgreSQL running/ready, uses one-shot migrated/validated transactional owner
preparation, then execs normal ledger backend. No extra mode/manual exports/profile
switch/JAR handling. Config contract: exactly
`jdbc:postgresql://127.0.0.1:54329/NAME`; NAME/user are ASCII identifiers up to63
characters. Derive internal LONGNEST_DEV_DB_NAME from URL; effective datasource
username/password drive Compose interpolation and Java consistently. Compose
always uses absolute root/.env.dev and deploy/compose.dev.yaml, projectlongnest-dev.
Only absent/matching stopped postgres starts with --no-recreate. Existing running
service is never restarted for readiness; mismatched container configuration,
remote Docker/JDBC endpoints, Docker access/exec errors or readiness expiry stop.
Readiness limit60s; Docker default30s/start180s, plus forced-kill grace2s. No host
daemon startup, permission changes, mounts, reset/removal or production provisioning.

Preflight excludes both integrations. Exclusive locked storage accepts exactly one
matching owner, or initializes generation zero only if all five ledger tables are
empty with durability on. Storage/query/migration/owner or uncertain-commit failure
never means fresh state or starts normal backend; no destructive recovery. Direct
ordinary runtime remains non-seeding. Existing diagnostic strict init is retained
but not required. Local flock persists across phases/final exec; cross-checkout/
distributed bot exclusion remains operator responsibility. Ctrl-C/error does not
stop/remove PostgreSQL; same-container stop/start/restart preserves data, whereas
deliberate removal/recreation loses this disposable cluster. No real finance-data
durability claim follows from container-layer storage.

Final checks:63/63 launcher synthetic cases in `launcher-repaired.log`, including
both Peer findings and stopped/nonzero Maven distinctions;648 Java package tests,
zero failures/errors/skips, and isolated smoke exit0. Reuse package/smoke only after
fresh hash verification that all10 changed server paths remain identical to that
tested snapshot; repairs affect shell/tests/public operational docs only. Both Peer
findings are closed by Lead repair/regression evidence, not by the rejected
candidate's prior passes. Fresh syntax/whitespace,22 source hashes,23 public paths,
106 local doc targets, Compose contract/mount/secret-like scans and ignored/untracked
private env checks pass. Env export cannot overwrite resolved helper controls;
colliding original env names are still passed literally. No .env/.env.dev values
were read/changed, no real Docker/DB/provider/Telegram/network/paid call occurred.
No new schema/dependency, financial/UI/tool/memory/confirmation behavior changes.

Complete changed paths: `.env.example`, `.gitignore`, `AGENTS.md`, `README.md`,
`apps/server/README.md`; server Java `ledger/config/LedgerInitConfiguration.java`,
`ledger/repository/JpaLedgerStore.java`, `ledger/repository/LedgerStore.java`,
`ledger/service/TransactionService.java`, `ledger/service/impl/TransactionServiceImpl.java`;
tests `ledger/config/LedgerInitConfigurationTests.java`,
`ledger/repository/JpaLedgerStoreUnitTests.java`, `ledger/service/LedgerProxyTests.java`,
`ledger/service/LedgerUnitTests.java`; `deploy/README.md`, `deploy/compose.dev.yaml`,
`docs/DECISIONS.md`, `docs/DELIVERY.md`, `docs/SERVICES.md`, `docs/WORK_STATUS.md`,
`scripts/README.md`, `scripts/dev-server.sh`, `scripts/dev-server.test.mjs`.
Residual limits: mocks/source checks do not verify actual Docker context/plugin/
template/image behavior, PostgreSQL migration/locks/durability/reopen or live bot/
provider quality/latency/cost. The operator's original Jansi failure exit status
is still unknown; repaired stop-status handling is not proof of its root cause.
No permissions/credential/global Maven settings were changed. Passing local
checks are not live activation authority; default-off integration flags remain.

Lead `39abd32c-52fa-4cdf-82b4-fa507677fd06` owns launcher, bounded startup/init
service/store wiring, synthetic fixtures and affected public guidance from base
`3048e118634d6929a096a69a34c440e5b1ea4b65`. Same session/room reconciled;
prior Peers remain released/idle, no concurrent writer or new assignment. Preserve
the intentional post-publication status note; no other pending source changes.
Authorized outcome: the existing no-argument dev command loads .env.dev, builds/
tests, prepares genuinely fresh storage or verifies its configured owner, then
runs normally. This supersedes separate explicit init as a dev prerequisite only.
Extended authority includes managed dev Compose startup/readiness in this same
launcher, superseding separate Docker startup for this workflow only. Effective
datasource names drive container credentials and a strictly local JDBC URL supplies
its database name. Reuse/start only the named dev service without recreation;
refuse mismatched existing container configuration, remote endpoints and readiness/
Docker errors. All Docker/DB effects in verification remain synthetic fakes.
Preparation uses authoritative locked storage and existing transaction/durability/
empty-table checks, not exception-driven missing-owner inference; uncertain/failing
migration/init stops startup before integrations. Keep direct normal runtime
non-seeding and explicit diagnostic init compatible. Checkout-local launcher
exclusion prevents overlapping local runs; no distributed bot lease is selected.
Verify mock-storage/profile/transaction cases and fake-tool launcher sequencing,
failure/interruption/concurrency/env secrecy, then freeze a bounded candidate.
No private env access/edit, actual DB/provider/Telegram/container execution, paid calls,
new schema/framework, Git commit/push or destructive reset is assigned.

Moving implementation also reproduces a phase-runner defect independently with
shell-only fakes: job-control plain wait returned147 for a stopped job whose final
exit was0. Use wait-f for true termination and numeric build diagnostics; true
nonzero exits still fail. This corrects the owned runner, not a proven diagnosis
of the operator's Jansi exception. Repository401/Mockito warnings and BUILD SUCCESS
text do not establish Maven exit status; original operator status remains unknown.
Focused Java checks pass78 tests. First expanded launcher run58/59 was blocked by
ENOSPC copying a host utility into a synthetic fixture; replaced fixture copies
with symlinks and moved new evidence/fixtures to ignored target on the spacious
project filesystem, preserving all prior files. Subsequent59/59 pass; final
runner regressions now pass61/61. No real Docker, Maven-network or private env
was involved in launcher checks. Full offline Java package is running locally.

Source/docs candidate frozen:22 public implementation paths in
`apps/server/target/dev-first-use-evidence.4WTpty/candidate.sha256`, SHA-256
`2056bcd5203153d43e80f81e0078b419303224d9e4f433d01a076b942a77ef46`;
WORK_STATUS excluded for evidence updates. Fresh read-only Peer
`4f388fb9-41fc-4066-80a6-54fb849e8bba` inspects state/env/failure/no-recreate/
owner/integration ordering with no writes/builds/private env or external calls.
Selected current slp-peer/gpt-6.1-sol/medium profile; no read-only runtime mode
exists, so auto plus explicit read-only scope. Lead retains every write scope and
will not change listed files until inspection closes. No dependent assignment.

Exact frozen candidate verification now passes:61 launcher cases (fake Docker/
Maven/Java),648 Java package cases with zero failures/errors/skips, isolated smoke
exit0 with non-web context/managed virtual-thread execution, bash syntax and
whitespace. Evidence under `apps/server/target/dev-first-use-evidence.4WTpty/`:
`launcher-final.log`, `package.log`, `smoke.log`; prior focused log at
`/tmp/longnest-dev-first-use-focused.log` (78 cases). Offline existing Maven cache,
Java25.0.4.1/Maven3.10.0, isolated env/empty user and global settings; no private
file values reach builds. Fresh22-path hashes plus23 public path/content checks,
106 local doc targets, Compose interpolation/mount boundaries and ignored/untracked
private env checks pass. Pattern scan is not exhaustive secret detection. All
local build/fixture writers finished; review still pending, so no final acceptance.

Peer submitted/stopped with all22 hashes unchanged. ACCEPT both P2 findings and
REJECT2056bcd5...ef46 for final acceptance: arbitrary env exports could replace
Docker helper's validated tools/database/timeout controls; TERM-ignoring clients
could exceed a timeout without forced-kill escalation. No additional init or
integration-ordering defect found statically. Peer loop explicitly closed; no
write ownership acquired. Lead owns only these repairs/regressions and matching
docs from the rejected snapshot; its source freeze is lifted for Lead alone.
Freeze argv/derived name before exports, remove script-array attributes in the
Docker subshell, and use timeout kill-after2. New tests cover inherited/file control
collisions and a TERM-ignoring fake Docker. Java source remains unchanged, so its
648-test/package and isolated-smoke evidence can be reused after hash verification.

### Publication — accepted amount clarification and native fallback

Published commit `3048e118634d6929a096a69a34c440e5b1ea4b65`
(`Complete amount clarification and rules-first native fallback`) includes the19
reviewed public paths for both accepted increments. Normal SSH push to
`git@github.com:nghlong3004/longnest.git`, `main:main`, succeeded. Independent
post-push ls-remote confirms this exact hash at refs/heads/main; local main and
origin/main match, divergence0/0, with origin/main upstream. Prior635-test/package
and isolated smoke evidence is reused against freshly verified unchanged source;
no tests/builds or app provider/DB/Telegram calls were made during publication.
No private env/generated state was staged, no force/credential/permission change
occurred. Only this post-publication status note remains intentionally uncommitted;
no status-only amend/commit loop. Live language quality, latency/cost, PostgreSQL
durability and live Telegram UX remain unverified; integrations stay default-off.

Explicit publication authority now covers AMOUNT-CLARIFICATION-1 plus
RULES-LLM-ROUTING-1 only. Same Lead owns publication; all source writers and
read-only Peers are stopped/released. Fresh checks reproduce all109 accepted
server hashes and final manifest/archive digests below. Reuse the recorded635-test
package and isolated smoke passes; no build or live integration was rerun.
Reviewed19 related public paths, including both new synthetic test files and the
prior intentional status note. Private env files remain ignored/untracked; no
private/generated path or credential-like public content was flagged (not an
exhaustive secret guarantee). Configured author/committer are Long; branch main,
upstream origin/main and origin git@github.com:nghlong3004/longnest.git match the
authorized destination. Independent SSH ls-remote confirms remote main still at
base24036d5. Normal commit/push outcome will be recorded after publication without
a postcommit status-only amend/commit loop. Historical local acceptance details
below describe the frozen pre-publication state; no feature changes were reopened.

### RULES-LLM-ROUTING-1 — ACCEPT consistent fallback

Lead `39abd32c-52fa-4cdf-82b4-fa507677fd06` reopens input recognition metadata,
native/pending routing and argument guards, affected synthetic fixtures and docs.
Accepted input is AMOUNT-CLARIFICATION-1 manifest `6f585094...2d2e9bfe` on Git
base `24036d5`; all 109 file hashes matched before writes. Same Lead; both prior
read-only Peers are released/idle with no writers. Accepted local changes and
the prior post-push status note remain intact; no unrelated scope is taken.
Outcome: successful deterministic capture/commands/controls stay local; parser
recognition failures reach native interpretation/clarification when enabled.
Separate recognition failure from validated financial/safety failure, preserve
default-off diagnostics, pending-source guarantees, service/auth/replay/storage/
actual-user proof boundaries. No new tool, broader financial policy, longer memory,
bespoke interpreter, private env, live/paid call or Git action. Lead owns local
verification and acceptance; no Peer write assignment is opened.

Candidate frozen for read-only inspection: server manifest
`apps/server/target/rules-llm-candidate-server.sha256`, SHA-256
`9337698bacd2a787c4156b2cec1014e22354e01378409c6996ed40f701fe3b23`.
Two intermediate package checks passed609 tests; final snapshot additionally reuses
the existing native magnitude conversion for local pending replies with three
synthetic cases. Selected current `slp-peer`/gpt-6.1-sol/medium profile; no read-only
runtime mode exists, so auto plus explicit read-only/no-build brief. Lead retains
all writes and freezes this candidate during inspection; builds/evidence may run.
Read-only Peer `2bb1f76f-5897-4147-b065-133f28fb22f1` is inspecting that snapshot;
no write ownership is transferred. Exact candidate package passes612 tests.
Peer submitted/stopped with all109 hashes matching. ACCEPT four static findings
and REJECT `9337698...fe3b23` for final acceptance: signed conflicting cues hidden
by numeric note grammar (P1); Unicode separator partial-capture gap (P2); supported
native magnitude spellings still tagged validation (P2); safe tabs still tagged
validation (P2). No extra pending/auth/storage/proof defect found. Lead owns exactly
these repairs/regressions from that snapshot; source freeze lifted for Lead only,
Peer loop closed with no further assignment or write ownership.

Final Lead disposition: ACCEPT repaired 109-file server snapshot
`apps/server/target/rules-llm-candidate-server-final.sha256`, SHA-256
`10cca82ee8953d2d0b50aaa03605739618f4b4e1cc4d85adc2c948a62b7dded6`.
Archive `apps/server/target/rules-llm-candidate-server-final.tar.gz`, SHA-256
`583686a5ff8a38f705a12f6728f148a15ab20e352d7167ebe26e52237214d2d3`.
All source hashes reproduce; writers/builds stopped. Peer findings are closed by
the named Lead repairs and dedicated passing regressions, not by reusing the
rejected candidate's passing tests. No Peer retains an assignment/write scope.

Exact routing: internal Problem.failureKind separates RECOGNITION from VALIDATION
without changing default-off legacy statuses/codes/grammar. Unsupported grammar,
extra prose, valid date prefixes, native magnitude wording, digits in notes,
safe conversational whitespace and incomplete intent reach the existing native
ChatClient/tools when enabled. Examples: `ăn trưa hết 45k`, `45nghìn cho bữa trưa`,
`ăn trưa 45,5 triệu`, `-45k đồ dùng phòng 2`, `xem\tchi tháng này`.
Known invalid/ambiguous Money/currency, contradictory signed type cues and unsafe/
domain-invalid fields remain local. Native text keeps the existing1024-code-point
bound; Note/Money/date domain bounds are still independently enforced. Ordinary
line/tab breaks may reach native reads/clarification; the complete existing
transaction-separator definition (including Unicode line/paragraph separators)
prevents native create from silently selecting part of a multi-capture request.

Successful rules, protocol commands, field replies and authenticated user controls
remain local, including while canceled inference unwinds. Known local pending
amount completion remains local with original validated facts/date, absolute TTL,
identity/reset/eviction/source-event/question binding and single consumption intact.
It reuses existing native nghìn/ngàn/triệu conversion, not model arithmetic. When
no validated pending source exists, amount-only input continues to native missing-
fact clarification instead of stopping at a known-phrase gate. A guessed native
create for an amount-only message is host-converted to Missing.TYPE; a missing
current amount cannot be invented. No history-derived pending financial draft
or broader capture workflow is installed; restate complete current source facts
when a prose clarification has no validated pending source. Foreign currency
before/after a literal, partial arithmetic and contradictory ASCII signed source
types cannot be normalized into a valid-looking create. Signed cue checking is
independent of local note grammar/raw length and normalizes NFC/whitespace.

Recovery/storage failures and authorization never fall through into interpretation;
all proposed financial values use existing mappers/services and actual-user proof
contracts. Unsupported financial requests may clarify UNSUPPORTED, never gain a
new capability. No callback, schema/dependency/configuration, output-token option,
memory duration/capacity, model loop, budget/allocation policy, category query or
multi-field editing change. Model input/history coverage is broader when enabled,
so actual request/egress/cost exposure can increase; activation authority is not.

Verification: Java25.0.4.1/Maven3.10.0, isolated environment, offline existing
cache and empty user/global settings. `apps/server/target/` evidence:
`rules-llm-repaired-focused.log`329 passing focused cases;
`rules-llm-package-repaired.log` **635 tests, zero failures/errors/skips**
(accepted baseline564 +71); `rules-llm-smoke-final.log` exit0, non-web context,
managed virtual-thread execution/closure. Earlier focused/package passes and
the rejected snapshot/archive remain separate evidence. New fake-model fixtures
exercise formerly rejected grammar through real ChatClient/native callbacks,
current literal/source/type/currency/amount/date validation, missing facts and
unsupported requests. Existing unauthorized, storage-failure/frozen-delivery
recovery, direct controls and full pending-capture integration regressions pass.
All four Peer findings have passing targeted regressions; native direct-call tests
also protect signed cue conflict beyond the finite parser's raw input limit.
No real provider, Telegram, PostgreSQL or paid call was made. Existing nonfatal
SDK/Lombok/Mockito warnings remain; this is not live language or DB evidence.

Increment since accepted AMOUNT snapshot: 11 server paths changed, 98 unchanged,
none added/removed. Server paths: `apps/server/README.md`; Java
`conversation/ConversationAssistant.java`, `conversation/LedgerTools.java`,
`input/InterpretationResult.java`, `input/TransactionInterpreter.java`,
`telegram/adapter/TelegramCapture.java`; tests
`conversation/AmountClarificationTests.java`,
`conversation/ConversationAssistantTests.java`, `input/TransactionInterpreterTests.java`,
`telegram/TelegramAdapterTests.java`, `telegram/TelegramAmountClarificationTests.java`.
Other changed paths: `AGENTS.md`, `docs/DECISIONS.md`, `docs/DOMAIN.md`,
`docs/PRODUCT.md`, `docs/SERVICES.md`, `docs/WORK_STATUS.md`.
Combined worktree has19 public changed paths, including the retained prior amount
increment's ConversationMemory/resource edits and its two new test files. Static
checks: seven Markdown docs/90 local targets,110 unique resource keys, whitespace,
ignore/tracking and credential-like content/path scan pass (not a universal secret
detection guarantee). Private `.env`/`.env.dev` remain ignored/untracked with no
private-file access/edit; nothing staged and HEAD remains `24036d5`. No commit,
push, external call, activation or unrelated work was taken. Exact source is
frozen for handoff. Actual semantic intent, Vietnamese/model accuracy, latency/
cost, prompt-injection robustness, PostgreSQL durability and live Telegram UX
remain unverified; neither broad routing nor valid arguments prove those qualities.

### AMOUNT-CLARIFICATION-1 — ACCEPT bounded local completion

Lead `39abd32c-52fa-4cdf-82b4-fa507677fd06` owns the cohesive local input,
conversation memory/dispatch, Telegram routing, synthetic tests and affected docs.
Base `24036d5ca2a579586b96f1f0e18c2bebc317329b`; only the intentional prior
post-push status note was dirty. Same Lead/current room reconciled; prior review
Peer is released, with no active writer. No delegation or concurrent scope.
Outcome: known local phrase missing only amount → localized question → exact
amount-only reply → authoritative create preserving validated source facts/date.
No model-derived pending facts, schema, new tool, longer/persistent memory or
proposals 2/3. Verify invalid replies, abandonment, expiry/reset/isolation and
trusted-event replay with local fixtures, then freeze/dispose exact candidate.
No private-env changes, provider/DB/Telegram/paid calls, activation or Git action.

Lead source candidate frozen for independent inspection: 109-file server manifest
`apps/server/target/amount-candidate-server.sha256`, SHA-256
`1323e80d442e484b11d785c95413b76c90c23e96db6e6a3edcb9f3110475c0b6`.
`amount-package.log`: 560 tests passed, no failures/errors/skips. A fresh read-only
Peer brief asks about consumption/replay/expiry and adapter/control routing, not
new features. Selected configured `slp-peer`/gpt-6.1-sol/medium; provider lacks a
read-only runtime mode, so normal mode with explicit no-write/no-build scope.
Read-only Peer `a265b6b5-f826-41a7-9aef-595be1446c91` has no write scope;
Lead alone retains all writes; server source remains frozen during inspection.

Read-only Peer submitted/stopped with all 109 hashes matching; ACCEPT its two
findings and REJECT snapshot `1323e80d...75c0b6` for final acceptance: an explicit
reply to an old amount question can consume a replacement source (P1); pre-routing
busy check can block deterministic capture/commands after model timeout (P2).
No other important defect reported; static inspection, not executed fixtures.
Lead owns both repairs and regressions from that exact snapshot; Peer has no
further work/writes. Source freeze is lifted only for this named Lead repair.

Final Lead disposition: ACCEPT exact repaired 109-file server manifest
`apps/server/target/amount-candidate-server-final.sha256`, SHA-256
`6f58509413c1db34be3cfc8f2e74e51e286d1e7cf08322be3bde239b2d2e9bfe`.
Archive `apps/server/target/amount-candidate-server-final.tar.gz`, SHA-256
`0a101cc73fc453a294ea926c71af98e68c4e269ef2cf2832dbdbd591d1bb20d3`.
Implementation/build writers stopped; no Peer owns writes. The rejected original
manifest/archive and its verification logs remain separate, not promoted evidence.

Precise behavior: only an exact existing local phrase/alias lacking amount can
establish type/category/normalized note and original Vietnam financial date.
`ăn trưa` → localized amount question → unsigned exact `45k` → shared create and
service-authoritative receipt preserving `ăn trưa`, EXPENSE/FOOD and original date.
No model call or invented placeholder/source fact. Bare `45` remains 45 VND.
The authenticated completion event/timestamp owns ingest; source facts expire at
an absolute ten-minute deadline inside the unchanged idle/context/reset boundary.
Recognized numeric amount-like invalid replies ask again without write/inference;
other requests abandon it. New amountless source replaces it; clear complete
capture stays immediate. Commands/non-text/valid callbacks/field replies abandon
pending capture without gating their existing routes on model busy. Explicit
amount replies to stale conversational questions clarify without consuming the
replacement; only the latest bound amount question or an unthreaded reply can
complete the current source. Expiry, reset, eviction/restart forget unsaved state.

Consume source before dispatch. Same-event durable recovery precedes ephemeral
completion; if nothing has frozen, a same-event cached attempt preserves exact
input until success, source deadline or abandonment. Another amount event cannot
reuse it. At most eight source-event tombstones per current context lifetime:
duplicates do not rearm or extend deadlines; once full, ask for a complete capture
instead of evicting IDs. These IDs/question bindings are ephemeral, not durable
prompt deduplication across reset/expiry/restart. Completed creates keep ordinary
durable event recovery. No semantic duplicate suppression for new full messages.

Verification: isolated env, Java25.0.4.1/Maven3.10.0, offline existing cache and
empty user/global settings as in the previous accepted increment. Local logs under
`apps/server/target/`: `amount-package-final.log` **564 tests passed, zero failures/
errors/skips** (baseline530 +34); `amount-smoke-final.log` exit0, non-web context,
managed virtual-thread execution and closure. New tests cover exact facts, income/
bare dong, midnight, invalid amounts, unrelated/complete/new source, identity,
absolute expiry/reset/eviction/restart, failed-ingest retry input, unavailable
recovery, real service/mapper/renderer over mock store, committed-receipt delivery
failure/recovery without second insert, stale/current-question association and
deterministic routes while an interrupted fake model ignores cancellation.
Existing single-tool, confirmation, replay, SDK output-default and memory checks
still pass. This is mock-port evidence, not real transaction/durability or live UX.
Initial focused failures were test-only expectations (foreign principal uses
LedgerException; configured Mockito cannot mock a final receipt record); repaired
using actual error contract/receipt values. `amount-focused.log` preserves those
failures; `amount-integrated.log`/`amount-repaired.log` preserve intermediate passes.
Both Peer findings have dedicated passing regression fixtures in the final package.

Static verification: 16 changed public paths, seven Markdown docs/90 local targets,
110 unique Vietnamese resource keys, whitespace and all server hashes pass.
No private/generated/credential-like outgoing path/content flagged; pattern scans
are not universal secret detection. Private `.env`/`.env.dev` remain ignored and
untracked, with no private-file read/edit in this increment. Nothing staged; HEAD
remains base `24036d5`. No Git commit/push, network/provider/Telegram/DB/paid effect.
No schema/dependency/configuration, memory-duration/capacity, new native tool,
multi-tool policy, category query or multi-field-edit change. Existing nonblocking
deterministic routing and actual-user Save/Delete/Restore contracts are preserved.

Complete changed paths: `AGENTS.md`; `apps/server/README.md`; server Java
`conversation/ConversationAssistant.java`, `conversation/ConversationMemory.java`,
`conversation/LedgerTools.java`, `input/TransactionInterpreter.java`,
`telegram/adapter/TelegramCapture.java`; `messages_vi.properties`; existing test
`input/TransactionInterpreterTests.java`; new tests
`conversation/AmountClarificationTests.java`,
`telegram/TelegramAmountClarificationTests.java`; `docs/DECISIONS.md`,
`docs/DOMAIN.md`, `docs/PRODUCT.md`, `docs/SERVICES.md`, `docs/WORK_STATUS.md`.
The prior intentional post-publication status note is preserved. All source is
frozen/stopped for local handoff; no dependent assignment or active Peer remains.
Usable within the existing default-off native binding, with no model call for
local clarification. Arbitrary prose/date-bearing/signed fragments and broader
missing-fact workflows remain unsupported. No live language quality, PostgreSQL
durability or Telegram runtime readiness is claimed; existing authority unchanged.

### CONVERSATION-POLISH-1 — ACCEPT provider-default output and presentation-only name

Lead `39abd32c-52fa-4cdf-82b4-fa507677fd06` owns conversation configuration/prompt,
narrow Telegram presentation/dispatch, affected tests/resources and public docs.
Base: `199afb045f57850b594f14d5bc34841994e06254`, accepted server manifest
`b698106aa4800cf4fed7c5940d3296f70fbe77b4d6009e15d7759d1ec7a436de`.
Current room reconciled: same Lead, no active Peer writer or pending scope transfer;
prior read-only Peer remains released. Existing `.env.example` and setup/status docs
changes are preserved inputs, not recreated. Private env files are out of scope.

Outcome: leave output tokens unset in native SDK options and serialized requests;
provider/model defaults are variable/unknown, not unlimited or guaranteed cheaper.
No replacement numeric cap or mapping of legacy DEEPSEEK_MAX_TOKENS. Keep single
tool, concurrency/rate/deadlines/retry/default-off and no-live boundaries unchanged.
Product is quick personal-finance capture/questions, not extended social chat;
existing short bounded memory suffices. Authenticated Telegram first/last display
name is sanitized/bounded in presentation only for localized clarification,
never principal/scope/event/proof/tool input, model instructions or stored memory.
No preferred-nickname setting, persistence or new context machinery is required.

Acceptance: actual SDK in-process fixture must omit output-token fields; synthetic
rendering/adapter checks prove bounded literal Unicode names, neutral fallback,
unchanged numeric-user authorization and no new model-name egress. Offline package
and isolated smoke, then stable candidate/disposition. After technical acceptance,
related safe public env/docs plus this increment may be committed and normally
pushed to the existing approved origin/main, after destination/identity/content
checks. No runtime/provider/DB/Telegram/paid calls; no private env reads or edits.

Stable Lead handoff/disposition: ACCEPT exact 107-file server manifest
`ecb13ea051829f1a825b702b165f848c18f4944d0848e4ab007b0981d41a29b4`.
Evidence root: `apps/server/target/conversation-polish-evidence/` (ignored generated
evidence only); `candidate-server.sha256` and `candidate-server.tar.gz`, archive
SHA-256 `324010c1b5629f50e26dd2ec92af87ae3936dbadad352b41dd9a752251ae4a32`.
Related public `.env.example` reviewed at SHA-256
`6a606e4b877e412f1d2f17db903b5342655040dfdc1fe5f83355a89500b2a7a9`:
synthetic DB placeholders, integrations false, commented credential/user/model
placeholders and no token-cap setting. Prior env/setup-doc work is included as
explicitly authorized; private `.env`/`.env.dev` remain ignored and untouched.

Verification: isolated environment, Java25.0.4.1/Maven3.10.0, offline existing cache,
empty user/global Maven settings. `package.log`: **530 tests passed, zero failures/
errors/skips** (baseline526 + four presentation/adapter tests). `smoke.log` exit0:
non-web context, managed virtual-thread execution and closure. SDK in-process fake
HTTP fixture verifies null maxTokens and absence of max_tokens/max_completion_tokens/
max_output_tokens, while thinking remains disabled and eight tools/one request/
direct rendering remain. No network socket/provider call is used by that fixture.
Synthetic UI/adapter checks cover NFC/Unicode code-point bounds, control/bidi/markup
removal, missing-label fallback, literal apostrophes, foreign-ID impersonation
rejection, unchanged principal/scope and no display-name data in provider prompts.
All original single-tool, memory, replay and actual-user control tests still pass.
Initial focused run failed two test expectations (allowed digit retained; name
substring also matched the timezone); assertions corrected, full package passed.

Static checks: 21 Markdown documents/202 local targets, balanced fences, 108 unique
Vietnamese keys, whitespace, ignore/tracking and private-file metadata unchanged.
No new dependency, schema, HostContext field, nickname store or memory mechanism.
Ledger services, tool callbacks, transport authorization, conversation memory and
dev launcher source remain unchanged. Implementation/build writers are stopped;
candidate writes are frozen pending any explicitly assigned repair. No Peer write
assignment was opened. Existing compiler/Mockito/Lombok deprecation warnings remain.

Complete related paths: `.env.example`; `apps/server/README.md`; server Java
`conversation/ConversationConfiguration.java`, `conversation/ConversationAssistant.java`,
`telegram/adapter/TelegramCapture.java`, `telegram/mapper/TelegramReplies.java`;
`messages_vi.properties`; tests `ConversationConfigurationTests`, `UiMessagesTests`,
`TelegramAdapterTests`; `docs/DECISIONS.md`, `docs/DELIVERY.md`, `docs/PRODUCT.md`,
`docs/SERVICES.md`, `docs/WORK_STATUS.md`; `scripts/README.md`.

Usable outcome: enabled native requests now defer output length to the configured
provider/model; Telegram clarification may use the authenticated sender's sanitized
first/last name, e.g. `Minh Nguyễn, Bạn muốn xem ngày hoặc khoảng thời gian nào?`.
Names stay local to presentation, with neutral fallback; pages/receipts/proofs are
unchanged. Quick capture/questions and existing short memory are sufficient;
read-only multi-tool batches remain deferred, not selected by this change.
Actual provider-default length/cost/latency are unknown and variable, not unlimited
or necessarily cheaper. Live language quality, PostgreSQL durability and Telegram
runtime behavior remain unverified; default-off/live/paid/egress authority unchanged.
Configured identity/main/origin/origin-main were inspected for the authorized Git
publication; exact commit/push outcome follows without a status-only commit loop.

Publication completed: commit `24036d5ca2a579586b96f1f0e18c2bebc317329b`
(`Use provider-default output and safe Telegram display names`) contains the 16
related reviewed public paths, including the synthetic `.env.example`. Normal
SSH push to `git@github.com:nghlong3004/longnest.git`, `main:main`, succeeded;
independent `ls-remote` confirms that exact commit at `refs/heads/main`, and local
main/origin-main divergence is 0/0. Private env files and generated evidence were
not staged; no force push or credential change occurred. This post-publication
status record alone remains local, intentionally without another commit/amend.

### Environment preparation — completed 2026-10-23, local only

Lead `39abd32c-52fa-4cdf-82b4-fa507677fd06` owns this bounded `.env`/`.env.example`
and setup-docs change. Explicit private `.env` edit authority applies only here;
no broader private-file, runtime, credential-value or provider authority follows.
Created public root `.env.example` solely from public configuration: current
Compose synthetic DB defaults, required backend names, `ledger` profile, both
integrations false, exact DeepSeek base URL and commented token/user/key/model
placeholders. No private value was copied into the example or any project artifact.

Root `.env` was mechanically prepared with opaque value preservation. Only four
unambiguous key names were normalized: DB_USERNAME/DB_PASSWORD to the corresponding
SPRING_DATASOURCE names; DEEPSEEK_API_KEY/DEEPSEEK_MODEL to LONGNEST_DEEPSEEK names.
Every original assignment suffix/value and unrelated line is unchanged. Existing
identity/enablement choices and file mode remain; absent supported settings use
public synthetic defaults or commented optional placeholders. Legacy DB_URL and
DEEPSEEK_BASE_URL remain unchanged because their formats cannot be established
from names alone; DEEPSEEK_MAX_TOKENS has no native setting and is also retained.
Any former output cap belongs to its historical code snapshot; the current
CONVERSATION-POLISH-1 change leaves it unset without altering private comments or
values. New settings are not a claim that the private file is runtime-ready.

Verification: names-only inspection, opaque byte/assignment preservation guards,
single-link regular target/mode check, synthetic collision/duplicate/quoting tests
and idempotence passed. Initial synthetic idempotence failure was corrected before
any private write. `.env.dev` contents were never read or edited; inode/size/time/
mode metadata matched before/after. `.env` and `.env.dev` remain ignored/untracked;
the safe example is nonignored/untracked until an explicitly requested Git action.
No private values/diffs were emitted. Static example/format/ignore checks only;
no build, startup, DB/Telegram/provider/network/paid action, stage, commit or push.
Launcher still reads only `.env.dev`; `.env` and `.env.example` are not auto-loaded.
Setup docs now link the example; prior implementation/evidence stays unchanged.

### Git publication — completed 2026-10-23

Explicit destination authority: `git@github.com:nghlong3004/longnest.git`.
Lead `39abd32c-52fa-4cdf-82b4-fa507677fd06` verified configured identity
`Long <nghlong3004@gmail.com>`, current branch `main` and HEAD
`199afb045f57850b594f14d5bc34841994e06254`. No prior remote/upstream; only the
intentional post-commit WORK_STATUS note was dirty and nothing was staged.
All five reachable commits/217 unique blobs (2,729,483 bytes) were inspected for
private/generated paths and known credential-like literals; none were flagged.
No private files or dangling objects were included in the pushed branch history.
Pattern checks are not a guarantee that every possible secret format is detected.

Existing SSH access succeeded without credential/host-permission changes.
The destination initially advertised no refs. Added `origin` at the exact approved
SSH URL, then ordinary non-force `git push --set-upstream origin main:main`
succeeded as a new branch. Independent `git ls-remote origin refs/heads/main`
confirmed `199afb045f57850b594f14d5bc34841994e06254`; upstream is `origin/main`,
local tracking comparison is 0 ahead/0 behind. No other branch/tag was pushed,
history rewritten, deployment performed or runtime/provider activated.
Earlier no-remote/no-push statements below describe their historical checkpoints.
This publication/outcome note remains intentionally local and uncommitted; no
documentation-only commit loop. Accepted source and its existing evidence unchanged.

### NATIVE-AI-1 — ACCEPT native conversation, synthetic/local only

Lead `39abd32c-52fa-4cdf-82b4-fa507677fd06` owns the stable integrated candidate,
shared docs and final handoff. Predecessor `8442a05c-6421-45c4-adc7-65b64111c30f`
had no active turn and was archived before transfer; no old writer was restarted.
Base `0d76abf9d42163454b95411e8f8463389ad9ba7e`; prior status-only handoff checkpoint
`b100fbeadbe282b6cfc392c8686bab3a415f2c33`. No unrelated/private changes taken.

Outcome: default-off Spring AI 2.0.1 ChatClient, native @Tool callbacks and controlled
ToolCallingManager execution, bounded Spring MessageWindowChatMemory, grounded
Vietnam dates and isolated follow-ups. Existing deterministic clear capture,
commands/field prompts/controls, services, exact money, auth, revisions, replay and
actual-user Save/Delete/Cancel proof remain. Superseded bespoke JSON interpreters,
protocol results and fixtures are removed, not retained as unused fallback layers.
No POM/dependency/schema change, generic agent framework or persistence layer.

Settled meanings: THIS_MONTH is first day through the frozen host business date
(month-to-date, no later dates); LAST_MONTH is the whole previous calendar month.
FOOD is exactly active EXPENSE + Category.FOOD, not groceries or note search.
Follow-ups retain validated dimensions unless explicitly changed. No usable scope
means clarification. Natural reads reuse exact full-range summaries/pages and
existing current-entry/Edit/Delete navigation, without ledger-result/model egress.
Capture creates immediately when clear; tools only preview Edit/request Delete.
Restore is the existing authorized replay-safe mutation. Model Save/Confirm/Cancel,
proof/actor/operation-ID/SQL inputs and multi-callback execution are absent.

Ownership/dispositions:

- FOOD-QUERY-1 Peer `00acd16e-22cc-440d-82ed-79ffc04f6145` submitted/released nine
  paths, aggregate `7db1f8f904a21e0756672f34bddff9ce5fadee4366ed896ab7903925a6b589ff`.
  ACCEPT unchanged hashes: shared FOOD filtering/totals/pages/cursors and integrated
  tests pass. PostgreSQL execution remains unverified. No Peer writes reopened.
- NATIVE-AI-TEST-1 continued as Peer `0e88f928-28ce-4ddb-bd9a-131eeef79086` after
  failed predecessor was archived; explicit two-test-file assignment and current
  callback restored. Submitted/released `c3b47e6f1cb3f7186dc3147d34bc5b243f18a641c9cfbbb25f1e572a34035126`
  was REJECTED as complete: actual SDK fixture missing, fake options unstubbed.
  Lead explicitly took repair scope; retained memory tests, corrected configuration
  fixture and added actual in-process SDK/native request check. Peer acknowledged
  stopped writes/builds; repaired tests are part of the accepted Lead candidate.
- Fresh read-only Peer `43125d4d-8647-4424-bff7-72911d50085c` inspected exact 107-file
  snapshot `fb9d8fd5ebf8af6264d07ae9ca325630d45284d83b89aac1cb94a6a2532d7814`.
  Profile slp-peer/gpt-6.1-sol/medium selected after profile inspection; read-only
  runtime mode unavailable, so auto mode plus explicit no-write assignment used.
  Peer matched all hashes, found no proof/multi-call/result-egress bypass, and
  reported two P2 defects: canceled-before-start model task wedges busy state;
  explicit KEEP+FOOD after INCOME incorrectly clarifies. Lead accepted findings,
  REJECTED that snapshot for final acceptance and owned both repairs/tests.
  Peer acknowledged complete/no ownership. No review writer exists.

Final Lead disposition: ACCEPT exact **107-file server manifest**
`b698106aa4800cf4fed7c5940d3296f70fbe77b4d6009e15d7759d1ec7a436de`.
Manifest/archive: `/tmp/longnest-native-ai.HBq4sb/candidate-server-final.sha256`
and `candidate-server-final.tar.gz`, archive SHA-256
`8b2afcbefe9fe2d372c19631ab2dff3b8138505dd9ffaae737f405fd9609b1e1`.
All implementation/build writers stopped and source hashes reproduced. The two
repair regressions pass: queued cancellation frees the guard without allowing a
late callback, and explicit FOOD scope retains prior income dates correctly.
Acceptance applies to this repaired candidate, not the reviewed predecessor.

Verification: Temurin Java25.0.4.1/Maven3.10.0/Linux WSL2, isolated empty environment,
empty user/global Maven settings, offline existing cache. Final package **526 tests,
zero failures/errors/skips**. Earlier complete package/fresh-class runs passed524
before the two repair tests. Final standalone smoke passes non-web managed virtual
thread execution and context closure. Logs `apps/server/target/native-package-repaired.log`
and `native-smoke-final.log`; fresh-class evidence `native-package-fresh.log`.
Reproduce via server README build/smoke commands, using the existing cache and empty
settings `/tmp/longnest-nl-query-lead.1eG5tB/settings.xml` for both -s and -gs.
No provider/socket/DB/Telegram/container/private-env/paid/deployment action occurred.

Checks cover actual ChatClient/memory/native-manager routing and SDK request shape,
non-thinking/512-token cap/eight native tools/one request/no second narration;
relative-date/FOOD follow-ups and ordinary reply-to pages; full-range totals, paging
and current record controls; strict schema/unknown/duplicate/multi-call rejection;
literal amount/currency/known unsupported finance; authorization/target/replay and
storage-failure handling; bounded/reset/expiry memory, quota, concurrency and both
running/queued cancellation. Real language correctness is not tested by fake models.
JAR includes native conversation/generated mapper and excludes old interpreters,
smoke launcher/YAML. Static checks: local Markdown targets/fences, 107 unique
Vietnamese resource keys, changed-file whitespace. No formatter installation.
Existing Mockito/Lombok/Unsafe warnings and deprecated pinned tool-callback API
warnings remain nonfatal; no compatibility with a future Spring AI version claimed.

Failed/unknown checks preserved separately: initial test compilation/stubbing,
paging-fixture and known-flow case-matching failures were corrected; intermediate
logs remain under `/tmp/longnest-native-ai.HBq4sb`. Offline clean goal failed before
execution because clean plugin is uncached. Cross-filesystem generated-output move
hit full /tmp, so that attempted check is not accepted evidence. Partial generated
copy was recovered to ignored `apps/server/target/native-recovery-copy`, and prior
classes kept in `native-build-before`; no source/private data deleted. Later
fresh-output compilation/package and final repaired package passed without downloads.

Usable downstream input: this native candidate and synthetic fixtures, with existing
activation names/default-off behavior; see [server README](../apps/server/README.md#opt-in-deepseek-conversation).
Memory is eight user utterances, sixteen contexts, ten-minute idle expiry/reset and
no restart persistence; historical utterances would be egress when enabled. One
active request, 60 attempts per process-local hour, one attempt and 2/8/10-second
deadlines bound workload, not spend. No persistent quota/token-cost accounting.
No exactly-once reply/charge, general intent/injection guarantee, fuzzy selection or
unsupported category/note search. Model can still be semantically wrong despite
valid structure; live evaluation remains required before activation.

Remaining frontier: Human/operator owns endpoint/model choice, permitted utterance/
history privacy/terms/region/retention, credential custody and monetary caps before
live evaluation/activation. PostgreSQL provisioning/init/durability and live Telegram
scope/evidence remain outstanding, not waived by code acceptance. No deployment,
push or remote/destination guess. Existing configured identity/main/no remote or
upstream verified for the local commit. No other code assignment is left active.

Complete change set (A added, M modified, D removed):

Committed locally as `199afb045f57850b594f14d5bc34841994e06254` —
`Replace bespoke interpretation with native Spring AI conversation`, parent
`b100fbeadbe282b6cfc392c8686bab3a415f2c33`. All 51 inspected staged/live paths
matched; whitespace/excluded-path/credential-like-literal checks passed. No push,
remote or upstream. Tree was clean after commit; only this post-commit outcome
record is intentionally unstaged. No documentation-only commit/amend loop.

```text
M	AGENTS.md
M	apps/server/README.md
A	apps/server/src/main/java/me/nghlong3004/longnest/conversation/ConversationAssistant.java
A	apps/server/src/main/java/me/nghlong3004/longnest/conversation/ConversationConfiguration.java
A	apps/server/src/main/java/me/nghlong3004/longnest/conversation/ConversationMemory.java
A	apps/server/src/main/java/me/nghlong3004/longnest/conversation/LedgerTools.java
M	apps/server/src/main/java/me/nghlong3004/longnest/domain/RangeFilter.java
D	apps/server/src/main/java/me/nghlong3004/longnest/input/DeepSeekInterpreter.java
M	apps/server/src/main/java/me/nghlong3004/longnest/input/InputConfiguration.java
M	apps/server/src/main/java/me/nghlong3004/longnest/input/InputInterpreter.java
M	apps/server/src/main/java/me/nghlong3004/longnest/input/InterpretationResult.java
D	apps/server/src/main/java/me/nghlong3004/longnest/input/RangeQueryInterpreter.java
M	apps/server/src/main/java/me/nghlong3004/longnest/input/TransactionInterpreter.java
M	apps/server/src/main/java/me/nghlong3004/longnest/ledger/adapter/LedgerClient.java
A	apps/server/src/main/java/me/nghlong3004/longnest/ledger/dto/OperationRecovery.java
M	apps/server/src/main/java/me/nghlong3004/longnest/ledger/dto/RangeSummary.java
M	apps/server/src/main/java/me/nghlong3004/longnest/ledger/mapper/TransactionMapper.java
M	apps/server/src/main/java/me/nghlong3004/longnest/ledger/repository/EntryRepository.java
M	apps/server/src/main/java/me/nghlong3004/longnest/ledger/service/impl/LedgerQueryServiceImpl.java
M	apps/server/src/main/java/me/nghlong3004/longnest/ledger/service/impl/TransactionServiceImpl.java
M	apps/server/src/main/java/me/nghlong3004/longnest/ledger/service/LedgerQueryService.java
M	apps/server/src/main/java/me/nghlong3004/longnest/ledger/service/TransactionService.java
M	apps/server/src/main/java/me/nghlong3004/longnest/telegram/adapter/TelegramCapture.java
M	apps/server/src/main/java/me/nghlong3004/longnest/telegram/config/TelegramConfiguration.java
M	apps/server/src/main/java/me/nghlong3004/longnest/telegram/mapper/TelegramReplies.java
M	apps/server/src/main/java/me/nghlong3004/longnest/telegram/model/TelegramNavigation.java
M	apps/server/src/main/resources/application.yaml
M	apps/server/src/main/resources/messages_vi.properties
A	apps/server/src/test/java/me/nghlong3004/longnest/conversation/ConversationAssistantTests.java
A	apps/server/src/test/java/me/nghlong3004/longnest/conversation/ConversationConfigurationTests.java
A	apps/server/src/test/java/me/nghlong3004/longnest/conversation/ConversationMemoryTests.java
M	apps/server/src/test/java/me/nghlong3004/longnest/domain/DomainValuesTests.java
D	apps/server/src/test/java/me/nghlong3004/longnest/input/DeepSeekInterpreterTests.java
D	apps/server/src/test/java/me/nghlong3004/longnest/input/InputConfigurationTests.java
D	apps/server/src/test/java/me/nghlong3004/longnest/input/RangeQueryInterpreterTests.java
M	apps/server/src/test/java/me/nghlong3004/longnest/ledger/pagination/LedgerCursorsTests.java
M	apps/server/src/test/java/me/nghlong3004/longnest/ledger/repository/JpaLedgerStoreUnitTests.java
M	apps/server/src/test/java/me/nghlong3004/longnest/ledger/service/LedgerControlsTests.java
M	apps/server/src/test/java/me/nghlong3004/longnest/ledger/service/LedgerRangeTests.java
M	apps/server/src/test/java/me/nghlong3004/longnest/presentation/UiMessagesTests.java
M	apps/server/src/test/java/me/nghlong3004/longnest/telegram/config/TelegramLifecycleTests.java
M	apps/server/src/test/java/me/nghlong3004/longnest/telegram/TelegramAdapterTests.java
M	apps/server/src/test/java/me/nghlong3004/longnest/telegram/TelegramRangeTests.java
M	docs/ARCHITECTURE.md
M	docs/DECISIONS.md
M	docs/DOMAIN.md
M	docs/features/adapters/README.md
M	docs/PRODUCT.md
M	docs/SERVICES.md
M	docs/WORK_STATUS.md
M	README.md
```

### NL-QUERY-1 — ACCEPT explicit-date conversational reads, synthetic/local only

Base: accepted commit `1767ce779d3a84bd4fe1b56617de797fc199a47c`, 102-file server
manifest `8f5240edcec13091c8b7754ff3d6f6262e9dd25893b25e0ff8b9e0afeaaa12b4`
reproduced before writes. Only existing working change was the post-commit status
outcome. Lead `8442a05c-6421-45c4-adc7-65b64111c30f` owns implementation/shared
docs/integration; existing Peers remain idle/released, no dispatch or extra session.

Outcome: LIST_RANGE | CLARIFY for conversational requests with two explicit ISO
dates and income/expense/all scope; ordinary questions must not become purchases.
Reuse Spring AI/default-off config, host-owned context, LedgerClient.list and the
existing exact summary/selectable page/current-record/L2 controls. One interpreted
read, direct Vietnamese rendering, no ledger-result egress or second model call.
Scope: cohesive input classification/typed schema/evidence validation, narrow
Telegram dispatch and localized messages, fake-response/query-navigation tests and
minimal canonical docs. No new dependency/schema, relative dates, multi-turn
memory, inferred target/action requests or native tools/agent loop. Existing clear
capture/recovery and host-only Save/Delete proof stay unchanged.

Verification: inspect stable source, fake model/SDK/service callbacks, proportional
offline package and isolated smoke; no DB/Telegram/provider/container/private-env/
paid/deployment effects. Live endpoint/model/data/privacy/cost choices remain
unresolved, independently from code readiness. Exact candidate/paths/results and
explicit technical disposition will follow; commit only reviewed verified paths
with existing identity/main, no push or invented destination.

Stable Lead handoff/review: implementation/build writers stopped, no Peer scope
reopened. ACCEPT exact 104-file server manifest
`4f49515dde4df9fa18f067d0b8214626e6244fb0135502e76201c40644b1e977`.
Frozen evidence: `/tmp/longnest-nl-query-lead.1eG5tB/candidate-server.sha256` and
`candidate-server.tar.gz`, archive SHA-256
`702aca85e561486310e5186af3ba26f602b9ec1c647c5c4cb9b39858bde3c6e3`.
Versus base: 93 server files unchanged, nine modified, two added, none removed.
POM/dependencies, Spring AI factory/activation settings, schema, services/store,
MapStruct mappings and financial/control logic are unchanged.

Implementation: one cohesive stateless RangeQueryInterpreter guard/validator and
an immutable ListRange result in the existing sealed interpretation boundary.
DeepSeekInterpreter composes the read branch with its existing one-request SDK/
deadline/strict-JSON handling. No command hierarchy, native function registration,
agent framework or extra provider client. Recognized read cues/questions take
precedence over capture even with inference off; valid clear capture, frozen-event
recovery and authenticated field-prompt/command/callback routing remain intact.

Supported standalone examples (fake-model verified, not live language claims):
`Cho tôi các khoản chi từ 2026-10-01 đến 2026-10-23.`,
`Liệt kê khoản thu từ 2026-10-01 đến 2026-10-23.`,
`Xem giao dịch từ 2026-10-01 đến 2026-10-23.`, and
`Mình đã chi bao nhiêu từ ngày 2026-10-01 tới ngày 2026-10-23?`.
Source must contain exactly two valid ISO endpoints, ordered in explicit
`từ [ngày] A đến/tới [ngày] B`, with explicit income/expense/all scope. Equal
endpoints mean one day. No swapped/repaired/default endpoints, relative dates,
prior-message facts, inferred targets or arbitrary category/amount/currency filters.
Known unsupported or missing facts clarify locally without a provider call;
finite lexical checks do not establish unrestricted language understanding.

Query wire schema has exactly six string fields: intent/from/to/filter/
intentEvidence/filterEvidence. Only LIST_RANGE with matching literal endpoints,
enum filter and original request/scope evidence becomes a read intent; CLARIFY,
foreign/duplicate fields, capture-shaped output, native tool calls or truncation
never dispatch a query or mutation. Queries bypass create-inbox recovery/ingest;
the authenticated host calls existing LedgerClient.list/query service and directly
renders its exact same-snapshot full-range summary and ten-entry selectable page.
Next/Previous/current-state get/L2 controls use existing bounded navigation, without
another model call or ledger/result egress. Model output cannot supply principal,
operation identity, SQL/method, proof or authoritative amounts/counts/net.

Verification: Temurin Java25.0.4.1/Maven3.10.0/Linux WSL2; isolated environment,
empty user/global Maven settings, offline existing cache. Focused parser/presentation
run passed287; integration-focused run passed119 (intermediate scopes). Final
package passed **562 tests, zero failures/errors/skips**, versus base526 (+36).
Standalone isolated smoke exit0, non-web managed virtual-thread execution and
closure. Logs: `focused.log`, `integration-focused.log`, `package.log`, `smoke.log`
under the frozen directory. No failed Maven/test run occurred in this increment;
existing Mockito/Unsafe warnings remain nonfatal. Added checks cover all filters,
single-day/missing/invalid/reversed endpoints, conflicting scope, cross-route and
foreign-field rejection, query/capture separation, natural Vietnamese clarification,
unauthorized actors, exact service dispatch/full-range rather than page totals,
paging/current revision/L2 controls and no second provider/mutation call.

Executable JAR contains the new query/result types and generated mapper, without
smoke YAML/test launcher. Static checks: 21 public Markdown files/199 local targets,
balanced fences, 99 unique Vietnamese keys, changed-file whitespace clean. Source
hashes reproduced after verification; no Java/Maven/background writer remains.
No private env read/edit, DB/Telegram/provider/container call, paid effect or
deployment. These results accept bounded code, not PostgreSQL durability, live UX,
provider quality or full R1–R19 acceptance.

Complete changed paths (two new files; all others modified, none deleted):

```text
AGENTS.md
README.md
apps/server/README.md
apps/server/src/main/java/me/nghlong3004/longnest/input/DeepSeekInterpreter.java
apps/server/src/main/java/me/nghlong3004/longnest/input/InterpretationResult.java
apps/server/src/main/java/me/nghlong3004/longnest/input/TransactionInterpreter.java
apps/server/src/main/java/me/nghlong3004/longnest/input/RangeQueryInterpreter.java
apps/server/src/main/java/me/nghlong3004/longnest/telegram/adapter/TelegramCapture.java
apps/server/src/main/java/me/nghlong3004/longnest/telegram/mapper/TelegramReplies.java
apps/server/src/main/resources/messages_vi.properties
apps/server/src/test/java/me/nghlong3004/longnest/input/RangeQueryInterpreterTests.java
apps/server/src/test/java/me/nghlong3004/longnest/presentation/UiMessagesTests.java
apps/server/src/test/java/me/nghlong3004/longnest/telegram/TelegramRangeTests.java
docs/ARCHITECTURE.md
docs/DECISIONS.md
docs/DOMAIN.md
docs/PRODUCT.md
docs/SERVICES.md
docs/WORK_STATUS.md
docs/features/adapters/README.md
```

Disposition/next frontier: this enables bounded full-date conversational reads,
not full conversation. Relative-date grounding, follow-up/clarification memory,
fragment completion, fuzzy records and natural-language Edit/Delete/Restore remain
absent; existing explicit commands and authenticated controls still implement the
approved ledger flows. Any later conversational increment needs its own bounded
grounding/context/action contract, not a generic tool platform or another ledger
feature gate. Live use still needs explicit endpoint/model, permitted source-data,
privacy/retention/region/terms, private API credential custody and monetary/session
request limits. The same LONGNEST_DEEPSEEK_ENABLED/API_KEY/BASE_URL/MODEL names apply;
default off, existing connect2/read8/overall10-second limits, no retry/tools and
512-token output cap. No ledger results are submitted to the provider. Repeat
query delivery may re-interpret/re-read current state; no exactly-once reply or
provider-charge guarantee. PostgreSQL/init/live Telegram scope/config/evidence
remain separate and outstanding, not a prerequisite imposed on this code task.
Reviewed local Git commit follows with existing identity/main; no remote or push.
Committed locally as `0d76abf9d42163454b95411e8f8463389ad9ba7e` —
`Add typed explicit-date conversational range queries`, parent
`1767ce779d3a84bd4fe1b56617de797fc199a47c`; 20 inspected paths, 725 insertions,
35 deletions. Staged/live content matched; whitespace and credential/excluded-path
checks passed, no private/generated/protected paths included. Tree was clean after
commit. Only this post-commit hash/outcome record remains intentionally unstaged;
no documentation-only follow-up commit/amend loop and no push/destination guess.

### DS-FALLBACK-1 — ACCEPT default-off capture interpretation, synthetic/local only

Explicit continuation selects deterministic rules first, DeepSeek fallback only;
no replacement parser, live/provider calls, credentials, egress or spending is
authorized. Lead `8442a05c-6421-45c4-adc7-65b64111c30f` owns this bounded scope;
existing Peer assignments remain closed/idle, no dispatch or additional session.
Base: local commit `4a570246e6b9585287587a5642f2e72a43e8050b`, accepted 97-file
server manifest `cb35c199f201c434418d8dc515b2c88279253b3fe2615914de5643f96308cfeb`.
The only pre-existing working-tree change is this baseline outcome record.

Outcome: opt-in capture interpretation through the existing Spring AI DeepSeek
2.0.1 binding, strict typed JSON/local rules revalidation, trusted-event recovery
before repeat interpretation, Vietnamese clarification/errors, synthetic tests
and local build. Scope: input boundary/provider/config, narrow frozen-create
recovery service/client path, Telegram injection/capture, affected tests/resources
and minimal canonical documentation. Existing explicit controls bypass the model;
no model tools, ledger arithmetic, owner/proof fields or new finance flows.
Eligibility will be conservative: UNKNOWN_PHRASE only, not rejected/ambiguous/
missing-fact/unsupported currency, debt/transfer or relative-date paths. Capture
only; no natural-language query/edit/date/action dispatcher in this increment.
Provider client construction and requests default off, including init/smoke.
Live endpoint/model, data/privacy terms and spend/request authority remain separate
operator decisions. PostgreSQL/Telegram/R1–R17 remain unverified and are not a
prerequisite to this independent synthetic implementation. Exact candidate,
verification, complete paths and disposition follow after review.

Lead writer handoff/review: all implementation/build writers stopped, no Peer
assignment reopened. ACCEPT the exact 102-file server manifest
`8f5240edcec13091c8b7754ff3d6f6262e9dd25893b25e0ff8b9e0afeaaa12b4`
from the baseline above. Frozen source manifest/archive:
`/tmp/longnest-deepseek-lead.rouHbW/candidate-server.sha256` and
`candidate-server.tar.gz`; archive SHA-256
`47f22bdcf8e569404b88f1b0fe9402e456b003b383e408030879b5b8e50d4ce7`.
All source hashes were rechecked after build/smoke. This accepts code/configuration,
fake-model/SDK handling and local packaging, not live interpretation or R1–R19.

Implemented: InputInterpreter with unchanged default rules, a cohesive rules-first
DeepSeekInterpreter and opt-in InputConfiguration. Existing Spring AI starter/BOM
2.0.1 are reused; POM, dependencies, schema and money/date/financial controls are
unchanged. Official [Spring AI compatibility](https://docs.spring.io/spring-ai/reference/getting-started.html)
lists Boot4.1 support; its [DeepSeek binding](https://docs.spring.io/spring-ai/reference/api/chat/deepseek-chat.html)
and [DeepSeek JSON guide](https://api-docs.deepseek.com/guides/json_mode/)
support the selected manual SDK/JSON-object route, not arbitrary JSON-schema
guarantees. [Thinking documentation](https://api-docs.deepseek.com/guides/thinking_mode/)
requires explicitly disabling its current default; actual SDK serialization is
verified by the in-process mock HTTP fixture, without a socket/provider call.

Eligibility: only UNSUPPORTED/UNKNOWN_PHRASE, one locally exact amount, one
noncontradictory supported type cue and conservative lexical checks. Known invalid,
ambiguous/missing-fact, negated/hypothetical, foreign-currency, debt/transfer,
relative-date and command routes stay local. Model JSON must have exactly four
string fields and one completed choice; no extra/duplicate fields, tool calls,
trailing JSON or truncated response. Canonical draft reuses the deterministic
parser, exact local amount and literal source/type/note evidence. Unsupported
semantics clarify; this finite evidence policy cannot prove general language
understanding or prompt-injection safety. Category may remain uncategorized.

Authenticated capture recovers a frozen create/original receipt before another
provider interpretation. A storage failure stops it, not an empty-success guess.
This uses the same inbox and transactional service proxy: pending creates can
finish, completed originals replay, no model-owned ID/context/proof and no open
database transaction during inference. Existing direct typed ingest retains
changed-payload conflicts; distinct trusted events remain distinct purchases.
No model query/edit/date/Delete/Save/Cancel dispatcher or generic tools were added.

Configuration: LONGNEST_DEEPSEEK_ENABLED defaults false; explicit API_KEY, BASE_URL
and MODEL with that same prefix are required when enabled. Only the direct HTTPS
DeepSeek base URL is accepted, redirects disabled, no implicit model/endpoint
selection or third-party route. Init/smoke exclude the provider factory even with
inherited enablement. Existing root-env launcher loads these names literally with
inherited-env precedence; no private file was read/edited. JSON-object/non-thinking,
512 output tokens, one attempt/no retry/provider switch; connect2/read8/overall10
seconds with deadline cancellation and preserved interruption. Existing Boot-managed
execution is reused. No prompts, notes, secrets, response bodies or exception text
are logged by new app code; upstream prompt-bearing warnings are explicitly off.
Two new Vietnamese resource messages cover clarification/provider failure.

Verification: Temurin Java25.0.4.1/Maven3.10.0/Linux WSL2. Final offline package
with isolated environment/explicit empty user+global Maven settings passed
**526 tests, zero failures/errors/skips**, versus accepted baseline473 (+53).
Standalone isolated smoke exit0: non-web context, managed virtual-thread execution,
normal closure. Logs in the frozen directory: `package-accepted.log`, `smoke.log`.
The executable JAR includes the input boundary, SDK2.0.1 and generated mapper;
no smoke YAML/test launcher. Static checks: 21 public Markdown files/198 local
targets, balanced fences, 95 unique Vietnamese keys, changed-file diff whitespace
check clean. No Java/Maven/background writer remains. No runtime DB/Telegram/LLM,
paid/provider request, private-state access or deployment occurred.

Retained intermediate evidence: focused-second.log (39 passing), focused-third.log
(156 passing), package.log (521 passing before final guard expansion).
focused-initial.log records a compile failure from Spring7's boolean error-handler
signature; production handler corrected. package-final.log records one fixture
assertion failure (unused tool callbacks are null); sdk-focused.log records one
fixture predicate mistake (shouldRetry is not the retry counter). Final checks use
null-or-empty tool assertions, execute/count a failing local retry to prove one
attempt, and encode/decode actual SDK JSON through MockRestServiceServer. No failed
result was treated as acceptance. Existing Mockito/Unsafe warnings remain nonfatal.

Complete changed paths (five added Java/test files; all others modified, none removed):

```text
AGENTS.md
README.md
apps/server/README.md
apps/server/src/main/java/me/nghlong3004/longnest/input/InputInterpreter.java
apps/server/src/main/java/me/nghlong3004/longnest/input/DeepSeekInterpreter.java
apps/server/src/main/java/me/nghlong3004/longnest/input/InputConfiguration.java
apps/server/src/main/java/me/nghlong3004/longnest/input/InterpretationResult.java
apps/server/src/main/java/me/nghlong3004/longnest/input/TransactionInterpreter.java
apps/server/src/main/java/me/nghlong3004/longnest/ledger/adapter/LedgerClient.java
apps/server/src/main/java/me/nghlong3004/longnest/ledger/service/TransactionService.java
apps/server/src/main/java/me/nghlong3004/longnest/ledger/service/impl/TransactionServiceImpl.java
apps/server/src/main/java/me/nghlong3004/longnest/telegram/adapter/TelegramCapture.java
apps/server/src/main/java/me/nghlong3004/longnest/telegram/config/TelegramConfiguration.java
apps/server/src/main/resources/application.yaml
apps/server/src/main/resources/messages_vi.properties
apps/server/src/test/java/me/nghlong3004/longnest/input/DeepSeekInterpreterTests.java
apps/server/src/test/java/me/nghlong3004/longnest/input/InputConfigurationTests.java
apps/server/src/test/java/me/nghlong3004/longnest/ledger/service/LedgerUnitTests.java
apps/server/src/test/java/me/nghlong3004/longnest/telegram/TelegramAdapterTests.java
apps/server/src/test/java/me/nghlong3004/longnest/telegram/config/TelegramLifecycleTests.java
docs/ARCHITECTURE.md
docs/DECISIONS.md
docs/PRODUCT.md
docs/SERVICES.md
docs/WORK_STATUS.md
docs/features/adapters/README.md
docs/research/LIFEOS.md
scripts/README.md
```

Disposition limits/next outcome: default remains usable rules code; eligible prose
can reach a bounded validated draft/clarification through fake-provider boundaries.
Actual DeepSeek accuracy/latency/availability is unmeasured. Operator still owns
live endpoint/model selection, permitted data, applicable privacy/retention/region/
training terms, private credential custody and monetary/session request limits;
one request/512 tokens is not a monetary budget and cancellation does not prove no
charge. The next provider outcome is a separately authorized synthetic-only
quality/cost trial, not a new deterministic ledger feature gate. PostgreSQL and
live Telegram readiness remain independently unverified. Broader natural-language
queries/dates/action dispatch are intentionally outside this bounded capture scope.
Existing Git authority permits a reviewed local increment with the coherent
configured identity/main branch; no remote exists, so no push/destination guess.
Local increment committed as `1767ce779d3a84bd4fe1b56617de797fc199a47c` —
`Add default-off rules-first DeepSeek capture fallback`, parent baseline
`4a570246e6b9585287587a5642f2e72a43e8050b`; 28 reviewed paths, 997 insertions,
50 deletions. Staged/live content and credential/excluded-path checks passed;
no private/generated/protected guidance was staged, no config/remote changed.
Tree was clean immediately after commit. Only this post-commit hash/outcome record
is intentionally uncommitted; no documentation-only follow-up commit/amend loop.

This section supersedes the historical docs-only authority/ownership below.
Lead `8442a05c-6421-45c4-adc7-65b64111c30f` owns root/shared docs, ignore rules,
Git staging/commits and integration/acceptance. Recovery confirmed duplicate Lead
`040207a9-3fe2-4cb2-a478-6e571c892405` explicitly yielded with no writes or background
processes; it is quiescent. Original Lead `90b5d356-e005-4c81-8073-8ca3fbd1ace1`
and original Peer `e375c70d-365a-4b9a-a3da-d33c2ec79037` had no active turns and
were terminated through Paseo to prevent stale resumption. Continued Peer
`06b08ca3-0e21-427f-bc02-7a0c9cc6203e` originally confirmed a safe bootstrap
resumption. It is now terminated; recovered L1-STORE-1 ownership/disposition is below.
Existing authority: stage/commit/push inspected legitimate project baseline and
verified increment only with usable existing Git metadata, verified identity and
intended branch/remote/upstream/content; no guessed destination, force/history
rewrite, credential/config change or protected metadata workaround. First make the Java 25/Spring Boot backend compile,
start safely locally and smoke-test, then advance gradually toward bounded L1.
No deployment, real Telegram messaging, paid/LLM calls or real finance
egress; no DB installation or host-wide infrastructure/permission changes.

Current Git reconciliation (2026-10-23): authorized operator initialization is
confirmed; `.git` exists (0755) and Git recognizes `main`. The subsequent explicit
local-baseline authority permits the existing configured identity; author and
committer resolve consistently to the same valid name/email. No identity/config
change or additional approval gate is needed. No remote/upstream is configured:
local commit is authorized independently; push is not attempted or implied.
The absent/nonrepository and earlier empty-0555 observations below are historical.

BASELINE-1: Lead owns the released stable server/shared scope; both existing Peer
sessions are idle, no new assignment and no Java/Maven background writer observed.
All 97 accepted L3 server hashes still match
`cb35c199f201c434418d8dc515b2c88279253b3fe2615914de5643f96308cfeb`.
Reviewed inventory: 122 public project files, including unchanged project-local
backend guidance, source/tests, Maven wrapper/config, docs, dev Compose and launcher.
Existing ignore rules exclude private `.env`/`.env.dev`, `.codex`/`.aws` and generated
server `target`; no private files were read. Public-file credential-pattern review
found no matches; synthetic dev credentials are documented disposable placeholders.
Explicit reviewed paths only, no blind add-all, protected-guidance edit, metadata
workaround, build rerun or external operation. Prior 473-test/package and isolated
smoke evidence remains applicable to the unchanged source, not new verification.
Staged whitespace review reports existing mixed space/tab indentation in the
accepted POM; retained rather than changing the verified source for this commit.
Product wording now distinguishes implemented application contracts from unverified
runtime behavior and absent HTTP APIs; historical per-increment Git facts remain.
Disposition: ACCEPT BASELINE-1 local root commit on `main`,
`4a570246e6b9585287587a5642f2e72a43e8050b` —
`Establish deterministic ledger and Telegram adapter baseline`.
122 files, 16,534 inserted lines; complete staged inventory/diff reviewed, with no
credential-pattern matches or excluded/private/generated paths. Git's index matches
public files, allowing only automatic CRLF normalization in the existing skill and
root ignore file; protected guidance on disk was not edited. Working tree was clean
immediately after commit. This post-commit outcome record is intentionally the only
uncommitted file change; no documentation-only follow-up commit/amend or history
rewrite. No push: no configured remote/upstream and no destination guessed.
PostgreSQL/live Telegram and release R1–R17 evidence remain outstanding; no runtime
acceptance is added.

The project backend skill now exists and was read; it was externally/manually
created after the prior snapshot. Preserve it unchanged. `.agents` now observes
0755 but managed read-only protection remains; no skill relocation/refactoring.
Its branch router selects sections of existing BACKEND_RULES. Historical claims
that .agents was empty/uninstalled describe PG-RULES-1, not the current filesystem.

Read-only environment inspection: Temurin Java/javac 25.0.4.1, Maven 3.10.0;
existing `apps/server` starter uses Boot 4.1.1/Java 25. No PostgreSQL tools on PATH
or /usr/lib/postgresql; Docker CLI exists but daemon/socket access is denied
(`/var/run/docker.sock`, mode 0660). No container/DB provisioned or contacted.
Actual PostgreSQL startup/migration evidence is blocked until an authorized isolated
instance is available; no H2 fallback or mocked-context claim of DB integration.

First bounded Peer scope: existing `apps/server/**` bootstrap/build/runtime/test
only (not the whole backend); preserve unrelated starter files, no Git/root/shared
docs writes. Lead assigns exact base/owner at dispatch below. Outcomes: compile,
safe non-network/no-database Spring smoke, virtual-thread assertion as relevant,
reproducible commands and explicit PostgreSQL integration block. No fake domain
service, future HTTP/LLM/mail features or ledger-success claim. Production persistence
must not be silently disabled merely to pass a test; explicit smoke-only isolation.

Before dispatch Lead read root/shared rules, backend skill/all backend sections,
Domain/Services and L1; selected saved Peer profile after inspecting profile notes.
Prior docs candidates remain accepted for their design purpose, not implementation.

### Authorized sequence — VI-UI-1 → L1 initialization → L2 → L3

Lead `8442a05c-6421-45c4-adc7-65b64111c30f` remains sole current writer; existing
Peer is idle/closed, no dispatch/tab. Accepted L1-BROWSE-1 base reproduced:
`5e5618f3deb9050ba5cdb8f30411b5b39a93072b2203273934a98c85bf14c00f`,
71 server files. VI base archive/manifest: `/tmp/longnest-vi-ui.6ZG4fY/`.
At the VI sequence preflight, Git found .git **absent**, not the earlier 0555 directory;
rev-parse still exits 128/not a repository. No usable metadata/verified branch,
identity, remote or upstream; no initialization/configuration/protected workaround.

VI-UI-1 scope: UTF-8 Spring message bundles and a small transport-independent
message accessor; injected Telegram presentation/config and affected synthetic
tests, minimal canonical docs/status. Localize receipts/labels/help/clarification/
errors/buttons and actual reachable status text; preserve machine codes, commands,
IDs/tokens, exact strings and all auth/finance/navigation behavior. No language
selector/frontend/shared-client framework or dependency change. Verify Vietnamese,
parameter/encoding/fallback/coverage and existing local build/smoke before accepting.

Newly authorized continuation after VI acceptance: explicit first-use owner-head
initialization command/procedure, full L2 preview/Save/Cancel/edit/date/confirmed
Delete/Restore with persistent bounded action state and host proof, then L3 exact
inclusive summaries/type/range lists and shared navigation. Name stable bases and
owners per increment; no overlapping writers or additional session. This explicitly
permits **synthetic-only L2/L3 code over inspected stable contracts** while durable
L1/runtime acceptance is still unmet; it does not waive the canonical integration
dependencies or R1–R17 evidence. No LLM/provider/framework, future finance policy,
purge, DB/container install/connect/migrate, live Telegram/egress, private env,
paid call or deployment. Continue independent ready implementation, report concrete
contract blockers without inventing product gates. Final exact dispositions pending.

### VI-UI-1 — ACCEPT Vietnamese presentation, synthetic/local only

Lead accepts exact 75-file server manifest
`967a58a83f2d89fe6ca6f27938b551988a9f24d644a9926fb33dce3415a48404`,
base `5e5618f3deb9050ba5cdb8f30411b5b39a93072b2203273934a98c85bf14c00f`.
Frozen source manifest/archive: `/tmp/longnest-vi-ui.6ZG4fY/candidate-server.sha256`
and `candidate-server.tar.gz`; archive SHA-256
`522aed2541c3d2ce649b18645d0d7909b9e54533801eaacc412c22121c52d01e`.
Lead owns this accepted output; all VI code writes/builds stopped. No Peer/tab was
created or dispatched during localization. Full first-release continuation remains
authorized separately above, without promoting durable L1/runtime acceptance.

UiMessages is a single independent presentation component injecting Boot's standard
MessageSource, fixed locale vi. UTF-8 messages_vi.properties holds 55 UI keys;
messages.properties supplies one generic Vietnamese root fallback and enables
Boot auto-configuration. System-locale fallback is disabled. Missing keys return
generic unavailable text, not raw keys/arguments. No locale selector, second
message system, frontend/API, runtime language negotiation or new dependency.
Later server adapters can reuse UiMessages or compatible keys; Java properties
are not automatically browser-consumable. Vietnamese is end-user UI only; project
coordination/docs/identifiers remain English, now explicit in canonical guidance.

Audited reachable text: receipts/replay labels, entry type/category/state/date/note
labels, recent rows/empty/select/Next text, buttons, help and not-yet-implemented
notices, parser status/field/error explanations, service errors, uncertainty advice,
retry and startup-recovery operator-action advice. Callback acknowledgments remain
text-free; expiry/stale navigation refreshes the localized page as before. English
operator logs and sanitized exceptions are not sent to users. Commands, operation
IDs, callback data, error codes, category/type enum values and API records remain
unchanged. Exact canonical money/ID/date strings are substituted without grouping,
rounding or arithmetic. Plain-text Telegram has no Markdown/HTML parse mode;
notes/apostrophes/braces/markup remain literal arguments, not templates.

Complete changed paths (no deletions):

```text
Added:
apps/server/src/main/java/me/nghlong3004/longnest/presentation/UiMessages.java
apps/server/src/main/resources/messages.properties
apps/server/src/main/resources/messages_vi.properties
apps/server/src/test/java/me/nghlong3004/longnest/presentation/UiMessagesTests.java
Modified:
apps/server/src/main/java/me/nghlong3004/longnest/telegram/config/TelegramConfiguration.java
apps/server/src/main/java/me/nghlong3004/longnest/telegram/mapper/TelegramReplies.java
apps/server/src/main/resources/application.yaml
apps/server/src/test/java/me/nghlong3004/longnest/telegram/TelegramAdapterTests.java
apps/server/src/test/java/me/nghlong3004/longnest/telegram/TelegramBrowsingTests.java
apps/server/src/test/java/me/nghlong3004/longnest/telegram/config/TelegramLifecycleTests.java
apps/server/README.md
AGENTS.md
docs/DOMAIN.md
docs/PRODUCT.md
docs/BACKEND_RULES.md
docs/features/README.md
docs/WORK_STATUS.md
```

Verification: Java 25.0.4.1/Maven 3.10.0/Linux WSL2, isolated process environment,
explicit empty Maven settings, offline public-artifact cache. Focused run exit 0:
95 tests, no failures/errors/skips. Full fresh package from absent target exit 0:
**396 tests**, no failures/errors/skips (353 prior baseline plus 43 new UI cases).
Standalone isolated spring-boot:test-run smoke exit 0: non-web/managed virtual
thread/clean close; existing no-DB normal-start failure still fails closed.
Logs: `/tmp/longnest-vi-ui.6ZG4fY/focused.log`, `package.log`, `smoke.log`;
earlier generated target preserved there as `target-before-fresh-build/`.
Surefire reports in apps/server/target/surefire-reports. New tests verify actual
Vietnamese outputs/substitutions, all current error/type/state/category keys,
real interpreter clarifications, root/unknown-key fallback, host-locale independence,
Spring auto-configuration/injection without Telegram, exact amount extremes/long
IDs/dates, literal notes and unchanged protocol tokens. Existing SDK/auth/browse/
replay/service/mapper tests pass with Vietnamese assertions. Bundles and UiMessages
are packaged with generated mapper; no smoke YAML/test launcher in JAR. All bundle
keys unique. No compile/test failure; nonfatal Mockito/JVM agent/Unsafe warnings
remain. No source/POM/financial/auth/navigation change beyond the listed presentation
wiring; Capture/lifecycle/navigation/domain/input/ledger and financial schema stay
byte-identical. Earlier language-sensitive test expectations were mechanically
updated, not skipped or weakened. No generated/protected/private path edited.

Example: Đã lưu; #42 · Phiên bản 1 · Còn hiệu lực; Chi tiêu: 45000 VND;
Ngày: 2026-10-20 (Asia/Ho_Chi_Minh); Danh mục: Ăn uống; Ghi chú: ăn trưa;
Mã thao tác: tg:123:9. Buttons: Trang sau / Trang trước / Xem #42.
Remaining first-release code: explicit owner initialization, L2 all approved
edit/date/preview/Save/Cancel/Delete/Restore, then L3 inclusive overview/type/range
lists with exact summaries/paging. Live Vietnamese UX, PostgreSQL durability,
restart/recovery and R1–R17 remain unverified. No live Telegram/DB/container/private
env/model/paid/deployment action. .git absent/nonrepository, rev-parse exit 128;
no usable identity/remote/upstream metadata, no stage/commit/push or bypass.

### INIT-1 — ACCEPT explicit first-use owner initialization, synthetic/local only

Following VI-UI-1 acceptance, Lead reassigns existing Peer
`31d86be6-f7a4-45a0-b7c4-f8031ba34376` a new bounded scope, not revival of TG-SDK-2.
Selected inspected profile slp-peer-gpt-6-sol-high matches actual provider slp-peer,
model gpt-6-sol/high thinking; actual session mode full-access, no profile feature
overrides. No new tab/session. Base is exact accepted VI server manifest
`967a58a83f2d89fe6ca6f27938b551988a9f24d644a9926fb33dce3415a48404`.
Peer exclusively owns named apps/server initialization code/config/tests/README;
Lead retains shared/root docs/status and does not edit/build the moving server scope.

Outcome: explicit ledger-init operator profile/command initializes only a genuinely
empty migrated ledger with the configured authorized personal owner, commits its
generation-zero head, and exits cleanly; normal ledger startup never seeds or
recreates a missing head. Reuse proxied transaction service/store/JPA, validate
owner/durability and reject any existing head/financial/inbox state. No migration
or POM change needed; existing schema already has the head. Initialization is not
an ordinary Telegram or future model/tool action. Exclude Telegram lifecycle in
init mode even if inherited enabled=true; normal default/off behavior unchanged.
Provide command/required variable names and safe success/failure semantics without
reading real env or executing it against a database. Synthetic authorized/foreign/
nonempty/race-failure/proxy/closure/profile isolation and full package/smoke evidence.
Return immutable candidate/base/full paths/results/failures/limits and stop all
writers. Lead must inspect, verify and explicitly accept/reject before L2 dispatch.
No live DB/Telegram/container/private env/paid/deployment or Git workaround.

Peer submitted and released the named scope at 77-file server manifest
`a0ca4bf5cba56c6aebf85971a9b56a1a8f0518b42c454c20ef3d07cbebf6f8e9`.
Lead reproduced the manifest and confirmed the Peer is idle with no active turn;
no Java/Maven background process remains. Lead now owns the stable candidate for
inspection/verification; no new Peer assignment is active. Peer reports 407 tests
and isolated smoke passing; those are writer evidence, not yet Lead disposition.
Frozen manifest/archive and logs: `/tmp/longnest-init-1.cxSnEc/`.

Lead ACCEPTS this exact candidate after source inspection and independent fresh
offline package: **407 tests**, zero failures/errors/skips; isolated standalone
smoke exit 0. Lead logs `/tmp/longnest-init-lead.W0E27F/package.log` and `smoke.log`;
prior generated target preserved there, not deleted. JAR contains initializer,
UiMessages and generated mapper, no smoke/test launcher/configuration. Manifest
unchanged after verification. Archive SHA-256
`b2e5244312af677564e55a41fa3ff922f395d436bfd295ecf6656c85e9ce8bce`.
Initial writer test queried a closed context and failed; corrected before the
passing final writer/Lead runs. Mockito/Unsafe warnings remain nonfatal.

Complete server paths: added ledger/config/LedgerInitConfiguration.java and
test ledger/config/LedgerInitConfigurationTests.java; modified README.md,
ledger/model/entity/OwnerHead.java, ledger/repository/JpaLedgerStore.java and
LedgerStore.java, ledger/service/TransactionService.java and
service/impl/TransactionServiceImpl.java, telegram/config/TelegramConfiguration.java,
resources/application.yaml, and test ledger/repository/JpaLedgerStoreUnitTests.java,
service/LedgerProxyTests.java and LedgerUnitTests.java (Java paths relative to
src/main/java/me/nghlong3004/longnest/ or src/test/java/me/nghlong3004/longnest/).
No deletions, POM/schema/launcher/UI changes. Lead shared changes: Services and this
status. Operator procedure: packaged JAR --spring.profiles.active=ledger-init,
with private caller DataSource and owner environment, as documented in server
README. Direct Java does not load .env.dev. This command is implemented, **not run**.
Exclusive table locking/all-table emptiness, PostgreSQL durability/settings,
Flyway, real commit/concurrency/reopen remain unverified. Normal operations still
fail missing-head; initialization never becomes a model/Telegram tool. Git absent,
rev-parse 128: no metadata/identity/remote/upstream and no stage/commit/push/bypass.

### L2-CRUD-1 — bounded edit/confirmation/delete/restore implementation

Next assignment uses accepted INIT-1 base above. Existing sole Peer
31d86be6-f7a4-45a0-b7c4-f8031ba34376 owns apps/server L2 code, additive Flyway V2,
localized messages/tests/server README after explicit dispatch; Lead retains
root/shared docs/status and does not edit/build the moving server scope. Inspected
slp-peer-gpt-6-sol-high profile matches existing provider/model/high thinking;
full-access session retained, no new agent/tab or profile feature override.
Outcome: every approved edit field including date, persistent field prompts and
immutable before/after preview, actual-user bound Save/Cancel, confirmed reversible
Delete/Restore, original receipt replay/no-op/revision/expiry/replacement semantics,
through shared services and synthetic SDK callbacks. No L3 or provider work in this
increment. Additive migration implementation is permitted, execution is not.
Durable L1/L2 and R1–R17 acceptance remain conditional on PostgreSQL/live evidence;
new synthetic code is explicitly authorized without relabeling that evidence.
Stable candidate/actual handoff, Lead inspection and disposition required next.

LOMBOK-2 amendment while Peer still owns the moving L2 scope: audit **all backend model
types**, not only JPA entities: domain/input values and nested records, ledger
entities/control/proof/DTO models, Telegram/navigation/config models and other
backend model categories regardless of folder. Same accepted INIT-1 original
base; no new stable candidate/handoff has occurred. Existing Peer exclusively
owns this behavior-preserving model-boilerplate amendment within apps/server,
including minimal domain/input caller/test changes where justified; Lead does not
write affected paths. Use safe Lombok builders/constructors/getters where useful;
keep records concise, JPA no-args protected, all-field constructors/builders
non-public where they could bypass IDs/version/validation/state. Audit actual
callers/default/access/MapStruct behavior and explain concrete retained validation,
conversion/defensive-copy/state-transition exceptions. No blanket Data/setters,
financial/API redesign or generated-source edits. Include complete category and
per-entity decisions and focused builder/access checks in the L2 handoff; continue
ready L2 work rather than pausing it for a separate tab or duplicate audit.

Peer has now submitted L2-CRUD-1/LOMBOK-2 at 92-file server manifest
`9d423a99f909f7f0c7db29775711f9eecbe0a11a9635df6ab331fe5b78395d00`;
archive `/tmp/longnest-l2-peer.fTJAjW/candidate-server.tar.gz`, SHA-256
`87e6efbb8b106c7c51f9e412606ef9423269da9116fc28d07bf0ea43a417aa00`.
Lead reproduced both hashes and confirmed idle/no active Peer turn and no Java/
Maven background writer. Named server scope is released to Lead for inspection
and independent verification; candidate remains **unaccepted**, L3 not dispatched.

Lead REJECTS candidate `9d423a99…` for L2 acceptance. Independent fresh offline
package passed 440 tests/no failures/errors/skips (407 accepted INIT baseline plus
33); log `/tmp/longnest-l2-lead.10NKLv/package.log`, earlier target preserved there.
Passing tests missed Restore replay: entryCallback passes each fresh callback
update ID to restore(id, old revision), so another click on the same displayed
button conflicts rather than returning its original receipt. /restore ID retries
re-read current revision before existing-outcome lookup, changing the fingerprint
after a committed result. No duplicate financial mutation was observed/claimed;
truthful original-outcome recovery is incomplete. Additional bounded fixes:
validate mutation operation identity before store access, tighten PROMPT field
non-null SQL check, complete mapper construction and safe plain-model getter audit.
No standalone smoke rerun is claimed for this rejected snapshot.

L2-CRUD-2 / LOMBOK-2 repair explicitly returns server ownership to the **same Peer**,
base the frozen rejected 92-file `9d423a99…` manifest/archive above; original L2
accepted base remains INIT-1. Lead retains shared docs/status and will not edit/
build the moving repair. Require stable displayed Restore identity across clicks,
same-event recover-before-current-revision command handling (or remove the extra,
noncanonical /restore convenience route without removing Restore capability),
actual service+SDK regression evidence and fresh package/smoke. Do not add another
human confirmation to Restore or change its canonical id/revision service path.
Candidate needs a new exact handoff/disposition; L3 waits for that stable code.

Repair submitted at exact 92-file manifest
`c3abecec472da4bbc99294a3562ba79793b429682f5c8fabfd535ca5bb77f979`;
Lead reproduced manifest/archive SHA-256
`fc679f68b22b48027607000af19c7745d292bb17526a90054892d1531642193a`.
Frozen `/tmp/longnest-l2-peer.fTJAjW/repair-server.sha256` and `repair-server.tar.gz`.
Peer is idle/no active turn, reports all builds/writers stopped; no Java/Maven
process remained before Lead verification. Lead now owns repair for review;
writer reports 443 tests and smoke passing, not yet Lead acceptance. No added/
deleted files versus rejected snapshot. Independent fresh package in progress,
evidence `/tmp/longnest-l2-repair-lead.dkkGOr/`.

### L2-CRUD-2 / LOMBOK-2 — ACCEPT synthetic/local implementation

Lead ACCEPTS exact 92-file `c3abecec472da4bbc99294a3562ba79793b429682f5c8fabfd535ca5bb77f979`
after inspecting the repaired service/SDK flow, schema/mappings, model access and
actual integrated regression. Independent fresh offline package exit 0: **443
tests**, no failures/errors/skips; isolated standalone smoke exit 0. Logs
`/tmp/longnest-l2-repair-lead.dkkGOr/package.log` and `smoke.log`; prior generated
target preserved there. Original accepted INIT base a0ca4bf5…; rejected repair
base 9d423a99… remains rejected, not retroactively accepted. Full hashes and frozen
manifest/archive above. Server manifest unchanged after verification. JAR has V2,
VI bundles, SDK 10.3.0 modules and generated mapper, no smoke/test launcher. Javap
confirms only intended fluent proof/event getters, no public signature/raw event
constructor, protected JPA no-args. No inline qualified-name exception needed.

Implemented: every approved field (amount/type/category/note/date), persisted
field-prompt/reply bindings, current/proposed VI values and choices, multi-field
immutable replacement preview, actual authenticated Save/Cancel, request-only
/delete ID, target-bound Confirm Delete, deleted view and single-click Restore.
Typed restore(id,expectedRevision) remains reusable; unsafe extra /restore ID
convenience command was removed (not a required canonical command). Restore display
actions persist independently of callback delivery and return original receipts
after later changes/reconstructed instances. No second confirmation gate. Control
state/live cap 100/10-minute expiry, retained outcomes, changed-event conflicts,
no-op revisions/generation, owner checks and original create receipts remain.
Application validation/finance remains outside Telegram; MapStruct constructs
representations, manual conversion helpers preserve exact policy. No command bus,
per-button hierarchy or speculative pattern/framework.

Actual regression on rejected base failed REVISION_CONFLICT, retained at
`/tmp/longnest-l2-peer.fTJAjW/restore-base-regression.log`; repair fixes it. Writer
also retained earlier obsolete /delete fixture, root-directory Maven invocation
without POM, and outdated mocked browse fixture failures before corrected fresh
runs. Mockito/dynamic-agent/Unsafe warnings remain nonfatal, not skipped checks.

All-model audit coverage: domain Money/Note/TransactionDraft and Category/type
enums; input sealed statuses/codes/Ready/Problem and parser nested state; ledger
DTO records, HostContext/CreateIntent/PersonalOwner/exception, opaque proof and
signer, store snapshots/enums, cursor Key/Cursor; TelegramSettings/Failure and
navigation Route/Visit/EntryControl; all seven JPA entity/embeddable classes.
Records stay records; compact constructors retain money/date/NFC/compatibility,
optional/null and defensive-copy behavior. Category already uses generated required
construction; existing dependency constructors/getters remain Lombok where useful.
Per entity: LedgerControl replaces large assignment constructor with private
Builder/private AllArgsConstructor behind frozen-state checks; ControlEvent uses
private generated all-args behind validated bound factory and narrow generated
fingerprint getter. Both keep protected NoArgsConstructor. InboxId retains its
existing generated pair/equality. OwnerHead manual constructor validates PersonalOwner
and generation 0; LedgerEntry converts embedded values, starts business revision 1,
leaves provider ID/version unset and retains controlled mutate; CreateInbox checks
uncompleted intent and converts embedded values/time, with one-time complete;
EntryValues explicitly converts exact Money/Note/date to/from persisted fields.
These conversions/default/state checks are not raw all-field copying. ControlProof
plain accessors are generated, but constructor/signature accessor clone bytes and
stay non-public; TelegramSettings validates token/user and derives namespace without
token toString. Exception super/redaction and navigation defensive copies remain
explicit. No public raw-state builder/allargs, Data/setters, equality/serialization
expansion or generated-source edits. Focused reflection/default/access/mapping tests
are included in the 443-test result, not database-provider evidence.

Complete server changed paths versus accepted INIT-1 (no deletions):

```text
apps/server/README.md
apps/server/src/main/java/me/nghlong3004/longnest/ledger/adapter/LedgerClient.java
apps/server/src/main/java/me/nghlong3004/longnest/ledger/dto/ConfirmationRequired.java
apps/server/src/main/java/me/nghlong3004/longnest/ledger/dto/ControlBinding.java
apps/server/src/main/java/me/nghlong3004/longnest/ledger/dto/FieldPrompt.java
apps/server/src/main/java/me/nghlong3004/longnest/ledger/dto/TransactionPatch.java
apps/server/src/main/java/me/nghlong3004/longnest/ledger/mapper/TransactionMapper.java
apps/server/src/main/java/me/nghlong3004/longnest/ledger/mapper/TransactionRepresentationMapper.java
apps/server/src/main/java/me/nghlong3004/longnest/ledger/model/ControlProof.java
apps/server/src/main/java/me/nghlong3004/longnest/ledger/model/ControlProofs.java
apps/server/src/main/java/me/nghlong3004/longnest/ledger/model/EditField.java
apps/server/src/main/java/me/nghlong3004/longnest/ledger/model/HostContext.java
apps/server/src/main/java/me/nghlong3004/longnest/ledger/model/LedgerException.java
apps/server/src/main/java/me/nghlong3004/longnest/ledger/model/entity/ControlEvent.java
apps/server/src/main/java/me/nghlong3004/longnest/ledger/model/entity/LedgerControl.java
apps/server/src/main/java/me/nghlong3004/longnest/ledger/model/entity/LedgerEntry.java
apps/server/src/main/java/me/nghlong3004/longnest/ledger/repository/ControlEventRepository.java
apps/server/src/main/java/me/nghlong3004/longnest/ledger/repository/ControlRepository.java
apps/server/src/main/java/me/nghlong3004/longnest/ledger/repository/JpaLedgerStore.java
apps/server/src/main/java/me/nghlong3004/longnest/ledger/repository/LedgerStore.java
apps/server/src/main/java/me/nghlong3004/longnest/ledger/service/TransactionService.java
apps/server/src/main/java/me/nghlong3004/longnest/ledger/service/impl/TransactionServiceImpl.java
apps/server/src/main/java/me/nghlong3004/longnest/telegram/adapter/TelegramCapture.java
apps/server/src/main/java/me/nghlong3004/longnest/telegram/config/TelegramConfiguration.java
apps/server/src/main/java/me/nghlong3004/longnest/telegram/mapper/TelegramReplies.java
apps/server/src/main/java/me/nghlong3004/longnest/telegram/model/TelegramNavigation.java
apps/server/src/main/resources/db/ledger/V2__ledger_controls.sql
apps/server/src/main/resources/messages_vi.properties
apps/server/src/test/java/me/nghlong3004/longnest/ledger/repository/JpaLedgerStoreUnitTests.java
apps/server/src/test/java/me/nghlong3004/longnest/ledger/repository/LedgerModelAuditTests.java
apps/server/src/test/java/me/nghlong3004/longnest/ledger/service/LedgerControlsTests.java
apps/server/src/test/java/me/nghlong3004/longnest/ledger/service/LedgerProxyTests.java
apps/server/src/test/java/me/nghlong3004/longnest/ledger/service/LedgerUnitTests.java
apps/server/src/test/java/me/nghlong3004/longnest/telegram/TelegramAdapterTests.java
apps/server/src/test/java/me/nghlong3004/longnest/telegram/TelegramBrowsingTests.java
apps/server/src/test/java/me/nghlong3004/longnest/telegram/TelegramL2Tests.java
apps/server/src/test/java/me/nghlong3004/longnest/telegram/config/TelegramLifecycleTests.java
```

Lead shared paths in this sequence: AGENTS.md, README.md, docs/BACKEND_RULES.md,
docs/DECISIONS.md, docs/SERVICES.md, docs/features/ledger/README.md and this status.
Static changed-doc fences/local targets checked; no runtime claim from those checks.
PostgreSQL migration/schema validation, real locks/commit/replay/reopen/backup and
live Telegram UX still unverified; R1–R17 are not product passes. No private env,
DB/container/provider/live/paid/deployment operations. Git absent/nonrepository,
no usable metadata/identity/remote/upstream: no commit or push, no workaround.

### L3-RANGE-1 — next bounded implementation

After L2 acceptance the same existing Peer owns apps/server range/query/navigation
code/tests/server README under explicit dispatch, accepted base c3abecec… above;
Lead retains root/shared docs/status, with no concurrent server writes/builds.
Outcome: inclusive overview, income/expense/all range totals+selectable ten-entry
pages, exact aggregates, shared cursor lifecycle and same-query Next/Previous/
refresh after expiry/mutation/restart. Vietnamese text externalized, selection
re-gets current state and retains L2 controls. No schema/dependency changes expected.
Preserve LOMBOK-2 model guards/concise records and all accepted capture/init/L2
behavior. Prove synthetic/local code, not real PostgreSQL/live acceptance. Require
stable handoff and explicit Lead disposition; runtime/privacy/Git boundaries unchanged.
Dispatch acknowledged to the same Peer, with finish notification enabled for this
Lead. Profile re-inspected slp-peer-gpt-6-sol-high, same current provider/model/
high thinking/full-access session, no feature override/new tab. Original base and
named scope above; no other server writer/build. Shared query policy includes
validated compact range/filter callback context for same-query refresh after
ephemeral navigation loss, within Telegram's 64-byte limit; this is read-query
metadata, never financial authority. No signing/snapshot subsystem is introduced.

L3 recovery: Peer hit its account limit after partial edits, without a candidate.
Paseo confirmed error/no active turn; Lead terminated that session successfully
and checked no Java/Maven/background writer. No new Peer/tab was created. Lead
8442a05c-6421-45c4-adc7-65b64111c30f now exclusively owns L3 server implementation
as well as shared docs; closed Peer may not resume on an old callback. Accepted
input remains c3abecec…; interrupted partial manifest
`1581bfda2c163c14d8b0e00dd5de9213e755ed8f224411f611d50ad028c0fde0`
is preserved under `/tmp/longnest-range-lead.OyQuL6/interrupted-server.sha256`
and `interrupted-server.tar.gz`, not accepted. Lead continues from actual partial
DateRange/RangeFilter/RangeSummary/TransactionPage edits without reverting them.

### L3-RANGE-1 — ACCEPT Lead-owned synthetic/local implementation

Lead accepts exact **97-file** server manifest
`cb35c199f201c434418d8dc515b2c88279253b3fe2615914de5643f96308cfeb`,
from accepted L2-CRUD-2 base
`c3abecec472da4bbc99294a3562ba79793b429682f5c8fabfd535ca5bb77f979`.
Interrupted partial 1581bfda… was inspected/preserved, not accepted as a handoff.
Frozen manifest/source archive `/tmp/longnest-range-lead.OyQuL6/candidate-server.sha256`
and `candidate-server.tar.gz`; archive SHA-256
`516c324a29724540ebd7c4afcb14d346be1223d7d88e1d3fd898574cb930e0a4`.
Lead owns all stable server/shared output; implementation writes/builds stopped,
no active Peer assignment/background process, no additional agent/tab. The Peer
that hit its limit was quiesced before Lead takeover, not revived or overlapped.

Implemented user outcomes: /overview x y totals/counts/net only; /income and
/expenses x y full-type total/count plus selectable first ten; /transactions x y
all-type summary plus list. Both strict real inclusive endpoints required, equal
day valid, full 1900–9999 supported, invalid/missing/reversed asks correction with
no substituted range. Only current owned active financial-date entries contribute.
Empty queries return zeros/no rows. Changed amount/type/date/deletion/restoration
state is read from the same ledger, with no parallel totals ledger. Example
25,000,000 income and corrected 40,000 expense yields net 24,960,000, not bank balance.
All new text/help/errors remain Vietnamese MessageSource values; numeric/date/ID
strings remain exact, plain text with no parse mode. Clear capture and L2 actions
are unchanged, not gated behind reports or an LLM.

One LedgerCursors replaces RecentCursors for recent/range, sharing owner/scope/
filter/range/generation/TTL/capacity rules, with date/ID versus creation-time/ID
keyset order. Range page and full-range summary assemble after one owner-head lock
in the existing proxied query unit. Native PostgreSQL numeric aggregates/counts
convert BigDecimal.toBigIntegerExact to canonical strings; no long/double narrowing.
TelegramNavigation retains bounded immutable history and compact validated query
metadata for same-range/type refresh after expiry/eviction/restart/stale generation;
maximum ID/date callback fits 64 bytes. Known copied/changed-query controls reject;
unknown valid metadata is reauthorized as read-only query, never action authority.
Selection re-gets current revision before offering existing L2 controls. No pattern
hierarchy, per-command classes, signing key, snapshot service, dependency or migration.
New DateRange/RangeSummary/Totals/RangeSelection/Key state remains concise records
with explicit bounds/copy/net validation; RangeFilter enum has no constructor
boilerplate. Projection interface accessors are required Spring Data contracts,
not handwritten getter implementations. LOMBOK-2 guards remain intact.

Verification: Java25.0.4.1/Maven3.10.0/Linux WSL2, isolated environment/empty Maven
settings/offline cache. Initial wiring package 444 tests passed; first focused
47 passed, expanded focused 50 passed. Final **fresh package from absent target:
473 tests, zero failures/errors/skips**, 30 more than accepted L2 baseline443.
Final standalone isolated smoke exit0, non-web managed virtual-thread execution
and closure. Logs under `/tmp/longnest-range-lead.OyQuL6/`: initial-package.log,
focused.log, focused-final.log, package.log, smoke.log; earlier generated target
preserved as target-before-fresh-build, not deleted. No failed L3 build/tests;
one patch context mismatch was corrected before checks. Mockito/Unsafe warnings
remain nonfatal. Generated mapper has range summary/page implementations; JAR has
new shared cursors/models, no legacy RecentCursors or smoke/test classes. POM,
runtime YAML and V1/V2 hashes unchanged from accepted L2. One justified inline
qualified Java type remains in JpaLedgerStoreUnitTests: Spring Data Query annotation
collides with the imported jakarta.persistence.Query used by EntityManager mocks.
All other changed handwritten references use normal imports; no generated edits.

Coverage: actual query service and synthetic SDK routing over port state; inclusive/
equal/bounded/bad ranges, Vietnam midnight/default financial date versus later
creation, all/type/empty/negative/greater-than-long totals, fractional projection
rejection, >10 rows/tie ordering/full totals each page, lock-call order, foreign
owner/query/cursor, generation changes, Next/Previous/history cap, copied controls,
expiry/reconstructed-instance same-query refresh, max-ID/date callback length,
selection of changed current record and correction/date/delete/restore contributions.
These do not establish database SQL/Hibernate query execution, real locks/coherent
concurrent snapshots, commit/reopen/backup or actual Telegram UX. R1–R17 remain
unaccepted end-to-end; no H2 substitute or durability waiver.

Complete server changes versus accepted L2 (two removals are renames, content
generalized/preserved in new files; recoverable from accepted/frozen archives):

```text
apps/server/README.md
apps/server/src/main/java/me/nghlong3004/longnest/domain/DateRange.java
apps/server/src/main/java/me/nghlong3004/longnest/domain/RangeFilter.java
apps/server/src/main/java/me/nghlong3004/longnest/ledger/adapter/LedgerClient.java
apps/server/src/main/java/me/nghlong3004/longnest/ledger/config/LedgerConfiguration.java
apps/server/src/main/java/me/nghlong3004/longnest/ledger/dto/RangeSummary.java
apps/server/src/main/java/me/nghlong3004/longnest/ledger/dto/TransactionPage.java
apps/server/src/main/java/me/nghlong3004/longnest/ledger/mapper/TransactionMapper.java
apps/server/src/main/java/me/nghlong3004/longnest/ledger/mapper/TransactionRepresentationMapper.java
apps/server/src/main/java/me/nghlong3004/longnest/ledger/model/LedgerException.java
apps/server/src/main/java/me/nghlong3004/longnest/ledger/pagination/LedgerCursors.java
apps/server/src/main/java/me/nghlong3004/longnest/ledger/repository/EntryRepository.java
apps/server/src/main/java/me/nghlong3004/longnest/ledger/repository/JpaLedgerStore.java
apps/server/src/main/java/me/nghlong3004/longnest/ledger/repository/LedgerStore.java
apps/server/src/main/java/me/nghlong3004/longnest/ledger/service/LedgerQueryService.java
apps/server/src/main/java/me/nghlong3004/longnest/ledger/service/impl/LedgerQueryServiceImpl.java
apps/server/src/main/java/me/nghlong3004/longnest/telegram/adapter/TelegramCapture.java
apps/server/src/main/java/me/nghlong3004/longnest/telegram/mapper/TelegramReplies.java
apps/server/src/main/java/me/nghlong3004/longnest/telegram/model/TelegramNavigation.java
apps/server/src/main/resources/messages_vi.properties
apps/server/src/test/java/me/nghlong3004/longnest/ledger/config/LedgerConfigurationTests.java
apps/server/src/test/java/me/nghlong3004/longnest/ledger/pagination/LedgerCursorsTests.java
apps/server/src/test/java/me/nghlong3004/longnest/ledger/repository/JpaLedgerStoreUnitTests.java
apps/server/src/test/java/me/nghlong3004/longnest/ledger/service/LedgerRangeTests.java
apps/server/src/test/java/me/nghlong3004/longnest/ledger/service/LedgerUnitTests.java
apps/server/src/test/java/me/nghlong3004/longnest/presentation/UiMessagesTests.java
apps/server/src/test/java/me/nghlong3004/longnest/telegram/TelegramRangeTests.java
Removed old paths (renamed above):
apps/server/src/main/java/me/nghlong3004/longnest/ledger/pagination/RecentCursors.java
apps/server/src/test/java/me/nghlong3004/longnest/ledger/pagination/RecentCursorsTests.java
```

Shared L3 reconciliation: README.md, docs/BACKEND_RULES.md, docs/SERVICES.md,
docs/features/ledger/README.md, docs/features/adapters/README.md, this status.
No private/protected/generated/evidence/data content removed. Git preflight still
exit128/nonrepository with .git absent: no usable metadata/verified identity/branch/
remote/upstream, so no stage/commit/push or hash is claimed and no workaround used.

### Current usable code and next frontier

Implemented deterministic first-release code now covers capture/get/recent/current
selection, explicit operator-only first initialization, every L2 field/preview/
Save/Cancel/confirmed reversible Delete/Restore, and inclusive L3 overview/type/all
range views with shared ten-entry navigation. No approved first-release field or
flow is intentionally deferred to budget/salary/classification. Examples in server
README describe code, not a live running bot. The next smallest working-ledger
prerequisite is operator-provided isolated PostgreSQL plus explicit connection/
migration/init/runtime scope, private DataSource/owner config, then separately
authorized Telegram token/user activation and actual UX checks. Do not send secrets
in chat. Backup/custody/retention decisions remain operational, no purge is approved.

Rules-first DeepSeek capture (DS-FALLBACK-1) and explicit-date read intents
(NL-QUERY-1) are selected; replacement is not.
Live endpoint/model, privacy/egress/retention/cost limits and private credential
custody remain unresolved. Fake-provider checks do not establish provider quality
or authorize even synthetic outbound trials. Typed application capabilities remain
present; no general model/tool dispatcher or agent framework is implemented or
activated. Real-user Save/Delete/Cancel proof remains host-only.
No DB/container/live Telegram/provider/paid/
deployment/private-env action was performed. Further runtime work waits for that
specific authority/input, not another ledger-product decision.

Final read-only handoff checks: all 97 frozen server hashes unchanged, 19 public
Markdown files with 189 local targets/55 anchors and balanced fences pass; all 93
Vietnamese resource keys are unique. .git remains absent; no Java/Maven process
remains. Source archives and prior target evidence are retained. No post-freeze
server edits, extra dispatch or external activation followed acceptance.

### L1-BROWSE-1 — bounded Lead-owned implementation

Lead `8442a05c-6421-45c4-adc7-65b64111c30f` is sole implementation owner, keeping
shared docs/status as well. Room/status/handoff reconciled: TG-SDK-2 Peer remains
idle and its assignment closed/released; no revival/dispatch/new tab. Actual
accepted 69-file server base reproduced after EMPTY-DIRS-1:
`1e5468a1c84154a1470e17cb1fd97981c2302fe101db9e358f058d2daaf605dc`.
Base manifest/source archive: `/tmp/longnest-browse.66h2pn/base-server.sha256` and
`base-server.tar.gz`. Git remains protected .git 0555/nonrepository, rev-parse
exit 128; no stage/commit/push or bypass. Continue independently authorized code.

Outcome: /recent → Next → Previous → select entry → freshly retrieved current
record, with authenticated private-chat callbacks and immutable bounded cursor
history. Scope: Telegram capture/replies/config/lifecycle, one cohesive navigation
state class and focused tests, existing relevant SDK tests; minimal server/root/
canonical docs and this status. Existing get/recent services, financial contracts,
schema, POM/dependencies, launcher, Compose and private/protected paths stay intact.
No L2/L3, new financial actions or generic button/command framework. Use normal
Java imports in changed handwritten files; backend rule now records that convention.

Acceptance: synthetic SDK/service callbacks for authorized/foreign actors and
chat/message binding, Next/Previous boundaries, ten-entry paging, stale/invalid/
expired/restarted navigation refresh, bounded history/state, current-state get and
repeat interactions without financial mutations; new full package and isolated
standalone smoke, generated mapper and off-by-default/resource isolation preserved.
No live network/DB/container/private env/model/paid/deployment operations. Exact
final manifest/paths/results/limits and explicit disposition were pending at
assignment; resolved below.

#### L1-BROWSE-1 — ACCEPT implementation and synthetic/local browsing only

Lead implemented, reviewed and accepts exact 71-file server candidate
`5e5618f3deb9050ba5cdb8f30411b5b39a93072b2203273934a98c85bf14c00f`,
from the accepted 69-file base
`1e5468a1c84154a1470e17cb1fd97981c2302fe101db9e358f058d2daaf605dc`.
Deterministic manifest `/tmp/longnest-browse.66h2pn/candidate-server.sha256`;
source archive `candidate-server.tar.gz` in that directory, SHA-256
`6c434e83e6f62da5acbc29fb5aaccc418c4891864e0ff0de348bc2aa546aaf0c`.
Reproduce manifest: project-root `rg --files --hidden --no-ignore apps/server
-g '!**/target/**' | LC_ALL=C sort | xargs sha256sum | sha256sum` with pipefail.
Four non-status docs hash in order root README, server README, Backend rules,
Services: `c1aa53151a6ad45fa69fd52bfef1d05821b3799db61a5156766898b13b7eaf99`.
All builds/writers stopped; Lead retains ownership. Closed Peer remains closed;
no session/tab was created or delegated for this work.

Usable implemented interaction: `/recent` → Next → Previous → Show #42 → current
complete record. Existing text /show and /recent tokens remain usable. First page
has no Previous, final page no Next, empty first page no dead buttons. Each step
sends a fresh page reply; older messages keep immutable routes. Repeat Next on
the same old message resolves the same destination, never a mutable global index.
Selection checks displayed IDs and invokes get again; no cached amount/state/
revision is returned. Read-only callbacks cannot become create/edit/delete actions.

OOP/design decision: add only TelegramNavigation, encapsulating at most 100 page
contexts, ten-minute expiry and at most 100 prior cursors per context. Immutable
Route records hold only cursor history/selection identity; private Visit records
bind displayed IDs and original chat/message. No formal pattern was needed:
composition and immutable route values solve repeat interactions without a generic
command framework, per-button subclasses, service hierarchy or financial-state cache.
Next pushes current cursor; Previous uses prior cursor; missing history returns
page one. Token expiry/eviction/restart or invalid/stale service cursors refresh
with cleared history. The existing RecentCursors/service/store contract is unchanged.

SDK allowlist now includes callback_query alongside message. Callback sender must
be the single configured non-bot user in that private chat. Controls bind a random
128-bit page token and successfully sent message ID; copied-message controls are
ignored, malformed/expired browse tokens refresh, unrelated callback data is ignored.
Only IDs originally displayed can be selected. Authorized callbacks attempt SDK
AnswerCallbackQuery; an expired/unanswerable query does not strand read-only
processing. Failed/uncertain page sends remain unbound and may require /recent
again; no outgoing exactly-once/delivery claim. Trusted update operation identity,
capture reference/financial replay, synchronous SDK barrier and managed execution
are preserved. Navigation never calls create/recover/pendingCreates.

Full changed paths (project relative; no deleted file/directory):

```text
Added:
apps/server/src/main/java/me/nghlong3004/longnest/telegram/model/TelegramNavigation.java
apps/server/src/test/java/me/nghlong3004/longnest/telegram/TelegramBrowsingTests.java
Modified:
apps/server/src/main/java/me/nghlong3004/longnest/telegram/adapter/TelegramCapture.java
apps/server/src/main/java/me/nghlong3004/longnest/telegram/config/TelegramConfiguration.java
apps/server/src/main/java/me/nghlong3004/longnest/telegram/config/TelegramLifecycle.java
apps/server/src/main/java/me/nghlong3004/longnest/telegram/mapper/TelegramReplies.java
apps/server/src/test/java/me/nghlong3004/longnest/telegram/TelegramAdapterTests.java
apps/server/src/test/java/me/nghlong3004/longnest/telegram/config/TelegramLifecycleTests.java
apps/server/README.md
README.md
docs/BACKEND_RULES.md
docs/SERVICES.md
docs/WORK_STATUS.md
```

All changed handwritten Java uses normal imported short type names, including
previous inline qualified IOException/ObjectMapper/List/Function/Mockito/domain
types in touched files. **No simple-name collision/FQN exception was needed.**
Backend rules now records this convention; generated sources/protected .agents
were not edited. MapStruct remains responsible for representation construction;
manual text/button rendering and navigation are transport behavior, not financial
policy/property copying. No POM/dependency/schema/domain/input/ledger service or
repository/resource/launcher/Compose change; all original base files remain and
manifest comparison confines server edits to the nine listed server paths.
EMPTY-DIRS-1 removals remain absent; accepted evidence and archives are preserved.

New verification on Temurin 25.0.4.1, Maven 3.10.0, Linux/WSL2:

- Initial focused TelegramBrowsingTests/TelegramAdapterTests/TelegramLifecycleTests:
  exit 0, **50 tests**, zero failures/errors/skips (19 new browse cases then).
  `/tmp/longnest-browse.66h2pn/focused.log` retains that first run. A test-stubbing
  unchecked-varargs warning was eliminated without suppression before the final
  build; two additional browse cases cover three-page backtracking and read retry.
- Preserved generated output at `target-before-fresh-build/` in that evidence
  directory; full offline package started with absent apps/server/target, explicit
  empty Maven settings and isolated environment: exit 0, **353 tests**, zero
  failures/errors/skips. These are new results: prior 332 are baseline, 21 browse
  cases are new. `/tmp/longnest-browse.66h2pn/package.log`; fresh Surefire reports
  in apps/server/target/surefire-reports. No test-skipping or dependency changes.
- Standalone spring-boot:test-run / TestLongnestApplication with the same isolated
  settings/environment: exit 0, non-web managed virtual-thread assertion and clean
  context closure. `/tmp/longnest-browse.66h2pn/smoke.log`. Default/smoke remains
  integration-free; existing normal-no-DB negative test still fails closed.
- Inspected JAR: new navigation and SDK modules present, generated ledger mapper
  present; removed raw transport and test-only smoke launcher/YAML absent. Fresh
  MapStruct output retains Spring component/ledger-profile annotations; existing
  mapper/injection and transaction-proxy tests pass. Production/test code did not
  change after this full build/smoke; later integration edits were docs only.
- Tests cover authorized/foreign/bot/group/inline callbacks, message binding,
  first/last/empty and direct-token navigation, two/three-page Next/Previous,
  expiry/restart/stale generation/invalid cursor refresh, capacity eviction,
  bounded history and missing-history fallback, current/deleted/revised selected
  state, repeat clicks, copied/tampered controls, SDK callback answer/send failures,
  stable retry identity and no financial calls. Actual SDK models/markup validation
  and mocked client/services only, not Telegram sockets or PostgreSQL.
- No compile/test failures. One documentation patch rejected stale context before
  changing any files, then was corrected. Existing Mockito attach and Lombok/JVM
  Unsafe warnings remain nonfatal. Static checks confirm imported names, preserved
  base paths, balanced fences and existing local file-link targets in changed docs.
  Public Bot API callback/button documentation and installed SDK API signatures
  were checked read-only; no live bot call was made.

Disposition is bounded technical acceptance, **not** durable L1, live chat UX or
R1–R17 product completion. Still missing: explicit first owner-head initialization
workflow, approved L2 edit/date preview→Save/Cancel and confirmed Delete/Restore,
and L3 inclusive overview/type-range lists with matching full-range summaries and
shared range/filter paging. These remain first-release work, not optional future
budget/classification/salary features. No new product decision is needed for them.
Operator runtime inputs/authorization and PostgreSQL durability evidence above
remain outstanding; no live DB/Telegram/container/private env/paid/deployment action.
Git remains protected/nonrepository: no stage/commit/push/hash or workaround.
Next feature direction is L2 once its documented accepted durable L1 dependency
is established; any independently scoped synthetic preparatory work must preserve
that unmet integration/acceptance dependency rather than claim it satisfied.

### SDK replacement assignment — TG-SDK-1

Lead `8442a05c-6421-45c4-adc7-65b64111c30f` confirmed the stopped TG-CAPTURE-1
writer and compile are quiescent; no other project writer or build observed.
The stop/research-only instruction preserved partial files without acceptance.
New authority permits exactly one implementation Peer for this replacement, not
additional review/implementation tabs. Lead owns shared docs/status/integration;
the Peer alone owns the named server scope after dispatch. No concurrent Lead
server edits/builds while that scope moves. Dispatched Peer:
`31d86be6-f7a4-45a0-b7c4-f8031ba34376`; provider slp-peer / model gpt-6-sol,
high thinking, explicit auto-review workspace-write mode, no feature overrides.
First creation attempt failed before creating a session because mode inheritance
was unavailable; retry supplied the advertised explicit mode and created exactly
one Peer. Finish/error/attention notifications enabled on this Lead session.

Exact ORIGINAL INPUT, **unaccepted partial server snapshot**, 71 files excluding
target: `18cea497f439ed444bafae8e0e71255fafec6b93965af3db68ec2d668bd39c74`.
Manifest/archive: `/tmp/longnest-tg-sdk-base.1u12ml/server-manifest.sha256` and
`server-sources.tar.gz`. Accepted predecessor server/launcher inputs remain
DEV-START-2 (`67bd9bc19c46c83df7c1ecc6ec5681d29a3d510b29a7793259543bcecb7477dc`
server; `d7b0698a9b4ed74a89a7cdf89adfd982538c62635c9d22be877b2fabec87edd0`
six-path launcher/docs). Both are preserved; partial input is not promoted by dispatch.
Git recheck: protected .git 0555/nonrepository, status exit 128; no baseline commit/
push, verified identity/branch/upstream/remote or permission workaround. Missing
usable metadata/destination is operator-owned, not a block on safe code work.

Selected profile: demanding cross-module Peer (`slp-peer`, gpt-6-sol, high thinking;
no mode/features specified by profile; provider required an explicit supported
auto-review mode). Bounded outcome: actually bind
`org.telegram:telegrambots-longpolling:10.3.0` and
`org.telegram:telegrambots-client:10.3.0` through one version property and SDK
session/client APIs. Remove raw HTTP/custom offset polling and redundant fixtures;
retain application auth, finite parser/mapper/services and frozen-operation replay.
Do not blindly add the Boot-3-oriented starter or replace Boot's Jackson 3 mapper.
Use the SDK's required Jackson 2 locally at its transport boundary if necessary;
prove dependency coexistence/Java25/Boot4 binding with synthetic checks.

Peer write scope: apps/server POM and README; telegram production/test packages;
application.yaml's Telegram opt-in section only; existing six ledger recovery
bridge files (LedgerClient, TransactionService/interface implementation, LedgerStore,
InboxRepository, JpaLedgerStore) and focused ledger recovery tests; root server
LongnestApplicationTests for smoke isolation. Generated target/build evidence is
allowed, never a source artifact. No domain/input changes, schema/migration edit,
launcher/deploy/shared-doc/private/protected path changes, new product feature,
DB/container/Telegram/provider call or broad refactor. Reopen a concrete need
outside that scope with evidence before writing.

Acceptance: default-off/no-registration without explicit opt-in; fail-closed
token/numeric-user config, trusted sender/private-chat auth and stable original
event/time identity; exact amounts, unauthorized/ambiguous no-write, distinct equal
events independent; commit before saved receipt; SDK acknowledgment cannot race
ahead of unfinished/undurable handling. Use synchronous completion/durable handoff
before consumer return, not fire-and-forget sample consumers. On failed handoff,
stop/retry safely without allowing SDK's early internal marker to drop events.
Own SDK/consumer/executor/HTTP shutdown explicitly, guard automatic webhook removal,
preserve managed application execution, frozen pending recovery and honest reply-
delivery limits (no exactly-once outgoing-message claim). Synthetic SDK-client/
session tests only, no live socket/DB or actual registration against Telegram.
Full package/existing smoke/MapStruct-Lombok and resource isolation evidence needed;
dependency resolution from public artifact repositories is allowed for this build.
Return exact base/new manifest/full paths/results/failures/limits; stop all writers
at stable submission. Lead independently inspects/verifies and explicitly accepts
or rejects that output; no additional Peer review session.

L1 capture/retrieve, L2 edit/date/delete/restore and L3 ranges remain the approved
first-release ledger; L1 alone and this adapter alone are not completed CRUD.
R1–R17 remain unmet product acceptance.

#### TG-SDK-1 handoff and TG-SDK-2 repair

Peer submitted and stopped all writers/builds; Lead confirmed idle/no active turn,
no Java/Maven process and reproduced the 69-file server manifest
`8c77d76890ebe2fcf780e5fb5a8f554e9d26112ac04bca92ccd30caf8e1ba6a9`.
Immutable archive: `/tmp/longnest-tg-sdk-8c77d76890ebe2fcf780e5fb5a8f554e9d26112ac04bca92ccd30caf8e1ba6a9.tar.gz`,
SHA-256 `6fb2d73da67e4200a8102ffa8b86f1ad02c045a1ea51d44edd57e693680a32bd`.
Peer reports 328 passing tests and standalone smoke; these are writer evidence,
not independent acceptance. Server scope was released to Lead at submission.

**REJECT TG-SDK-1**, code inspection: production HTTP read timeout defaults to
10 seconds and call timeout is 40 seconds, while the actual cached SDK 10.3.0
DefaultGetUpdatesGenerator requests a 50-second poll (verified with javap).
Idle polling therefore times out before the requested server wait. Also startup
recovery queries at most ten pending operations once; later frozen operations
can remain stranded after Telegram retention expiry. Existing tests do not cover
either production timeout coherence or a multi-batch recovery backlog.

TG-SDK-2 repair assigned to the same Peer
`31d86be6-f7a4-45a0-b7c4-f8031ba34376`, exact base TG-SDK-1 above. Scope:
Telegram config/lifecycle/capture, their tests and server README only. Make SDK
poll timeout and HTTP deadlines coherent; drain pending creates in bounded query
batches before registration with interruption/stop checks and no unbounded
in-memory backlog. Retain synchronous SDK acknowledgment, same-event replay,
default-off networking, safe errors and explicit resource ownership. Add focused
regressions exercising actual configured SDK request/deadline values and more than
ten pending operations, then full package/smoke. No other Peer/tab, live network,
database or private state access. Lead owns shared docs/status; Peer exclusively
owns this moving repair scope until its new stable stop-write handoff.

TG-SDK-2 submitted: Peer explicitly stopped all builds/writers and released the
repair scope. Lead confirmed idle/no active turn, no Java/Maven process, and
reproduced the 69-file server manifest
`59509208551e9d446ea912e9dda3551a2e6c9a66d9f7f62050ff9f5fabd27655`.
Frozen manifest/archive are `/tmp/longnest-tg-sdk-2-59509208551e9d446ea912e9dda3551a2e6c9a66d9f7f62050ff9f5fabd27655.sha256`
and the adjacent `.tar.gz`; archive SHA-256
`4b35f76191285a06ab0131f0b86d05e42a59281104c647322ea6dfe406b2be2d`.
Peer reports 332 tests and standalone smoke passing. Lead now owns server scope;
independent fresh package/smoke and exact disposition were pending at receipt and
are resolved below. No Peer writes may resume on a delayed callback without an
explicit repair assignment.

#### TG-SDK-2 — ACCEPT implementation and synthetic/local behavior only

Lead independently accepted the exact submitted server manifest
`59509208551e9d446ea912e9dda3551a2e6c9a66d9f7f62050ff9f5fabd27655`,
repair base `8c77d76890ebe2fcf780e5fb5a8f554e9d26112ac04bca92ccd30caf8e1ba6a9`,
original partial base `18cea497f439ed444bafae8e0e71255fafec6b93965af3db68ec2d668bd39c74`.
TG-SDK-1 remains rejected, not retroactively accepted. Peer acknowledged closure
and no further writes/probes/builds; Lead owns all server/shared-doc integration.
No additional review or implementation sessions were created.

Production uses `org.telegram:telegrambots-longpolling:10.3.0` and
`org.telegram:telegrambots-client:10.3.0`, one `telegrambots.version` property;
actual `TelegramBotsLongPollingApplication`/`BotSession` and `OkHttpTelegramClient`
APIs, not unused declarations. The stopped handwritten route reused JDK HTTP and
Boot's existing Jackson 3 (confirmed in the preserved source). That explains its
implementation shape, not a demonstrated SDK incompatibility. It duplicated SDK
transport/polling; the replacement removes it rather than keeping a fallback.
No Boot-3-oriented starter is added. SDK-local Jackson 2 coexists with Boot
Jackson 3; only the opt-in configuration constructs SDK resources.

The retained six production files are cohesive responsibilities, not one class
per method: configuration creates gated SDK resources; lifecycle owns identity/
webhook checks, synchronous consumer barrier and shutdown; capture authenticates
and routes interpreter/create/get/recent/recovery; replies render authoritative
results; settings validate secret/one allowed user; failure carries sanitized
internal failure state. SDK update objects replace the custom wire model.
Ledger pending-query/recover methods are necessary durable application logic,
not redundant polling infrastructure. No compatibility facade or generic hierarchy.

Independent verification (Temurin 25.0.4.1, Maven 3.10.0, Linux/WSL2):

- Preserved writer target at `/tmp/longnest-tg-sdk-lead.E3WK5k/peer-target/`;
  built from absent `apps/server/target`, offline, empty explicit Maven settings
  and isolated process environment. `package` exit 0: **332 tests, zero failures,
  errors or skips**. This is new evidence, separate from accepted 297 baseline
  and writer's 328/332 results. Logs: `/tmp/longnest-tg-sdk-lead.E3WK5k/package.log`.
- Standalone `spring-boot:test-run` / TestLongnestApplication, same isolated
  settings/environment, exit 0: non-web managed virtual-thread smoke and closure.
  Log: `/tmp/longnest-tg-sdk-lead.E3WK5k/smoke.log`. Existing passing negative test
  still fails normal startup without a DataSource URL, rather than bypassing JPA.
- Inspected fresh generated MapStruct 1.6.3 implementation with ledger profile/
  Spring component; six mapper tests and five proxy tests pass. JAR contains
  SDK longpolling/client/meta 10.3.0, OkHttp 4.12.0 and Jackson 2/3; no custom
  poller/gateway/HTTP client/wire-update classes, test launcher or smoke YAML.
- Regressions inspect actual configured 30-second SDK poll, 35-second read and
  40-second call deadlines; drain 23 frozen operations in bounded ten-item queries;
  stop between recovery operations; failed startup recovery requires operator
  action without an automatic-retry promise. Existing synthetic checks cover
  trusted actor/private-chat identity, ambiguity/no-write, stable event times,
  same-event replay/distinct equal events, receipt failure, SDK batch barrier and
  resource/managed-work shutdown. These are not real PostgreSQL or Telegram proof.
- Source manifest stayed identical through independent package/smoke. Mockito
  attach and inherited JVM/Lombok warnings remain nonfatal. Writer's initial
  public-artifact DNS failure and incomplete synthetic SDK JSON fixture were
  corrected before final full runs; neither is counted as passing evidence.
- Final documentation checks: eight changed Markdown files have balanced fences
  and 79 existing local file-link targets. All 69 submitted files were rehashed:
  only Lead's server README reconciliation differs; no code/config/test/POM drift.
  Launcher source/tests and Compose retain their accepted hashes; they were not
  needlessly rerun. No diagram rendering or live environment inspection is claimed.

Complete SDK-replacement delta from ORIGINAL partial base, relative to apps/server:

```text
Modified:
README.md
pom.xml
src/main/java/me/nghlong3004/longnest/telegram/adapter/TelegramCapture.java
src/main/java/me/nghlong3004/longnest/telegram/config/TelegramConfiguration.java
src/main/java/me/nghlong3004/longnest/telegram/config/TelegramLifecycle.java
src/main/java/me/nghlong3004/longnest/telegram/mapper/TelegramReplies.java
src/main/java/me/nghlong3004/longnest/telegram/model/TelegramSettings.java
src/test/java/me/nghlong3004/longnest/LongnestApplicationTests.java
src/test/java/me/nghlong3004/longnest/ledger/repository/JpaLedgerStoreUnitTests.java
src/test/java/me/nghlong3004/longnest/ledger/service/LedgerUnitTests.java
src/test/java/me/nghlong3004/longnest/telegram/TelegramAdapterTests.java
Added:
src/test/java/me/nghlong3004/longnest/telegram/config/TelegramLifecycleTests.java
src/test/java/me/nghlong3004/longnest/telegram/config/TelegramSdkSessionTests.java
Deleted superseded custom transport:
src/main/java/me/nghlong3004/longnest/telegram/adapter/TelegramPoller.java
src/main/java/me/nghlong3004/longnest/telegram/client/TelegramGateway.java
src/main/java/me/nghlong3004/longnest/telegram/client/TelegramHttpClient.java
src/main/java/me/nghlong3004/longnest/telegram/model/TelegramUpdate.java
```

Deleted source is recoverable from the original partial archive above; no user
financial data was deleted. The seven-file TG-SDK-2 repair subset is README,
TelegramConfiguration, TelegramLifecycle, TelegramCapture, TelegramReplies,
TelegramLifecycleTests and TelegramAdapterTests. Its unchanged carried capture
scope includes application.yaml default-off, TelegramFailure, and the six ledger
recovery bridges: adapter/LedgerClient, service/TransactionService,
service/impl/TransactionServiceImpl, repository/LedgerStore, InboxRepository and
JpaLedgerStore (under the existing ledger package). All are covered by this
bounded full-candidate inspection/local acceptance, not by a prior live run.

Lead reconciled only public documentation after verifying code: apps/server/README
removes the candidate label and stale no-transport/no-auth claims; root README,
docs/SERVICES.md, docs/DELIVERY.md, docs/BACKEND_RULES.md, scripts/README.md and
deploy/README.md now describe the actual SDK binding, config/launcher integration,
Jackson exception and remaining limits. This WORK_STATUS is the sole tracker.
No production/test/resource/POM changes after the verified Peer snapshot.
Final 69-file server manifest including README-only integration:
`1e5468a1c84154a1470e17cb1fd97981c2302fe101db9e358f058d2daaf605dc` —
**ACCEPT** with the same code verification limits. Seven non-status docs combined
hash in order README, Services, Delivery, Backend rules, scripts README, deploy
README, server README: `f1cbe2a8823bca883425e54cb6d66d352140b8f9084559022d0e25c710b1aba2`.

Usable outcome: code for one trusted private user's deterministic income/expense
capture → existing service → accurate receipt, plus /show and text /recent Next,
with frozen replay/recovery and off-by-default networking. Not a proven running
bot, complete L1, or first-release CRUD. Previous/entry buttons, explicit safe
first owner-head initialization and approved L2/L3 code remain unimplemented.
R1–R17 remain unmet; no budget/salary/LLM capability is implied or needed here.

Next smallest working-path prerequisite: operator provides an authorized isolated
PostgreSQL endpoint/DB, DataSource username/password through private process
configuration, synthetic internal owner, and bounded migration/explicit-head-
initialization/save/replay/reopen scope. There is deliberately no automatic head
recreation. Separately, a live Telegram run needs operator-owned bot token, one
numeric allowed user, exclusive bot/process ownership (no competing webhook or
poller), explicit opt-in and approved synthetic messaging/egress scope. No secrets
belong in chat. Root `.env.dev` is read only by the launcher; no private file was
accessed, and Compose neither consumes nor injects Telegram settings. Startup can
run Flyway; it was not executed here. Independent future L1 navigation or explicit
initialization tooling can be implemented synthetically under a named follow-on
scope; missing live evidence alone is not a reason to idle that code work.

Limits: PostgreSQL SQL/query execution, locks, commits/reopen/durable replay and
live Telegram UX are unverified. SDK webhook preflight is not atomic against
another operator; Telegram retention and financial receipt recovery do not provide
an exactly-once/durable outgoing reply queue. Replies may duplicate or be absent.
Protected .git remains 0555/nonrepository (rev-parse exit 128); no stage/commit/
push/hash exists. Operator must supply usable permitted metadata plus verified
identity/branch/remote/upstream before authorized Git work can proceed. No bypass,
DB/container operation, live message, paid call, deployment or private-state change.

#### EMPTY-DIRS-1 — ACCEPT bounded Lead-owned cleanup

After TG-SDK-2 closure, Lead `8442a05c-6421-45c4-adc7-65b64111c30f`
rechecked actual room ownership (same idle, released Peer; no new assignment/tab),
no Java/Maven process and accepted 69-file server base
`1e5468a1c84154a1470e17cb1fd97981c2302fe101db9e358f058d2daaf605dc`.
Inspected public source/layout trees, exact real paths, directory types/contents,
active code/config/docs references and accepted package-move history before deletion.

Removed exactly these **empty directories** with explicit-path `rmdir`:

```text
apps/server/src/main/java/me/nghlong3004/longnest/telegram/client
apps/server/src/main/java/me/nghlong3004/longnest/ledger/persistence
apps/server/src/test/java/me/nghlong3004/longnest/ledger/persistence
apps/server/src/test/java/me/nghlong3004/longnest/ledger/service/impl
```

The client directory lost its only classes in the accepted SDK replacement;
ledger persistence entities/repositories and their test moved to model/entity
and repository in PACKAGE-1A. Service tests deliberately share package-private
helpers under test ledger/service, confirmed in history and actual imports/helper
calls; their empty impl child has no active content/reference or reserved role.
Production ledger/service/impl and Telegram model/adapter packages are populated
and preserved. Retained empty starter/planned resources static, templates and
db/migration: not confirmed superseded packages; the latter is the canonical future
Flyway location. No unfamiliar planned directory was removed merely for being empty.

Verification: all four targets were normal directories, resolved inside the
expected source trees and had zero children (including hidden content); rmdir
exit 0. Post-checks confirm exactly those paths absent and retained active/resource
paths plus named target/evidence/archive paths present. Full server file manifest
is **unchanged** at the exact base hash above, so all 69 accepted files/paths are
intact. Only docs/WORK_STATUS.md content changed; no source/POM/config/test files
were edited or deleted, no generated/build/evidence/temp archive/DB/private/protected
path removed. Empty directory structure can be recreated with mkdir; there was no
file content to recover. Existing historical removed-class references remain
accurate; no active import/config/file link depended on an empty removed directory.

Disposition: ACCEPT this structural-only cleanup; no rebuild/test rerun warranted.
Prior 332-test/smoke evidence is unchanged, not newly rerun or expanded. No live
DB/Telegram/provider calls. Git recheck remains .git 0555/nonrepository, rev-parse
exit 128: no stage/commit/push or workaround. Runtime/L1/L2/L3 limits above unchanged.

### Historical partial implementation — TG-CAPTURE-1 (unaccepted, superseded binding)

Lead `8442a05c-6421-45c4-adc7-65b64111c30f` owns this increment in the existing
two-session room; no Peer/new tab or archived assignment. Accepted original
DEV-START-2 six-path base reproduced:
`d7b0698a9b4ed74a89a7cdf89adfd982538c62635c9d22be877b2fabec87edd0`;
60-file server base `67bd9bc19c46c83df7c1ecc6ec5681d29a3d510b29a7793259543bcecb7477dc`.
Git baseline preflight BEFORE implementation: .git directory 0555, rev-parse and
status both exit 128/not a repository. No baseline stage/commit/push possible;
identity/branch/remote/upstream cannot be established from usable repo metadata.
Operator must supply usable permitted metadata plus intended identity/destination;
no global config/private credentials read or guessed. Continue uncommitted.

Bounded implementation: opt-in/off-by-default Telegram Java 25 HTTP + existing
Jackson 3.1.5 client binding, token/one numeric allowed-user config, private-chat
auth → trusted owner/event/time context, long polling/capture/replies, conservative
ack/replay and frozen-inbox pending recovery. Include simple /show and /recent
text routing because services already exist; defer navigation controls and explicit
owner-head initialization tooling rather than expanding this increment. Shared
ledger changes limited to bounded owner/namespace pending-operation query and
caller recovery through existing create proxy, no migration/schema change.
Acceptance: real processor/client/config/lifecycle code against synthetic HTTP/
service boundaries, no network sockets/DB; exact authorization/amounts/commit/
redelivery/distinct-event/restart/stop/off-by-default checks plus full local build.
Preserve launcher, feature packages, MapStruct/Lombok, smoke and Compose constraints.
L1/L2/L3 naming is correct: all remain approved first-release ledger scope; L1
alone is not CRUD completion and R1–R17 remain unmet product acceptance criteria.
New Git push authority supersedes earlier no-push text only for reviewed project
Git operations. Live network/DB/providers/deployment remain unauthorized. Candidate
and disposition pending; no runtime evidence is claimed by implementation readiness.

### Accepted build-and-run launcher — DEV-START-2

Lead `8442a05c-6421-45c4-adc7-65b64111c30f` retains scripts/shared-doc/server-README
ownership in the unchanged two-session room. No new agent/tab or old assignment
revived. Accepted original DEV-START-1 seven-path manifest reproduced before edits:
`23daaed869ee30a5d3873ee2f70d4edbc0b3d17d1de82ad4c212159fd0d876f6`.
Server base is `6aa56a58354cb02fb8f0a955a05b43af2b4c74a1d7529f52afbe90fae199bed3`;
only its README is in this revision. Existing 36 launcher / 297 Java tests remain
prior evidence. Git is still protected 0555/nonrepository (read-only status exit 128).

Outcome: one existing command packages via installed Maven with tests enabled,
then runs the packaged JAR; no prebuilt artifact requirement or stale-JAR fallback
on failure. Retain literal parser/precedence/root resolution, config checks and
ledger profile. Keep file-only runtime variables out of Maven; verify build-before-
run, failures/status and stopping during both phases with synthetic executables.
No private env access, DB/container/provider start/contact or migration execution.
This explicitly supersedes DEV-START-1's prebuilt-JAR workflow below; no Java/POM,
Compose or persistence settings change.

Final disposition: Lead ACCEPTS DEV-START-2 for automatic Maven package → executable
JAR startup, the retained safe env contract and the evidence below. It supersedes
the prebuilt-JAR requirement of DEV-START-1, not its financial/runtime limitations.
Single command from root: `bash scripts/dev-server.sh`; from another directory:
`bash /home/nghlong3004/projects/longnest/scripts/dev-server.sh`.

Selected flow: installed `mvn -B -ntp package` from apps/server, tests enabled, then
exec Java with root working directory, absolute packaged JAR and explicit ledger
profile. This matches existing documented Maven usage/available Maven 3.10.0 and
avoids wrapper distribution/bootstrap downloads and a Maven runtime parent. No
clean, skipped tests, dependency/install goal, wrapper change or new dependency.
Normal dependency resolution may need repository access on a cold cache; only
offline builds were executed here. Required config/tools fail before building.
Maven receives only original inherited environment, never file-loaded secrets or
file MAVEN_OPTS/JAVA_HOME/PATH values. Explicitly exported caller settings remain
caller-controlled and visible to build plugins/tests. Env grammar, precedence,
required DataSource/owner names and ledger-only selection are unchanged.

A failed package returns its exact status and cannot launch even with an old JAR
present. Only successful packaging proceeds to the readable-output check and Java;
no prebuilt shortcut or failed-build fallback. Package owns artifact freshness; no
unnecessary clean/removal of prior build output was added. Build runs in its own
job process group; INT/TERM forwards to that group and waits, exits 130/143 and
does not start the app. Runtime replaces the launcher PID via exec, preserving
Java status/INT/TERM. Foreground usage is documented; ignored SIGINT inherited
from a caller that deliberately backgrounds commands remains a shell limitation.

Exact changed paths: `scripts/dev-server.sh`, `scripts/dev-server.test.mjs`,
`scripts/README.md`, `apps/server/README.md`, root `README.md`, `docs/DELIVERY.md`
and this sole `docs/WORK_STATUS.md`. No added/deleted project path this increment.
Candidate file SHA-256, in manifest order excluding mutable status:

- `scripts/dev-server.sh`: `d659042ee30bab073272d904ae9bb2d4a977e1d4ed351b56a0274e7b47c2f790`
- `scripts/dev-server.test.mjs`: `ed13f96c8478b25b25438a4bcf8a9a29842e1bc185913167211d107812839dc3`
- `scripts/README.md`: `244597e53259ef7f82f9595b51985801078240038aec1bc5cc08e6d5d8e03bb2`
- `apps/server/README.md`: `627fac243ec1b7e52f5114c0bd4e98e6c42924e056fea73ad9a21a2a8c5fbac1`
- `README.md`: `6d8ea05be5f3b7312776323428559c0c7152567289e0e624ec5d65a0365116cb`
- `docs/DELIVERY.md`: `4d2d68462231ecc4fde7e6310b1962b2d39fe16867307fbf0bc2629f7bacdb77`

Combined candidate: `d7b0698a9b4ed74a89a7cdf89adfd982538c62635c9d22be877b2fabec87edd0`.
Reproduce: project-root `sha256sum scripts/dev-server.sh scripts/dev-server.test.mjs
scripts/README.md apps/server/README.md README.md docs/DELIVERY.md | sha256sum`.
Current server 60-file manifest:
`67bd9bc19c46c83df7c1ecc6ec5681d29a3d510b29a7793259543bcecb7477dc`.
Replacing only its README hash with original
`bf35d9bd94255f4919000ffa43e10cde9ba0b20f5a9dcbb5e541ba818be5b9aa`
reproduces the accepted DEV-START-1 server base above. All Java/POM/resources/tests,
package/MapStruct/Lombok inputs, Compose and deploy README are byte-unchanged.

New verification (Bash 5.3.9, Node 24.22.0, Temurin 25.0.4.1, Maven 3.10.0,
Linux/WSL2; no real private env loaded):

- `bash -n scripts/dev-server.sh`: exit 0.
- `node scripts/dev-server.test.mjs`: exit 0, **43 tests, no failures/skips**.
  Fake Maven creates the synthetic JAR only after the expected package invocation;
  fake Java asserts the build record and new contents. Covers no prebuilt target,
  outside-root/space paths, exact Maven flags/cwd with tests not skipped, absent
  tools/config, file-only secret/build-env isolation, existing quoting/precedence/
  injection/failure cases, success replacing an old artifact, build failure status
  23 while stale JAR remains untouched, missing artifact after success, Java exit
  37, same-PID runtime signals, and SIGINT/SIGTERM during build with confirmed
  build-child exit/no backend launch. Both synthetic full runs passed first time.
- Separate actual **offline package**, exit 0, **297 Java tests, 0 failures/errors/
  skips**, including six generated-mapper/Spring cases and existing isolated
  Spring lifecycle/virtual-thread/no-DB checks. This is a fresh test/package run,
  distinct from the prior accepted 297 baseline. Incremental compile reported
  classes up to date; not claimed as clean compilation or new annotation processing.
  Jar/repackage produced the executable; inspection finds generated mapper and
  normal/ledger YAML, no smoke YAML/test launcher. Existing Mockito attach warning
  remains; expected missing-owner/no-DataSource startup errors are passing negative
  checks, not failed builds. No standalone smoke goal or configured ledger start.
- 21 Markdown files, 191 local links, 56 anchors, balanced fences passed; preservation
  check reproduced all 60 server base hashes except README, plus unchanged Compose
  and deploy README. ShellCheck remains unavailable; no tool installed.

Evidence: `/tmp/longnest-dev-build-check.6WdHQZ/launcher-tests.log`, `package.log`,
synthetic `settings.xml`, `docs-preservation-check.txt` and candidate manifest.
Final launcher fixture tree: `/tmp/longnest-dev-start-VTEcjc/` (fake Maven/Java,
synthetic env/build records only). Actual Maven verification used env -i with only
JDK PATH/MAVEN_SKIP_RC=true, explicit minimal temporary user/global settings, -o,
and `-Dmaven.repo.local=/tmp/longnest-bootstrap-m2`; no Maven rc/private env input or
network dependency resolution. Surefire reports are in apps/server/target.

Usable outcome: the same command now builds/tests automatically before starting
the backend; no manual prebuild is needed. No actual configured launcher invocation
was executed. Normal launch can contact PostgreSQL and apply Flyway, with schema
validation retained and no owner-head seed. Runtime remains blocked on an authorized
isolated PostgreSQL instance/readiness, matching private DataSource/synthetic owner
values and explicit connection/migration/fixture-head/create/get/recent/replay/
reopen scope. A generated JAR is not durable-ledger evidence. Telegram/token binding,
L2/L3 remain absent; Compose loading/storage/loopback and Git protection unchanged.
No private env read/edit, DB/Docker contact/start/install, migration, provider/live
message, paid call, deployment, host config change or protected-path workaround.
All build/test processes completed; no new tabs. DEV-START-2 candidate writes stop
at acceptance; Lead retains ownership for the next explicit scope/runtime return.

### Accepted backend dev launcher — DEV-START-1

Lead `8442a05c-6421-45c4-adc7-65b64111c30f` retains scripts/shared-doc integration
ownership in the unchanged two-session room. No archived assignment reopened or
new agent/tab. Current accepted server base is LOMBOK-1
`3f3c2fcabfae41f25c254217341e223bc2d26fc484c0d9e8ee59a79ac9098e71`;
accepted DEV-PG-1 definition/docs manifest is
`b72cb3fd64d83004e02f8af1bec23aafdeebdd4517527c778c76dbc31d36da46`.
Both reproduced before writes. Delivery base SHA-256:
`6e118f13a2560287d770270518629594e3bd3f393e6a157091d112f2220de31a`.

Bounded outcome: repository-relative Bash backend launcher, strict non-evaluating
root .env.dev loading, inherited-environment precedence, required configuration
checks and exec lifecycle; synthetic tests and minimal usage/config reconciliation.
Do not read/edit real private env files or start Java/DB/containers/providers.
Compose itself remains unchanged: literal environment only, no interpolation or
service env_file. Its current --env-file /dev/null commands do not load .env.dev;
--env-file .env.dev alone would not forward values into this container or Java.
Normal ledger startup may execute Flyway; no such execution is authorized here.
This section supersedes DEV-PG-1's historical "dotenv loading unimplemented"
statements below only for the new explicit backend launcher; no automatic Spring,
Compose, production or base .env loading is introduced.

Final disposition: Lead ACCEPTS DEV-START-1 for the launcher, exact documented
loading contract and synthetic/static evidence only. Stable changed paths:

- Added `scripts/dev-server.sh`, `scripts/dev-server.test.mjs`, `scripts/README.md`.
- Updated `apps/server/README.md`, `deploy/README.md`, root `README.md`,
  `docs/DELIVERY.md` and this sole `docs/WORK_STATUS.md`.

Runtime uses Bash 4.4+/GNU env already present; Java 25 is resolved from inherited
PATH. No app or test dependency added. Resolve root from script location, require
readable root .env.dev and the existing executable JAR, parse literal values without
source/eval, reject unsupported syntax/duplicate keys without value diagnostics.
Snapshot inherited environment before script variables so even colliding names
retain original values; inherited values win, including empty ones. No .env merge.
Require nonblank SPRING_DATASOURCE_URL / SPRING_DATASOURCE_USERNAME /
SPRING_DATASOURCE_PASSWORD / LONGNEST_LEDGER_OWNER. Default/require ledger profile,
pass it explicitly to Java; no new dev profile, relaxed persistence or token binding.
Exec into Java (via env) with no surviving shell wrapper; PWD becomes repository
root. Script accepts no arbitrary args/build command or auto DB/container operation.

Candidate file SHA-256:

- `scripts/dev-server.sh`: `87b339121981b4b8c84a64e69de64108b24c1fb91a29ae1cdaa00e47828676b1`
- `scripts/dev-server.test.mjs`: `855cce37a87986864d4ffce3a6400cbc397801eb8aa64f1b4758fba7abafa68e`
- `scripts/README.md`: `73f1312e4803ccec35095db69ccdd19a0d5a8e9bbf91000a1bfb75e1ec5a0a6a`
- `apps/server/README.md`: `bf35d9bd94255f4919000ffa43e10cde9ba0b20f5a9dcbb5e541ba818be5b9aa`
- `deploy/README.md`: `1ae9801569f86cda1537e6d11d1780eb633f5c84cf47e407428586f32de199cd`
- `README.md`: `81420f58b668d0531f76f03a0055ffa5b297317e087d1551d24af0bd6e34bbf4`
- `docs/DELIVERY.md`: `14f5ec48f106134f0b05ed5fa8ad2fa3c308e511bbe7531050fed4b2063fe7e4`

Combined manifest (excluding mutable status), in the order listed:
`23daaed869ee30a5d3873ee2f70d4edbc0b3d17d1de82ad4c212159fd0d876f6`.
Reproduce from root: `sha256sum scripts/dev-server.sh scripts/dev-server.test.mjs
scripts/README.md apps/server/README.md deploy/README.md README.md docs/DELIVERY.md
| sha256sum`. Current 60-file server manifest is
`6aa56a58354cb02fb8f0a955a05b43af2b4c74a1d7529f52afbe90fae199bed3`;
substituting the original server README hash
`e519852e4acca5631299df21206682fd9295de4f3196c4d16b8f1592e18282bd`
reproduces LOMBOK-1 exactly. Thus all Java/POM/resources/tests/generated-mapper
inputs are unchanged. Compose bytes retain DEV-PG-1 SHA-256
`7dcff1d9626ae7e21b8741aae19178058097c8bb73ccbed624edad5006d3695d`.

New verification on Bash 5.3.9, Node 24.22.0, Compose v5.6.0, Linux/WSL2:

- `bash -n scripts/dev-server.sh`: exit 0. ShellCheck unavailable, not installed.
- `node scripts/dev-server.test.mjs`: exit 0, **36 tests, 0 failures/errors/skips**.
  Synthetic repositories with spaces, outside-root invocation, poisoned unused
  .env, LF/CRLF/no final newline, quoted/literal Unicode/#/=/$/backticks, no command
  execution, env-name collision/precedence/empty cases, required settings/profile,
  malformed/duplicate/BOM/NUL/control/escape/multiline cases, unreadable/missing
  env, missing JAR/Java, exit 37 and same-PID SIGTERM/SIGINT all checked. Only a
  fake Java executable and fake JAR ran; no real JVM/backend/DB/provider process.
- Isolated Compose `config --format json` with --env-file /dev/null versus a
  synthetic file supplying application plus POSTGRES_*/PGDATA keys: exit 0 and
  **identical** rendered models. One service/fixed credentials, loopback and no
  Compose volumes preserved. No service env_file or interpolation references.
  DOCKER_CONFIG points to a fresh temporary directory, env -i equivalent passed
  explicitly by subprocess, automatic env-file loading disabled. No daemon call.
- 21 public Markdown files, 192 local links, 57 anchors, balanced fences pass.
  All 60 server files except README and the Compose byte-preservation check pass.

Final durable local evidence: `/tmp/longnest-dev-start-check.3DnXpT/` contains
`launcher-tests.log`, synthetic.env, compose-rendered.json, compose-check.txt and
docs-preservation-check.txt. Final synthetic fixture tree:
`/tmp/longnest-dev-start-OKpi2u/`. These are synthetic disposable evidence, not
private inputs or another tracker. Earlier harness console output was lost,
causing assertions/readiness timeouts; replaced with synchronous writes and
bounded signal waits. Two hung harness sessions were interrupted (exit 130).
An added PATH assertion initially expected the opposite precedence; corrected to
assert the existing process wins. Those failures are not application results; the
final exact script/test candidate above passed all 36. Existing **297 Java tests
and generated mapper/Spring/isolated smoke remain prior accepted evidence**, not
newly rerun or newly inferred live startup. No Java change warranted repeating them.

Usable outcome: an operator with a built JAR can explicitly supply private dev
configuration through the documented strict root file and launch the existing
ledger-profile process, after runtime permission. This is configuration/lifecycle
availability, not a confirmed start. Compose --env-file .env.dev alone is optional
interpolation input, consumes none of our app keys, and forwards none wholesale to
the DB or separate Java process. Compose control keys could affect Compose itself;
recommended commands retain /dev/null. No Compose env wiring change was made or
needed. Telegram transport/library/token property remains absent, even if arbitrary
token-like environment keys reach Java; there is still no supported token location.

Next smallest working-ledger prerequisite remains operator-authorized isolated
PostgreSQL runtime: usable daemon/start/image/readiness/port inputs or an existing
isolated instance; private matching DataSource and synthetic personal-owner values;
explicit connection/Flyway/fixture-head and bounded create/get/recent/replay/reopen
execution scope. This startup runs normal Flyway/JPA if configured and does not
seed the owner head. Do not run it as a no-DB smoke or disable persistence to fake
success. Existing exact input/local service capabilities remain usable in synthetic
tests, but PostgreSQL behavior and durable L1 are unverified; L2/L3/Telegram are
unimplemented. No further wrapper or future-feature work is a runtime substitute.
No private env read/changed or protected path changed, host config altered, DB/container
started/contacted, migration, installation, live messaging, paid call or deployment.
Git read-only preflight still exits 128/nonrepository; no workaround or commit.
DEV-START-1 writes are stopped at this accepted snapshot; Lead retains integration
ownership. Return on runtime inputs/authority or another explicit bounded task.

### Accepted PostgreSQL dev definition — DEV-PG-1

Lead `8442a05c-6421-45c4-adc7-65b64111c30f` owns one PostgreSQL-only Compose
definition, minimal usage/config docs, root index reconciliation and this status
source. Same two-session room, no new agent or archived assignment resumed.
Accepted server base reproduced unchanged: LOMBOK-1 manifest
`3f3c2fcabfae41f25c254217341e223bc2d26fc484c0d9e8ee59a79ac9098e71`.
No application/POM/private file edits are needed. Existing 297-test results remain
prior evidence; this task requires static definition checks, not another build.

Actual Telegram inspection: no transport/client dependency, polling/webhook code,
token property/binding or dotenv/config import in server source/POM/public YAML.
There is no working token location today. `.env`/`.env.dev` loading is proposed
only in Delivery, not implemented; private files were not read. A future token
property and approved process secret injection must be designed with the adapter,
not guessed or supplied in chat. Compose PostgreSQL credentials are unrelated.

Selected location: `deploy/compose.dev.yaml`, in the root layout's infrastructure
area rather than backend source; new `deploy/README.md` makes dev-only use explicit.
Select official `postgres:18.6-bookworm`, a supported stable release/tag verified
against official PostgreSQL/image sources; no image pulled/inspected locally.
The image Dockerfile at upstream commit
`e00e1bd34ec5c8a8e7ad89b273b3d42efaf6d5bc` declares VOLUME /var/lib/postgresql.
Set PGDATA outside that path in the container writable layer so cluster lifetime
matches container lifetime. No Compose volume/mount entries; disclose the image's
unused automatic anonymous volume and remove it with scoped down --volumes.
No claim of zero image-declared runtime mounts. If literal zero anonymous mounts
is required, that needs a different image/build decision, not a hidden workaround.

Authority is file/docs plus static validation only: no image pull, container/
network provisioning, Docker daemon/socket change, PostgreSQL install/connection,
migration, live message or app start. Compose v5.6.0 is available; isolated
config checks will disable dotenv reading and use an empty temporary Docker CLI
config directory, without private configuration or daemon access. Candidate and
disposition pending; actual runtime inputs/permission still missing.

Final disposition: Lead ACCEPTS DEV-PG-1 for the bounded definition/docs and static
configuration only, not provisioning or working persistence. Exact changed paths:
added `deploy/compose.dev.yaml`, added `deploy/README.md`, updated root `README.md`
index/layout and this sole status source. No server, private or protected file changed.
Server base remains LOMBOK-1
`3f3c2fcabfae41f25c254217341e223bc2d26fc484c0d9e8ee59a79ac9098e71`.

Candidate file SHA-256:

- `deploy/compose.dev.yaml`: `7dcff1d9626ae7e21b8741aae19178058097c8bb73ccbed624edad5006d3695d`
- `deploy/README.md`: `69b862b13cbc854d36b0087bc7df78ae3c21d5964fd1851f45102063eec4a0fb`
- `README.md`: `261a240dbaaf03ef61dc1857c3cda76dfb5166bbd5058722aaeb3a62e58e3ad2`

Combined manifest SHA-256 (project-root `sha256sum deploy/compose.dev.yaml
deploy/README.md README.md | sha256sum`, excluding this mutable disposition):
`b72cb3fd64d83004e02f8af1bec23aafdeebdd4517527c778c76dbc31d36da46`.

New static verification: Docker Compose v5.6.0 `config --quiet` exits 0;
`config --format json` plus assertions verifies exactly one PostgreSQL service,
image postgres:18.6-bookworm, explicit UTF8/C init args, public synthetic-only
credentials, loopback 127.0.0.1:54329 → 5432, restart=no, 30s stop grace and
PGDATA=/tmp/longnest-pgdata outside image VOLUME. No raw/rendered volume, bind,
tmpfs, secret/config mount, build, env_file, privilege or additional service;
`config --services` returns postgres, `config --volumes` returns nothing.
This validates Compose declarations, not image-created mounts or a running service.
Image VOLUME/PGDATA/entrypoint behavior was checked in pinned official source and
current official tag/version documentation; no binary image inspection/pull.

Reproduction from project root, validation only:
`docker compose --env-file /dev/null -p longnest-dev -f deploy/compose.dev.yaml config --quiet`.
Actual check additionally used `env -i`, COMPOSE_DISABLE_ENV_FILE=1 and
DOCKER_CONFIG=/tmp/longnest-compose-check.xeUrqj so no ambient/private config or
dotenv files were read. Rendered model: that temporary directory's
`compose-rendered.json`. It is disposable local evidence, not another tracker.
20 Markdown files, 184 local links, 55 anchors and balanced fences passed.
One upstream raw-source fetch used an incorrect commit URL and failed; corrected
URL from official tag link supplied the verified source. No failed fetch is used
as artifact/runtime evidence. YAML parser/schema checks were available; no package
installation was needed. Prior 297 tests remain prior evidence and were not rerun.

Usage and risks are explicit in deploy/README: scoped up/stop/start/restart and
destructive down --volumes commands, all with --env-file /dev/null. No command was
run except version/help/config. Same-container restart/stop-start retains cluster;
removal/recreation discards writable-layer data. Image still creates an unused
anonymous volume at /var/lib/postgresql; scoped cleanup removes it. No literal
zero-runtime-mount, backup, host-storage absence or production durability claim.
The selected patch/tag is for disposable dev only, not a production DB/version/
role/security policy. Ponytail/backend guidance kept it to one database service,
no app/healthcheck/init/migration stack and no alternate runtime/token binding.

Remaining runtime inputs: authorized operator start/image pull and usable local
Docker context/daemon access (no permission change granted here), available loopback
port and actual readiness. Once running, the chosen dev coordinates/placeholders
are documented; Java still needs explicit DataSource configuration, ledger profile
and synthetic owner, plus connection/Flyway/fixture-head/save/replay/reopen execution
scope. No database exists merely because this file exists. Telegram still needs a
real adapter/library/identity+event contract, token property and approved secret
injection/live-test scope; there is no currently supported bot-token location.
No token requested/read. `.env` app loading stays proposed; no `.env`/`.env.dev`
file or other private state read/changed. Git remains protected 0555/nonrepository,
no commit/initialization/workaround. No DB/container/network provision, DB contact,
migration, installation, live message, paid call, deployment or data egress occurred.
DEV-PG-1 writes are stopped; Lead retains docs/integration ownership in the existing
room, with no new tab or reopened assignment. Next event: authorized runtime inputs
and execution scope become available, not another wrapper/refactor task.

### Accepted Lombok audit — LOMBOK-1

Lead `8442a05c-6421-45c4-adc7-65b64111c30f` owns the bounded source audit,
appropriate boilerplate repair and this status source in the existing session.
Current room still has only the retained Lead and coordination session; all old
assignments stay closed. No new agent/tab. No background build/DB process observed.
Accepted original MAPPING-1 base reproduced before writes: 60-file server manifest
`e8f6c8edf658ee0c23dc082fc771a24404d6604c026ba9d3436afbcad82ac281`.
Its **297 passing tests are prior accepted evidence, not new LOMBOK-1 verification**.
Comparison source/POM/README and pre-change Category bytecode are preserved at
`/tmp/longnest-lombok-base.IXFwPI/`.

Outcome: audit actual bodies class by class and replace only useful equivalent
boilerplate. Read all 36 production and 11 test Java files, including nested types;
generated MapStruct implementation is generator-owned and inspected, never edited.
Selected change: Category's field-assignment-only enum constructor becomes
@RequiredArgsConstructor. Keep nullable UNCATEGORIZED type and implicit private
constructor, exact enum constants and supports/null checks; add no getter or API.
Six injected classes already use required-args generation; five JPA classes already
use getters/protected no-args. No new dependency/config/schema, record annotation,
entity setter/builder/equality or financial/state change. Reopen if actual generated
access/signature/bytecode differs; do not broaden APIs to force Lombok adoption.
Acceptance requires fresh compile/full existing tests, equivalent Category bytecode,
unchanged mapper generation/Spring checks, isolated smoke and precise handoff.
Candidate/disposition pending; DB/live/protected operations remain unauthorized.

Class-by-class production audit (names below cover all 36 source files):

| Class/interface | Decision and body-based reason |
| --- | --- |
| Category | Changed only the plain assignment constructor to @RequiredArgsConstructor; enum-private signature and nullable type retained, no getter added. |
| Money | Retain record compact constructor's exact magnitude bounds, parsing and canonicalAmount conversion; not boilerplate/getter. |
| Note | Retain non-null/control checks, NFC normalization and code-point bound in record construction. |
| TransactionDraft | Retain required-value and type/category compatibility checks in record construction. |
| TransactionType | Plain enum with no assignment constructor/accessors to replace. |
| InterpretationResult | Interface/enums need none; nested Ready rejects null draft, Problem validates non-ready status/nonblank metadata, Ready.status supplies semantic tag. Retain record behavior. |
| TransactionInterpreter | Stateless finite-grammar decisions, no injected fields, handwritten constructor or bean getters. No redundant no-args annotation. |
| LongnestApplication | Framework entrypoint/static startup, no constructor/getter boilerplate. |
| CreateIntent | Retain non-null draft and supported-date checks; record constructor is an invariant boundary. |
| HostContext | Plain trusted-context record; generated component accessors need no Lombok. |
| PersonalOwner | Retain bounded/nonblank principal validation and per-call authorization. |
| LedgerException | Already @Getter for code; explicit constructor deliberately calls super(code.name()) then assigns code. Required-args alone would lose error-message semantics. |
| CreateInbox | Already @Getter/protected @NoArgsConstructor; explicit domain constructor rejects completed intent, freezes/decomposes data; complete prevents receipt replacement, snapshot reconstructs immutable values. |
| EntryValues | Already @Getter/protected @NoArgsConstructor; explicit constructor decomposes validated intent into persisted scalar fields; intent reconstructs checked Money/Note/draft/date. Not an all-field assignment constructor. |
| InboxId | Already @Getter/protected @NoArgsConstructor/@AllArgsConstructor; deliberate two-part key @EqualsAndHashCode retained unchanged. No entity-wide equality added. |
| LedgerEntry | Already @Getter/protected @NoArgsConstructor; explicit constructor creates EntryValues, starts business revision 1, converts source Optional; snapshot converts persistence state. No all-fields bypass constructor. |
| OwnerHead | Already @Getter/protected @NoArgsConstructor; advance checks expected generation/exact increment. No setter or auto-init added. |
| LedgerClient | Already @RequiredArgsConstructor for three final injected dependencies; failure/transaction-boundary logic stays explicit. |
| RecentCursors | Already @RequiredArgsConstructor for Clock/random; initialized cache excluded. Key/Cursor are records, resolve/issue enforce ownership/expiry/capacity, not getters. |
| TransactionMapper | Already @RequiredArgsConstructor for Validator/generated mapper; validation/defaults/fingerprints/replay helpers are deliberate logic. |
| TransactionRepresentationMapper | MapStruct interface; generated Spring implementation owned by processor. No handwritten constructor/getter to convert or duplicate Lombok generation. |
| TransactionInput | Plain validated DTO record, no Lombok. |
| TransactionView | DTO record plus state enum, no Lombok. |
| MutationReceipt | DTO record, no Lombok. |
| TransactionPage | Retain defensive List.copyOf plus fixed size/cap checks in record construction. |
| LedgerResult | Tagged interface/records; Ok rejects null data, Failure has ordinary record components. No redundant annotations. |
| LedgerConfiguration | @Bean factories supply selected owner/clock/cursors, not field getters. No injected-field constructor needed. |
| TransactionService | Application interface contracts, no implementation boilerplate. |
| LedgerQueryService | Application interface contracts, no implementation boilerplate. |
| TransactionServiceImpl | Already @RequiredArgsConstructor for four dependencies; authorization/frozen intent/transaction/replay logic explicit. |
| LedgerQueryServiceImpl | Already @RequiredArgsConstructor for four dependencies; lock/owner/cursor/page logic explicit. |
| LedgerStore | Persistence port; Receipt/Inbox/Entry nested records need no Lombok, no managed entities exposed. |
| JpaLedgerStore | Already @RequiredArgsConstructor for four injected dependencies; actual repository/durability operations, not getters. |
| EntryRepository | Spring Data proxy interface; no handwritten implementation/accessors. |
| InboxRepository | Spring Data proxy interface; no handwritten implementation/accessors. |
| OwnerHeadRepository | Spring Data proxy interface with explicit lock query, not getter boilerplate. |

All 11 test Java files were also read: DomainValuesTests, TransactionInterpreterTests,
LongnestApplicationTests, TestLongnestApplication, TestcontainersConfiguration,
LedgerConfigurationTests, LedgerUnitTests, LedgerProxyTests, RecentCursorsTests,
TransactionMapperTests and JpaLedgerStoreUnitTests. No plain handwritten dependency
constructor or bean getter remains there. MutableClock.getZone/instant/withZone are
Clock contract overrides (constant/alternate-zone/current-time behavior), not bean
accessors to rename. UnitTransactions overrides assert transaction sequencing and
simulate failure. Fixture factories/reflection keep protected entity constructors
protected. Keep all assertions/helpers unchanged; dormant container helper unrun.

No new tests selected for this assignment-only constructor replacement: existing
DomainValuesTests exhausts all seven categories against both types, including
UNCATEGORIZED and null rejection; new bytecode/access comparison adds targeted
evidence without changing fixtures. New verification results are pending, separate
from the accepted 297-test baseline.

Final disposition: Lead ACCEPTS exact LOMBOK-1 60-file server manifest
`3f3c2fcabfae41f25c254217341e223bc2d26fc484c0d9e8ee59a79ac9098e71`
from original MAPPING-1 base above. Only changed server path:
`apps/server/src/main/java/me/nghlong3004/longnest/domain/Category.java`.
Only other project edit is this existing status source. No added/moved/deleted source,
test or dependency; POM, server README, 51 other source/resource files byte-identical.
The six appropriate injected classes and five JPA types needed no repair; the
body-based retained exceptions above are intentional, not blanket exemptions.
Ponytail/backend guidance kept the change to the one remaining pure assignment
constructor instead of annotating behavior, records or generated mapper code.
Enum-private generation was checked against [Lombok's constructor documentation](https://projectlombok.org/features/constructor)
and actual compiled output, not assumed from annotation presence.

**New LOMBOK-1 verification**, distinct from prior accepted MAPPING-1 evidence:

- Temurin 25.0.4.1, Maven 3.10.0, Linux/WSL2; preserved target, then ran offline
  `mvn -o -B -ntp -Dmaven.repo.local=/tmp/longnest-bootstrap-m2
  -f apps/server/pom.xml package` from absent target. Exit 0, compiled all 36
  production and 11 test sources; **297 tests passed**, no failures/errors/skips.
  No new tests claimed; existing category/type matrix and null tests reran.
- Pre/post `javap -p -s -c` comparison asserts equivalent private constructor,
  fields/method descriptors and instructions, including supports checks and nullable
  UNCATEGORIZED initialization; only method ordering and constant-pool indexes
  ignored. Class files are not claimed byte-identical (generated metadata/debug
  information can differ). No extra getter/API or serialization change selected.
- Generated TransactionRepresentationMapperImpl equals MAPPING-1's generated source
  after removing only its generation timestamp. Six real Spring injection/profile/
  mapping tests reran; JAR contains the generated mapper/API, original YAML/schema,
  and no smoke/test launchers/Mockito resource or mapper processors.
- Documented standalone `spring-boot:test-run`/test main: exit 0, non-web managed
  virtual-thread smoke passed and clean shutdown. Isolated normal JAR (`env -i`,
  classpath application.yaml, keep-alive false): expected exit 1, missing DB URL;
  no DB connection, migration, token or provider call. Existing Flyway/JPA retained.
- 19 Markdown files, 182 local links and balanced fences passed; no new anchors.
  Audit-table names exactly match all 36 production filenames. Initial table-count
  probe mistakenly included its Markdown delimiter row; corrected filename-set
  comparison passed. This text-probe failure is not a product/test failure.
  Nonfatal inherited Lombok Unsafe/Mockito warnings remain, not acceptance gaps.

Generated logs/reports: `apps/server/target/lombok-1-package.log`,
`lombok-1-smoke.log`, `lombok-1-normal-no-db.log`, `lombok-1-jar-entries.txt` and
`surefire-reports/` under that target. Pre/post Category javap and mapper comparison
copy are under `/tmp/longnest-lombok-base.IXFwPI/`; prior generated target is preserved
there as `target-before-lombok/`. Earlier MAPPING-1 logs were copied back to target
so existing README reproduction/evidence pointers remain valid. Local generated
evidence/archives are disposable, not commits or permanent backups. No data/source
was materially deleted. Server manifest remains unchanged after verification.

Current capabilities and frontier:

| State | Usable result or remaining limit |
| --- | --- |
| Verified local/synthetic | Exact bounded VND/note/category values and finite Vietnamese interpretation into drafts/problems; callable authorized create/get/recent, frozen-intent/original-receipt and cursor logic exercised through unit/proxy tests; Spring-injected generated DTO mapping; safe managed-thread bootstrap. JPA/Flyway source compiles, not a proven durable database. |
| Unverified persistence | Actual PostgreSQL migration/Hibernate validation, row locking, rollback/concurrent commit, same-operation reconciliation and reopen/restore behavior. No durable saved-ledger/bot outcome claimed. |
| Unimplemented / unauthorized runtime | Authenticated Telegram transport, real chat UX, L2 edit/delete/restore and L3 range reporting; future budget/classification/allocation/LLM/web. No live messaging, paid calls, deployment or real-data egress granted. |

Next smallest working-ledger prerequisite: host/Human supplies an isolated synthetic
PostgreSQL database plus approved credential/connection/migration/fixture/replay/
reopen scope. Return event: those inputs become available; Lead then verifies
durable L1 before L2, with actual authentication/Telegram separately authorized.
Git metadata remains protected 0555/nonrepository (status exit 128); no commit,
initialization, permission workaround or external push. No DB installation/
connection, private/protected-path change or host configuration change occurred.
LOMBOK-1 is closed; Lead retains server/shared-doc ownership, no active writer/build,
new tab, revived archived assignment or assumed inherited notification path.

### Accepted MapStruct adoption — MAPPING-1

Lead `8442a05c-6421-45c4-adc7-65b64111c30f` owns this bounded change within
the existing session: server POM, ledger mapper Java, affected tests/server README,
canonical backend/dependency wording and this sole status source. Current room
contains only the retained Lead and coordination session; archived assignments
remain closed. No dispatch/new session. No background build/DB process observed.
Accepted base independently reproduced: PACKAGE-1A server manifest
`43bb0ea1c97a6407635ed690dca36e008183af59a95277629ef623117c8d9f1f`.
Comparison copy: `/tmp/longnest-mapstruct-base.FQjobd/`.

Outcome: use Spring-managed MapStruct generation for suitable DTO representation
mappings while preserving explicit validation, exact money/date/error handling,
authorization, replay construction and smoke isolation. Add only the API dependency
at existing `${mapstruct.version}` (1.6.3); preserve both processor/binding setups.
Current evidence explains handwritten fallback: API absent despite processor paths,
canonical docs still called MapStruct proposed, and original L1 store scope froze
POM/dependency changes. No demonstrated incompatibility justified it.

Plan: keep TransactionMapper's validation/conversion/replay helpers; inject one
generated representation mapper via its constructor. Verify generated source and
Spring profile/discovery/injection, complete mapping fixtures and all existing
tests, fresh compilation, standalone isolated smoke and fail-closed normal startup.
Reopen on incompatible processor/API behavior rather than silently changing versions
or weakening checks. No schema/runtime-config/service-policy changes, DB connection/
installation, real messaging, paid call, deployment or protected-path workaround.
Candidate/disposition pending; PostgreSQL durability and Git blockers unchanged.

Implementation candidate: 60 server files excluding target, manifest
`e8f6c8edf658ee0c23dc082fc771a24404d6604c026ba9d3436afbcad82ac281`.
Changed server paths: `pom.xml`, `README.md`,
`src/main/java/me/nghlong3004/longnest/ledger/mapper/TransactionMapper.java`,
added sibling `TransactionRepresentationMapper.java`, affected
`src/test/java/me/nghlong3004/longnest/ledger/service/LedgerUnitTests.java` and
`LedgerProxyTests.java`, added
`src/test/java/me/nghlong3004/longnest/ledger/mapper/TransactionMapperTests.java`.
Shared docs changed: BACKEND_RULES, DECISIONS, ARCHITECTURE and this source.

Focused tests: 6 passing, real generated mapper and real Spring scanning/injection
under active/inactive ledger profiles; no DB auto-configuration. Original validators,
money/date/default logic, fingerprint and micros helpers are byte-identical; 47
other existing source/resource files unchanged. POM comparison proves only one
API dependency added; both existing processor/binding blocks byte-identical.
Generated source inspected: MapStruct 1.6.3 on Java 25, Spring @Component and
explicit ledger @Profile, exact String.valueOf(long) IDs/revision/generation and
Money.canonicalAmount expressions, unchanged original-receipt construction.

Initial offline check failed because the API POM/JAR was not cached; sandboxed
online resolution then failed on DNS. Approved network-enabled Maven resolution
succeeded, focused tests passed; nonfatal inherited repository-metadata 401 and
Lombok/Mockito warnings remain. No credentials/global config changed. Two README
patches failed context validation without editing that file; corrected patch applied.
Full fresh-output offline verification is in progress, not yet acceptance. Previous
target is recoverably preserved at
`/tmp/longnest-mapstruct-base.FQjobd/target-before-mapping/`, with initial resolution/
focused logs beside it. No source deletion or archive/session resumption.

Final disposition: Lead ACCEPTS exact MAPPING-1 server manifest
`e8f6c8edf658ee0c23dc082fc771a24404d6604c026ba9d3436afbcad82ac281` for
MapStruct adoption and preserved synthetic behavior, from exact PACKAGE-1A base
above. Server hash reproduced unchanged after verification. No further candidate
writes; Lead retains server/shared-doc scope. No new agent, revived assignment or
inherited callback. Existing two-session room preserved.

Acceptance evidence (Temurin 25.0.4.1, Maven 3.10.0, Linux/WSL2):

- Preserved previous target, then offline `mvn -o -B -ntp
  -Dmaven.repo.local=/tmp/longnest-bootstrap-m2 -f apps/server/pom.xml package`
  from absent target: exit 0; compiled 36 production and 11 test files; **297 tests,
  zero failures/errors/skips** (all original 291 plus six mapper cases).
- Real package-scanned Spring context injects the generated implementation and
  Validator into the facade; inactive profile has neither mapper bean. Boundary
  mapping fixtures assert every field, 1/10^12 money, BIGINT maximum IDs/generations,
  absent source time, deleted state, immutable bounded ordered pages and original
  replay. Existing service/proxy assertions are unchanged except actual generated-
  mapper construction/imports, preserving auth/rollback/commit-uncertainty tests.
- Documented standalone `spring-boot:test-run` and test main: exit 0, managed
  virtual-thread smoke passed and context closed. Isolated `env -i` normal JAR
  with classpath application.yaml: expected exit 1 for missing DB URL; PostgreSQL/
  Flyway/Hibernate remain enabled, no DB connection or provider activated.
- JAR contains TransactionRepresentationMapperImpl and mapstruct-1.6.3 API, not
  mapstruct-processor/binding or test smoke YAML/launchers/Mockito resource. Runtime
  YAML and migration retained. Generated mapping source read and assertions passed;
  compiler emitted actual DTO construction, not a handwritten generated stub.
- 19 Markdown files: 182 local links, 54 anchors and balanced fences passed.
  Active canonical proposal wording replaced by selected T12/backend mapping rule;
  historical candidates remain history. Diagram rendering not claimed.

Durable local reproduction/evidence: server README, generated
`target/generated-sources/annotations/.../TransactionRepresentationMapperImpl.java`,
`target/surefire-reports/`, `target/mapping-1-package.log`,
`target/mapping-1-smoke.log`, `target/mapping-1-normal-no-db.log` and
`target/mapping-1-jar-entries.txt` under `apps/server`. These generated files and
temporary comparison/archive directories are local/disposable, not commits/backups.
No build/background writer remains. The only new dependency is the requested API;
MapStruct version/property, Lombok setup and all other dependency versions retained.
Ponytail/backend guidance limited generation to representation work and retained
manual financial/provenance helpers instead of adding a validation/policy framework.

Usable downstream input: the same local create/get/recent services now consume a
Spring-injected generated DTO mapper with compile-time unmapped-target checking.
This verifies Java 25 processing/Lombok coexistence and local injection, not a whole
PostgreSQL deployment or authenticated bot. Next runtime input is still host/Human's
isolated synthetic PostgreSQL database and approved credential/connection/migration/
fixture/replay/reopen scope. Return checkpoint: those inputs become available; no
additional mock wrapper or L2 task is selected meanwhile. Git status still fails
128 against protected 0555/nonrepository metadata; no commit or permission workaround.
No DB installation/connection, live messaging, paid/model runtime call, deployment,
private-state/credential change or protected-path write occurred.

### Accepted package refactor — PACKAGE-1A (supersedes PACKAGE-1)

Recovery 2026-10-21: Lead `8442a05c-6421-45c4-adc7-65b64111c30f` recovered
the queued package-architecture assignment and terminated predecessor Lead
`7e3bd3ce-639d-4211-8e54-216a7ca8ad79` through Paseo before affected writes.
Prior Peer assignments remain closed; no Java/Maven/Gradle/PostgreSQL process was
observed. Old callbacks do not revive ownership. No inherited notification path
is assumed; a fresh assignment will request completion delivery to this Lead.

Accepted base independently reproduced: 58-file server manifest
`7233a964e531713c0a747b4a8d4e2957855664c9dbfb0325fa9cb2007b856305`.
Recovery did not rerun accepted tests. Peer
`cecc95b2-4465-4d9a-a497-df9d74d63163` now owns the ledger main/test Java
subtrees and server README only. Shared docs/status remain Lead-owned throughout;
Lead does not concurrently edit that moving scope. Selected saved Peer profile:
`slp-peer-gpt-6-luna-medium`, provider/model `slp-peer/gpt-6-luna`, medium thinking,
auto-review mode; no profile feature overrides. This profile fits the bounded
mechanical refactor with explicit contracts/checks. New creation requested finish,
error and attention notifications to current Lead; delivery is pending, not inherited.
Base comparison copy: `/tmp/longnest-package-base.RitozZ/`, never a second tracker.

Outcome: reorganize the existing ledger by feature/responsibility, as selected in
[Backend rules](BACKEND_RULES.md#feature-package-organization), preserving existing
domain, authorization, transaction/replay and schema behavior. This enables clear
navigation and scoped maintenance of the local backend, not a durable-ledger claim.
No speculative abstractions, dependencies or new financial/transport features.
Synthetic/local clean-build and smoke authority applies; no DB/remote/protected
operations. PostgreSQL and Git blockers below remain separate from this ready work.
Acceptance requires a stable exact manifest, complete moves/access changes,
unchanged protected base inputs/schema/config, all existing tests passing from a
clean compile, packaged-class inspection and preserved isolated smoke behavior.

PACKAGE-1 submitted by Peer `cecc95b2-4465-4d9a-a497-df9d74d63163`; all
writers/builds stopped and Java/test/server-README scope released to Lead. Fresh
completion notification reached current Lead, verifying delivery for this assignment.
Candidate (58 server files excluding target):
`5d05c0b56b1d3082c4d89ebb839498a3ff8ef3c42c21f06c72ffcefdee28417a`.
Writer reports fresh-output offline package exit 0, 291 tests with no failures,
errors or skips, standalone smoke exit 0 and isolated no-DB normal start expected
exit 1. Earlier build-directory property override did not redirect output; its
target was preserved at `/tmp/longnest-package.pRbKq9/target-preserved/` before
the successful absent-target build. No dependencies downloaded/changed. These
are writer evidence, not acceptance; independent Lead verification is pending.
No Peer callback authorizes changes to this frozen candidate.

Lead reproduced that manifest and confirmed idle/no active turn and no background
build. REJECT PACKAGE-1 for final acceptance pending narrow integration cleanup:
new imports need canonical ordering/removal of unused/self imports, and README
must give reproducible fresh-target commands and correct archived-evidence paths.
No production logic defect was found: all 27 production bodies match base apart
from declarations/imports/whitespace and the six required access promotions;
18 unaffected source/resource files plus POM remain byte-identical. Entity guards
and protected no-arg constructors are retained. Lead owns PACKAGE-1A repair from
the exact submitted hash: imports and server README only, then fresh independent
compile/package/smoke, new snapshot and explicit disposition. Peer scope stays
closed; no additional writer is assigned.

Peer acknowledged closure and no further changes/probes. Lead cleanup also restores
the moved LedgerException import for service-interface `@throws` Javadoc. A first
independent fresh-output package passed 291 tests; final import-complete source is
being verified from another absent target. A strict comparison probe initially
flagged blank-line differences after removing imports; corrected whitespace-aware
comparison confirms all 32 Java files differ from Peer submission only in imports/
whitespace. One README patch failed context validation without changing that file;
the corrected patch applied. Neither failed probe is behavioral evidence.

Final disposition: Lead ACCEPTS PACKAGE-1A exact 58-file server manifest
`43bb0ea1c97a6407635ed690dca36e008183af59a95277629ef623117c8d9f1f` for the
bounded structural refactor. Original PACKAGE-1 remains rejected/superseded, not
silently accepted after changes. Lead's final Java changes from it are imports/
whitespace only, including Javadoc exception imports; README now gives an actual
fresh-target reproduction and points to preserved historical evidence. Ponytail
and backend guidance kept the work to existing responsibilities without new
abstractions, APIs, dependencies or financial behavior.

Complete moved-file inventory, relative to the ledger main Java package (all
`.java`; no class removed or added):

| Original location | New location | Classes |
| --- | --- | --- |
| root | `model/` | CreateIntent, HostContext, LedgerException, PersonalOwner |
| `persistence/` | `model/entity/` | CreateInbox, EntryValues, InboxId, LedgerEntry, OwnerHead |
| root | `dto/` | TransactionInput, TransactionView, TransactionPage, MutationReceipt, LedgerResult |
| root | `service/` | TransactionService, LedgerQueryService |
| root | `service/impl/` | TransactionServiceImpl, LedgerQueryServiceImpl |
| root | `repository/` | LedgerStore |
| `persistence/` | `repository/` | JpaLedgerStore, EntryRepository, InboxRepository, OwnerHeadRepository |
| root | `mapper/` | TransactionMapper |
| root | `pagination/` | RecentCursors |
| root | `config/` | LedgerConfiguration |
| root | `adapter/` | LedgerClient |

Five test moves: root LedgerConfigurationTests → `config/`, root RecentCursorsTests
→ `pagination/`, persistence/JpaLedgerStoreUnitTests → `repository/`, root
LedgerUnitTests and LedgerProxyTests → `service/` (shared package-private test
helpers stay local). JPA test uses a reflective fixture constructor so production
OwnerHead's no-arg constructor remains protected. All assertions/fixtures otherwise
remain unchanged. Six required public-access promotions: CreateInbox constructor,
complete and snapshot; LedgerEntry constructor and snapshot; OwnerHead.advance.
Fields stay private, no setters, guards and JPA protected no-args unchanged.

Lead independently verified on Temurin 25.0.4.1/Maven 3.10.0/Linux WSL2:

- Final offline `package`, after preserving intermediate output and starting with
  no target: exit 0; compiled all 35 production and 10 test source files; 291 tests,
  zero failures/errors/skips. No clean plugin download or POM changes.
- Standalone `spring-boot:test-run` with the documented test main: exit 0,
  non-web smoke and managed virtual-thread assertion, clean shutdown.
- Isolated `env -i` normal JAR with only classpath application.yaml: expected exit
  1 for missing DataSource URL; Flyway/Hibernate remain enabled, no DB contacted.
- JAR inventory asserts all 27 moved production classes, no legacy ledger root or
  persistence classes, no smoke YAML/test launchers/Mockito resource, with normal/
  ledger YAML and original migration retained.
- Exact inventory comparison: 32 unique Java moves; 27 production bodies match
  accepted base except package/import/whitespace and the six reviewed access
  changes; 18 unaffected source/resource files and POM byte-identical. No original
  source lost, no compatibility facade or new class. Snapshot unchanged after tests.

Current generated evidence: `apps/server/target/surefire-reports/`,
`package-1a-package.log`, `package-1a-smoke.log`, `package-1a-normal-no-db.log` and
`package-1a-jar-entries.txt` under that target. Prior evidence is recoverably
preserved at `/tmp/longnest-package.pRbKq9/target-preserved/`; Peer output at
`/tmp/longnest-package-lead.Xsol9U/peer-target/`; intermediate Lead output at
`/tmp/longnest-package-lead.Xsol9U/intermediate-target/`. These are disposable
local evidence, not commits or permanent backups. No material source deletion.
An initial Node child-process JAR probe reported EPERM; direct shell `jar tf` plus
listing assertions passed. Nonfatal inherited Lombok/Mockito warnings and a JVM
performance-data file-lock warning remain separate from successful checks.

Lead-owned docs changed: BACKEND_RULES package convention, feature-map stale
authority/readiness text, and this sole status source. Server README is part of
the candidate. Local link/anchor/fence checks passed; no diagram rendering claimed.
Peer closure acknowledgment reached current Lead; no writers/build processes remain.
Server and shared-doc scopes are Lead-owned; PACKAGE-1/1A are closed. Old callbacks
grant no new writes. No inherited monitoring/notification path is claimed.

Usable outcome: navigate and call the same local create/get/recent capability via
`ledger.adapter.LedgerClient`, `ledger.service` interfaces and `ledger.mapper`,
with models/DTOs/JPA responsibilities separated. This is not a public Java API
compatibility promise or a working bot. Next ready runtime step still needs host/
Human to supply isolated synthetic PostgreSQL connection/credential mechanism and
bounded migration/fixture/replay/reopen execution scope. Return event: those inputs
become available, then Lead assigns database verification. No further synthetic
wrapper or L2 work is selected while durable L1 is unverified. Authentication and
actual Telegram transport remain unimplemented and live messaging unauthorized.
Git commits separately await usable, effectively writable metadata; no workaround,
commit, deployment, DB installation/connection or paid/model runtime call occurred.

### Earlier next-step assessment — PostgreSQL frontier (before PACKAGE-1)

Lead rechecked current files, accepted snapshot and room activity. Server manifest
is still `7233a964e531713c0a747b4a8d4e2957855664c9dbfb0325fa9cb2007b856305`.
Closed continuation/review assignments remain closed; no new Peer assignment.
Older sessions previously terminated are now listed idle/no active turn, with only
their old quota-error activity; that lifecycle display grants no renewed ownership.
Current Lead remains the only assigned owner. No superseded session was resumed.

The accepted `TransactionMapper.input` already adapts interpreter drafts, and
`LedgerClient` is the local synthetic create/get/recent caller. Another wrapper or
mock-backed command harness would not resolve the next missing usable outcome:
actual durable save/replay/reopen. No independently useful additional synthetic
increment was selected; do not add replacement storage or advance L2 from an
unverified durable base. This applies Ponytail's smallest-complete-change rule;
it does not assert that all future transport work is implemented.

Missing input for the next bounded implementation/verification step: host
operator/Human identifies an isolated synthetic PostgreSQL database/schema,
provides an approved connection/credential mechanism and execution scope covering
Flyway, explicit fixture owner-head initialization and synthetic persistence/
replay/reopen checks. No instance provisioning, installation, socket/permission
change or arbitrary endpoint probing is inferred. Return event: that isolated
connection and scope become available; Lead then dispatches PostgreSQL verification.
Git is a separate commit blocker, not an application-code dependency: host must
provide usable Git metadata with effective filesystem and managed-policy write
access. Current `.git` remains empty/read-only 0555; optional-lock-free status
still exits 128 (not a repository), so no commit exists.

Existing reports still show 291 synthetic tests passing, including accepted
bootstrap smoke/proxy checks; no build/test was repeated for this assessment.
No PostgreSQL tools were found on PATH or database process observed. Real
migration/locking/durability and actual Telegram behavior remain unverified;
authentication/Telegram transport and L2/L3 are unimplemented. Product acceptance
criteria stay outstanding, not waived. No candidate source, dependency, private
configuration or protected path changed; affected work stops at these inputs.

### Recovered bootstrap assignment — BOOTSTRAP-1

Writer: Peer `06b08ca3-0e21-427f-bc02-7a0c9cc6203e`, `apps/server/**` only;
candidate submitted, writes released to Lead. Peer confirms all processes stopped;
Lead observed idle/no active turn and no Java/Maven background process. Base: 12 unchanged starter files,
sorted checksum-manifest SHA-256
`e238e28a5b1b0c1638b4cc995f54d980e3749bd2fb355e63e4133d1fa5d55ff2`.
Profile rechecked: configured general Peer, `slp-peer` / `gpt-6.1-sol`, medium
thinking; existing continuation retained. Outcome: compile/package existing starter,
explicit non-web/no-DB/no-provider smoke and managed virtual-thread evidence,
without weakening normal PostgreSQL/Flyway validation or claiming L1.
No live integration, container provision, model call or private configuration read.
Reopen on incompatible bindings, required external authority or shared-contract change.
Return exact changed paths/base/candidate hashes, commands/results and limits; stop
all candidate writers at submission. Lead inspects and accepts/rejects exact output.
An explicit resumed prompt with `notifyOnFinish` restores completion/error/attention
delivery to this Lead; old parentage/callbacks are not assumed inherited.

Accepted BOOTSTRAP-1: 14-source-file manifest SHA-256
`98f11c3b81c639736d14c8edab5d5b6b06edbef52730114760e260ed872a3061`;
Lead independently reproduced hash. Changed paths: server application YAML,
application-class formatting, application tests, test launcher, test-only smoke
YAML and server README. No POM/wrapper/Dockerfile changes. Writer reports package
exit 0/two passing tests, standalone smoke exit 0 and normal no-DB exit 1;
Lead independently reran offline package (exit 0; 2 tests, no failures/errors/skips),
standalone smoke (exit 0; managed virtual-thread assertion/clean shutdown) and
isolated normal JAR (expected exit 1; missing DB URL, Flyway/Hibernate retained).
JAR inspection confirms only normal application YAML/main class, no test smoke
configuration/launchers. Source hash unchanged after verification; no background
Java/Maven process remains. Lead ACCEPTS this exact candidate for bootstrap only:
safe synthetic context/build works without pretending PostgreSQL or ledger success.
PostgreSQL migration/validation/concurrency/restart/recovery and Telegram remain
unrun/blocked, not accepted. Generated evidence/reproduction is in server README
and target/surefire-reports, target/bootstrap-smoke.log and
target/bootstrap-normal-no-db.log. Existing dependencies retained; no versions
changed. Initial DNS, RTK wrapper execution and intermediate import failures were
resolved by writer; nonfatal dependency/Unsafe warnings remain documented there.
Peer may not resume this candidate without a named further assignment.

Lead-owned reconciliation: README/Delivery/Decisions now route current implementation
authority to this source instead of claiming docs-only authority or an absent
backend skill. Root ignore rules retain private `.env`/`.env.*` exclusions, allow
only the documented optional `.env.example`, and exclude `.codex/` and `.aws/`.
No private files read or changed; no example environment file created. Text checks
passed for 18 Markdown files, 174 local file links and balanced fences. Git ignore
engine verification remains unavailable because `.git` is not a repository.

### Pure input increment — L1-INPUT-1 and repaired L1-INPUT-2

Accepted input: BOOTSTRAP-1 exact source manifest above plus canonical Domain
first-release grammar/amount/note rules and Services' parser error vocabulary.
Writer: Peer `06b08ca3-0e21-427f-bc02-7a0c9cc6203e` for new domain/input Java and
matching test packages beneath `apps/server/src/{main,test}/java/me/nghlong3004/longnest/`,
and the server README only. No changes to accepted bootstrap/config/POM or shared
docs without reopening with Lead. Lead retains integration/status/shared docs.
Outcome: exact bounded VND value, type/category/note invariants and pure deterministic
text interpretation into a ready draft or explicit clarification/rejection, never
a saved receipt. No persistence/ID/auth/transport simulation, new dependencies,
future finance/date-language expansion or L1 completion claim.
Verify canonical examples and boundary/adversarial fixtures, all listed aliases,
Unicode/code-point rules, decimal exactness, cue conflicts and unsupported input;
run offline full package and preserve bootstrap checks. Reopen on contract ambiguity
instead of inventing product semantics. Return source/base/candidate hashes and
complete evidence; submission stops writes. PostgreSQL integration stays blocked,
but does not block this independently testable input increment.

L1-INPUT-1 candidate submitted; all writer processes exited and scope released to
Lead for verification. Source manifest (23 server files excluding target):
`3951d64f00b1a06eef3e4b6db56eea1189a5bfcd6395d8a52b4d420d22d375d8`,
independently reproduced by Lead. Complete changed paths: server README; domain
Category, Money, Note, TransactionDraft, TransactionType; input InterpretationResult
and TransactionInterpreter; matching DomainValuesTests/TransactionInterpreterTests.
Writer reports package exit 0, 233 tests (71 domain, 160 interpreter, 2 bootstrap),
no failures/errors/skips; accepted bootstrap/config/POM unchanged. Superseded by
the rejection and repair disposition below.
Fresh read-only Peer `ef1cc5f3-a275-462c-94e9-5717e1b3d6ac` examines this exact
candidate for false-ready/monetary/Unicode/grammar bugs. General Peer profile
`slp-peer` / `gpt-6.1-sol`, medium thinking; provider has no read-only mode,
so explicit no-write assignment under normal permissions, no write ownership.
Lead independently reran full offline package: exit 0, 233 tests with no
failures/errors/skips; source manifest unchanged. Read-only inspection reproduced
two defects: U+FB29 in a signed description falsely returns READY; syntactically
valid zero/over-limit decimal suffix amounts are mislabeled AMBIGUOUS_AMOUNT.
Lead ACCEPTS those findings and REJECTS exact L1-INPUT-1 candidate
`3951d64f00b1a06eef3e4b6db56eea1189a5bfcd6395d8a52b4d420d22d375d8`
for input acceptance (bootstrap remains accepted). No persisted monetary failure
was observed. Reviewer stopped its JShell probe (exit 130 after a preferences-flush
failure); source manifest matched before/after, no repository writes or ownership.

Repair assignment L1-INPUT-2: Peer `06b08ca3-0e21-427f-bc02-7a0c9cc6203e`
owns only input/domain source and matching tests plus server README, starting from
the rejected exact candidate above. Add the demonstrated sign rejection and exact
invalid-versus-ambiguous classification with regressions; preserve ambiguous grouping/
unsupported precision and all prior behavior. No bootstrap/POM/config/shared edits.
Lead does not modify that moving scope. Return a new immutable manifest and full
package evidence, then stop writers for a new disposition. Reviewer scope is closed;
future callbacks do not authorize it to write or reopen the candidate.

L1-INPUT-2 submitted; all writers/processes exited, scope released to Lead.
Candidate manifest (23 server files excluding target):
`725de47f11e11d226400d5bbb07bfccb4957c6041a0a362fb035c81e18d4190c`,
independently reproduced from project root. Only TransactionInterpreter,
TransactionInterpreterTests and server README changed from rejected L1-INPUT-1.
Writer reports nine regression fixtures failed before repair and now pass; full
offline package exit 0, 242 tests, no failures/errors/skips. Lead inspected the
two predicate changes and regression assertions; independent full offline package
rerun exits 0: 242 tests, no failures/errors/skips (71 domain, 169 interpreter,
2 bootstrap). Before-repair log confirms the nine expected failures; prior grouping/
precision tests remain green. Source manifest unchanged after verification.
Lead ACCEPTS exact L1-INPUT-2 candidate above for pure input/domain behavior only:
it supplies exact validated drafts or explicit problems, never saved receipts.
Both inspection findings are resolved; no further independent pass is asserted.
Peer scope is closed; ownership is with Lead and no candidate writer may resume
without a new bounded assignment. No active build/probe remains.

Lead resolved two L1-INPUT-1 contract questions: Services now includes Domain's
existing UNKNOWN_PHRASE coverage code. Domain specifies a bounded amount-adjacent
currency-marker rule using bound-JDK ISO codes and Unicode currency symbols;
markers cannot silently become VND notes, and suffix+currency-label is invalid.
Later ordinary note words are not globally scanned as currency codes. No general
currency/language recognition or conversion is promised. Peer continues implementation
on these canonical clarifications without modifying shared docs.

Earlier input frontier (superseded by accepted local L1-STORE-1 below): input
interpretation lets synthetic callers obtain
an exact validated draft/clarification, not save/retrieve anything. Next ready local
wiring is real create/get/recent application services, owned transaction/head/replay/
inbox persistence and Flyway SQL, with a synthetic non-Telegram adapter. That code
can be developed without live credentials, but PostgreSQL acceptance cannot be
replaced by mocks or a smoke profile. Missing external input: host operator/Human
provides or identifies an authorized isolated synthetic PostgreSQL instance and
connection/execution scope. Return checkpoint: instance available and bounded
connection/migration checks authorized, then fresh/commit/reopen/replay evidence.
No database installation/socket-permission change is inferred. Actual Telegram
messaging remains separately unauthorized; no token/live test is needed for current
pure input work or local service wiring.

### Accepted inputs and continuation base

Accepted usable inputs: BOOTSTRAP-1 build/no-provider smoke and exact L1-INPUT-2
pure input/domain source above; reproduce with server README commands and existing
temporary Maven cache. Input/bootstrap write assignments are closed; the recovered
L1-STORE-1 disposition is below. Lead retains integration/shared-docs
authority; all candidate writers are now stopped.
Local service/store code is now accepted below, not blocked on future finance/LLM policies.
PostgreSQL execution/evidence still needs the isolated instance return checkpoint;
Git staging/commit still needs a usable writable repository from the host operator.
No push/deployment/live messaging or paid/model/data-egress action occurred.

Lead-owned changed paths in the prior continuation: `.gitignore`, `README.md`,
`docs/WORK_STATUS.md`, `docs/DECISIONS.md`, `docs/DELIVERY.md`, `docs/DOMAIN.md`,
`docs/SERVICES.md`, `docs/features/ledger/README.md`. Server changed paths and
immutable candidates are recorded above; no skill/private/session edits. No files
were moved or materially deleted. That continuation's documentation check passed: 19 Markdown
files, 176 local file links, 52 anchors, balanced fences. Diagram rendering and Git
ignore-engine behavior were not verified. One checksum probe used the wrong cwd
and failed; the corrected project-root pipefail-enabled probe reproduced the exact
accepted manifest. Failed probes are not used as acceptance evidence.

### Accepted local increment — recovered L1-STORE-1

Recovery 2026-10-21: current Lead is
`7e3bd3ce-639d-4211-8e54-216a7ca8ad79`. Predecessor Lead
`44fdb97a-a4c4-4716-ab7d-a57dbe22cc76` was terminated through Paseo before
affected writes. Existing Peer `06b08ca3-0e21-427f-bc02-7a0c9cc6203e` had no
active turn, and an explicit recovery prompt with finish notification failed on
the same usage limit. It was then terminated through Paseo; no Java/Maven/Gradle/
PostgreSQL background process was observed. This is lifecycle recovery, not a
claimed Peer submission. Server scope now transfers to Lead for reconstruction,
verification and disposition; no old callback authorizes renewed writes.

Recovered snapshot: 58 server files excluding target, sorted project-root
checksum-manifest SHA-256
`7233a964e531713c0a747b4a8d4e2957855664c9dbfb0325fa9cb2007b856305`.
Existing `target/l1-store-package.log` ends BUILD SUCCESS, 291 tests with no
failures/errors/skips. No stable Peer handoff was delivered; candidate inventory,
independent verification and acceptance were pending at recovery, resolved below. Accepted bootstrap/input
results are preserved. No database execution or Git mutation is authorized by
this recovery. All new Peer notifications must target the current Lead explicitly;
no predecessor callbacks or recurring wake-ups are assumed transferred.

Read-only Peer `39d52652-9094-4a76-8c8e-cef7d1a30b2c` inspected this exact
snapshot for authorization, validation, replay/atomicity and cursor defects;
no write ownership or build/DB execution. Selected configured demanding-review
Peer profile: `slp-peer` / `gpt-6-sol`, high thinking. Provider lacks read-only
mode, so explicit no-write scope uses auto-review. Completion notifications were
requested from creation to the current Lead. One schema-rejected create call
created no agent; corrected provider/model syntax created this Peer.

Lead independently ran `mvn -o -B -ntp
-Dmaven.repo.local=/tmp/longnest-bootstrap-m2 package` from `apps/server`: exit 0,
291 tests, no failures/errors/skips (169 input, 71 domain, 2 bootstrap, 49 ledger).
Existing classes were up to date; package reran tests and rebuilt the executable
JAR, not a clean compilation. Reports are in `target/surefire-reports/`; the earlier
writer's complete package log is preserved. JAR includes normal/ledger configuration
and migration, excludes test smoke YAML, launchers and Mockito resource. No DB
execution. Snapshot hash is unchanged and no Java/Maven background process remains.
Source inspection/disposition follows below; it is not implied by green tests
or the recovered writer's earlier build.

Final disposition: Lead ACCEPTS exact recovered L1-STORE-1 snapshot
`7233a964e531713c0a747b4a8d4e2957855664c9dbfb0325fa9cb2007b856305` for local
application/persistence code and synthetic unit/proxy behavior only. Lead inspected
all new service, mapper, cursor, entity/repository/adapter, SQL and test code.
Read-only Peer reproduced the same manifest before/after inspection, found no
concrete code-level defect in the bounded authorization/validation/replay/atomicity/
cursor paths, performed no writes/builds/DB checks and observed no background build.
Lead accepts that bounded finding; it does not prove absence of other defects.
The disposition was sent to the read-only Peer, which acknowledged closure and
no further writes/probes; current-session completion delivery was verified.
All candidate writers are stopped. Server scope remains with Lead; no active
implementation assignment or inherited callback may resume candidate writes.

Continuation reconciliation: Peer `91158e3c-fbec-44d6-a561-d46a4247fa67`, continued
from writer `06b08ca3-0e21-427f-bc02-7a0c9cc6203e`, returned a paused-write
DEPENDENCY_REQUEST for the pending handoff. Lead inspected its actual response
and idle/no-active-turn state: no files changed/tests rerun; reported manifest is
the exact accepted snapshot above. Lead ACCEPTS that report as corroborating
recovery evidence and resolves/closes the dependency request: no resumed write
assignment or additional handoff is necessary. Original assignment and continuation
handoff scope are closed; server ownership stays with Lead. Lead independently
reproduced the same hash and observed no Java/Maven/Gradle/PostgreSQL process.
A closure-only prompt with finish notification was sent to this continuation;
its completion notification reached current Lead, confirming explicit-prompt
delivery (not inherited callbacks). Peer acknowledged both scopes closed, no
active/background writers or processes started by this continuation, and no further
changes. No wake-up is needed for this closed assignment. Prior callback inheritance
is not claimed. No new build, probe assignment or candidate write.

Reconstructed changed-path inventory versus accepted input base (all relative to
`apps/server/`): modified `README.md`; 35 added files below. No deletion or move.

- `src/main/java/me/nghlong3004/longnest/ledger/`: `CreateIntent.java`,
  `HostContext.java`, `LedgerClient.java`, `LedgerConfiguration.java`,
  `LedgerException.java`, `LedgerQueryService.java`, `LedgerQueryServiceImpl.java`,
  `LedgerResult.java`, `LedgerStore.java`, `MutationReceipt.java`, `PersonalOwner.java`,
  `RecentCursors.java`, `TransactionInput.java`, `TransactionMapper.java`,
  `TransactionPage.java`, `TransactionService.java`, `TransactionServiceImpl.java`,
  `TransactionView.java`.
- Its `persistence/` package: `CreateInbox.java`, `EntryRepository.java`,
  `EntryValues.java`, `InboxId.java`, `InboxRepository.java`, `JpaLedgerStore.java`,
  `LedgerEntry.java`, `OwnerHead.java`, `OwnerHeadRepository.java`.
- `src/main/resources/application-ledger.yaml` and
  `src/main/resources/db/ledger/V1__create_ledger.sql`.
- `src/test/java/me/nghlong3004/longnest/ledger/`: `LedgerConfigurationTests.java`,
  `LedgerProxyTests.java`, `LedgerUnitTests.java`, `RecentCursorsTests.java`,
  `persistence/JpaLedgerStoreUnitTests.java`.
- `src/test/resources/mockito-extensions/org.mockito.plugins.MockMaker`.

Inventory is reconstructed from the accepted-base path inventory, present files
and predecessor activity, not a Git diff or a delivered writer handoff. No source
repair was needed during recovery. The old mock-maker initialization failure is
retained in `target/l1-store-mockito-before.txt`; it is resolved by the test-only
subclass mock-maker resource, without dependency/host changes. Nonfatal Mockito
self-attachment and inherited toolchain warnings remain; they do not count as
DB evidence. The recovered 291-test writer log and independent 291-test rerun agree.

Usable downstream input: callable create/get/recent services, mapper/local caller,
JPA adapter and opt-in PostgreSQL schema source; see server README to reproduce
local checks. No installed/provisioned/connected database or authenticated transport
is supplied. Next frontier is isolated PostgreSQL migration + Hibernate validation,
explicit fixture head initialization, actual lock/rollback/concurrent operation/
same-ID reconciliation/reopen/keyset/restore evidence. Owner of missing input:
host operator/Human supplies an isolated synthetic PostgreSQL instance and bounded
connection/migration execution scope. Return event: those are available; Lead then
assigns the database verification increment. No implicit installation, socket or
permission change, real messaging, LLM call, deployment, push or live-data egress.
L2 depends on accepted durable L1 behavior, which is not established by this local
code acceptance. Git work separately awaits a usable writable repository.

Recovery changed only `README.md`, `docs/SERVICES.md`, `docs/BACKEND_RULES.md`,
`docs/features/ledger/README.md` and this existing status source, aligning current
acceptance and opt-in schema location. Server candidate/private/.agents/.git files
were not changed. Final documentation check passed 19 Markdown files, 178 local
links, 52 anchors and balanced fences. Exact server hash remains unchanged;
no Java/Maven/Gradle/PostgreSQL process was observed after verification.

Original brief (closed by recovery/disposition above): Peer
`06b08ca3-0e21-427f-bc02-7a0c9cc6203e`, `apps/server/**` for real
create/get/recent application and JPA/Flyway wiring/tests/docs. Base: accepted
L1-INPUT-2 `725de47f11e11d226400d5bbb07bfccb4957c6041a0a362fb035c81e18d4190c`.
Accepted parser/value/bootstrap behavior is frozen except a concrete integration
repair approved by Lead; no POM/dependency/root/shared/private changes. Lead retains
shared docs/status/acceptance and does not write the server scope.
Outcome: callable real create/get/recent service interfaces/implementations and
DTO mapper, actual JPA records/repositories/adapter and PostgreSQL Flyway SQL;
owned head-lock/replay/inbox/receipt atomicity and bounded recent cursor semantics
follow Services. No in-memory pretend ledger, HTTP/Telegram/LLM or future L2/L3.
Missing head must fail; no automatic seeding/recreation. Integration activation
must be explicit and require configured personal identity; accepted no-DB smoke
stays isolated. A minimal explicit ledger profile is permitted for unfinished
runtime wiring; it is not a substitute for real PostgreSQL behavior or a live grant.
Offline compile/package and synthetic value/contract/security/unit checks are ready;
mocked persistence cannot prove commits/row locks/replay durability. Do not execute
migrations or connect a database. Return exact candidate/base/path inventory,
local evidence and explicit unrun DB criteria; stop writers at submission. Reopen
missing technical bindings/shared contracts with concrete evidence. After local
code verification, the runtime return checkpoint remains the host-provided,
authorized isolated PostgreSQL instance—not another code or product approval gate.

## Historical PG-RULES-1 documentation snapshot

The following records the prior stable handoff; current authority/state is above.

## Outcome and ownership

New bounded assignment: plan task-shaped project guidance under .agents without
bypassing read-only restrictions, and reconcile approved PostgreSQL across canonical
design/rules. This reopens the prior stopped documentation scope only. No application
code/scaffolding, implementation dispatch, installation, DB connection, migration,
infrastructure execution, credentials changes, inference calls or financial-data
transmission authorized.

Lead `90b5d356-e005-4c81-8073-8ca3fbd1ace1` owns PG-RULES-1 in resolved
`/home/nghlong3004/projects/longnest`. Current room confirms the same Lead and no
Peers/other project writers; no ownership transfer, background writer or dispatch.
Shared/local instructions, prior accepted handoff/status and affected docs read.
No project workspace protocol exists. English docs/communication; Vietnamese domain
examples preserved. Skill-creator read in full and used for the proposed minimal
routing; Ponytail full avoids extra files, toolchains and speculative mechanisms.

## Installation blocked, not substituted

Affected path: `/home/nghlong3004/projects/longnest/.agents`.
Managed policy explicitly read-only; directory stat observed mode **0555**.
Read-only directory metadata/names inspection only: .agents is empty, no private contents loaded.
No write probe, chmod, symlink/rename workaround, escalation or global modification.

Desired layout is in [Backend rules](BACKEND_RULES.md#project-local-routing-proposal--blocked):
.agents/AGENTS.md plus one longnest-backend SKILL.md and four references for Java/
services/mapping/Lombok; JPA/Flyway/concurrency; DTO/API/validation/pagination; config/
runtime/virtual threads. Proposed description routes backend tasks accurately; each
selected branch is read fully, canonical product/domain/service docs remain linked.
**None is created, installed, registered or validated.** Root AGENTS is now a small
routing entrypoint. Existing Backend rules/README guidance is retained temporarily,
not claimed as fulfillment of the required .agents location or a second installation.

Blocker owner: host operator. Required change: grant managed-policy write/create
access and filesystem write/create permissions for .agents/AGENTS.md and
.agents/skills/longnest-backend/ and necessary parents, keeping unrelated/private
paths protected. Return event: host confirms effective permissions, then a new
bounded installation/migration assignment. Stop at this handoff meanwhile.

## Decided versus proposed

[Decisions](DECISIONS.md) D20 approves **PostgreSQL** as the application database,
replacing H2 in development, integration tests and eventual operation. No H2 default/
compatibility-mode substitute. D21 requests .agents guidance but records its blocker.
All 12 D19 backend standards remain intact in [Backend rules](BACKEND_RULES.md).
JPA/Hibernate + Flyway SQL, service interfaces/implementations, DTO mappers/records/
validation, documented future HTTP, shared pagination, appropriate Lombok, injected
beans and Java 25 virtual threads are required designs, not installed artifacts.

[Services](SERVICES.md#persistence-and-query-consistency) now owns PostgreSQL binding:
one bot instance plus DB-server dependency; expected pgJDBC and separate Flyway
PostgreSQL module; versioned PostgreSQL SQL, explicit type/exact aggregate mapping;
separate @Version/business revision; short owner-head locks and READ_COMMITTED
coherent reads; atomic financial/action/inbox/result persistence; synchronous WAL
commit; same-operation reconciliation after connection-loss uncertainty.
DATE/instant precision and saved-value keyset ordering are explicit. Public cursor/
filter/generation/10-entry behavior and host-only real-user Save/delete proof stay
unchanged. No locks held across human/network waits.

PostgreSQL 18 with current minor at binding is a **supported-version proposal**,
not selected/pinned/provisioned. Actual PostgreSQL version, host, credentials and
Java 25/Boot/Hibernate/pgJDBC/Flyway compatibility remain untested. MapStruct and later
springdoc remain dependency proposals, no installs. Logical dump/isolated restore is
a recovery recommendation, not executed backup, retention or paid-service approval.

Approved product stays personal user/VND/Asia/Ho_Chi_Minh, authoritative ledger,
clear capture immediate, ambiguity no write, stable-ID full CRUD/recent/get,
immutable Edit preview → real Save/Cancel, confirmed reversible Delete/Restore,
editable financial date, inclusive totals/type views and selectable pages. Event/
action replay returns one outcome; distinct equal messages remain separate purchases.
[Domain](DOMAIN.md), [C4](ARCHITECTURE.md) and [ledger acceptance](features/ledger/ACCEPTANCE.md)
agree; no new first-release product decision or semantic suppression.

Future classification/daily budget and advisory salary branches remain central
proposed future slices, not prerequisites. LLM/web/Excel/export stay later; DeepSeek
is a preferred evaluation candidate only. Shared deterministic money/range/budget/
allocation rules, auth, idempotency and user confirmation cannot be model bypasses.
No new egress/provider/hosting/retention/purge approval.

## Stable candidate and base limits

Candidate: **PG-RULES-1** (permitted docs/routing plan, installation blocked).
Base: accepted BACKEND-RULES-1, manifest SHA-256 (17 artifacts excluding status)
`ef5b60433a531c18c2fd631d642e51a249434c9de76f44c7fbd4d41acc00549c`;
base status SHA-256
`2b38fbd4a09da5b2c921642733710721c2862efd9861a7388ba89f28f18633ed`.
Prior SDLC-LITE-FINAL-1 product/slice/research content is retained except explicit
supersession of H2/embedded assumptions. No usable Git commit existed in the prior
restricted placeholder; Git metadata not read/changed this turn. Snapshot covers
listed public Markdown, not private/unrelated workspace state.

Complete changed paths:

- `AGENTS.md`
- `README.md`
- `docs/BACKEND_RULES.md`
- `docs/SERVICES.md`
- `docs/DOMAIN.md`
- `docs/ARCHITECTURE.md`
- `docs/DECISIONS.md`
- `docs/DELIVERY.md`
- `docs/features/ledger/README.md`
- `docs/features/ledger/ACCEPTANCE.md`
- `docs/WORK_STATUS.md`

No file creation/move/deletion; .agents untouched. Other seven public documents,
private env/state and global config preserved. No apps/deploy/.env.example scaffold.

## Checks and disposition

Official PostgreSQL versioning, pgJDBC, Flyway PostgreSQL module, transaction/
row-lock/WAL/type/aggregate and pg_dump references browsed read-only. Exact source
links are in Backend rules, Services and Delivery; facts establish documented
semantics, not a tested dependency combination, functioning DB or runtime evidence.
Existing Java/Spring references remain qualified by the previous read-only review.

Documentation checks passed: 18 Markdown files, 172 exact-case local links,
52 anchors, 26 consistent-width tables and balanced fences; all 12 required-rule
labels retained, with shared pagination first and four proposed task branches.
One lowercase status, no apps/deploy/env scaffolds or .agents entries. C4 source
views retain 3/5/10 nodes and 4/5/18 resolved edges/balanced boundaries. Eight future
tools unchanged; actual Save/Cancel/Delete confirmation remains host-only. Reviewed
remaining H2/embedded wording: only explicit supersession/exclusion, no active route.
Changed-file hashes match the 11-path inventory; seven other existing public docs
are unchanged. No new/moved/deleted files. Existing domain/finance examples and
arithmetic are unchanged; no product behavior tests executed.
No remaining structural failure. Mermaid views remain unrendered (prior renderer
lookup absent), no skill quick-validation/discovery because installation is blocked,
no compilation/ORM/schema/migration/PostgreSQL/concurrency/restore/runtime checks.
Candidate manifest SHA-256 (17 artifacts excluding this status):
`98038e44fc3258d5164c380b25058169569cf4f90af5ec73b42d4aad51799643`.
Full status hash is returned separately to avoid self-reference. Reproduce candidate
hash: SHA-256 each README.md, AGENTS.md and docs Markdown except WORK_STATUS.md;
sort relative paths, emit `hash  relative-path` plus newline, SHA-256 the manifest.

**Lead ACCEPTS PG-RULES-1 for PostgreSQL design reconciliation and permission-aware
routing proposal only. .agents installation remains BLOCKED/unfulfilled.**
This is not acceptance of the entire desired installation, a working bot or tested
PostgreSQL behavior. No permission bypass, application code/scaffold, installs,
implementation dispatch, DB connections/migrations, credentials changes or external
financial actions occurred. Stop writes at stable handoff until reassigned.

## Next frontier and remaining choices

Host permission change plus bounded reassignment enables actual guidance migration
and skill-creator validation. Independently, later explicit implementation authority
can enable L1 capture/recent/get → L2 corrections/Delete/Restore → L3 range views;
all remain unassigned, with no accepted implementation inputs. PostgreSQL-backed
integration/recovery requires an authorized isolated instance, not live finance data.

Lead then binds compatible DB/driver/provider/Flyway/transport/toolchain versions
and collects actual migration, concurrency, pagination, commit/reconnect and restore
evidence on PostgreSQL (not H2). No database version choice is a new product gate.

Human choices return only at the affected phase: live application/DB host, exposure,
identity/credentials/TLS custody; backup destination/cadence/retention/recovery/purge;
future classification/daily policy; advisory salary target/residual/infeasibility;
future LLM/provider/privacy/cost/egress. Recommendations and decision owners remain
in [Decisions](DECISIONS.md) / [Delivery](DELIVERY.md) / [slice map](features/README.md).
No future branch blocks approved ledger behavior or authorizes live actions.
