# Decisions and proposals

Decisions describe authorized product direction and selected Lead design, not
implementation facts. Future policy and activation choices remain explicit proposals. Human owns product/cost/
privacy/external-risk choices; Lead owns technical design within granted scope.
[Status](WORK_STATUS.md) owns current assignments, candidates and evidence.

## Settled direction

| ID | Decision | Consequence |
| --- | --- | --- |
| D1 | Telegram is the current user channel; bot ledger is authoritative | No second ledger; Telegram is an adapter to application capabilities. |
| D2 | Excel/templates/export deferred; no round-trip editing | No workbook or export dependency in release one. |
| D3 | Common Vietnamese natural-language/shorthand direction; deterministic first release | Finite grammar now; future LLM may widen interpretation, not bypass rules. Vocabulary/sign mapping is selected by Lead in Domain. |
| D4 (historical; superseded by D22) | Earlier assignments were documentation/design only | Prior design acceptance remains documentation evidence, not runtime acceptance. |
| D5 | Lowercase docs/, apps/server/, apps/client/, deploy/ and requested environment/context layout | Document runtime areas without placeholders. Private environment files remain untouched. |
| D6 | Salary allocation advisory only | No moving funds, bank integration, paid services or external financial actions. |
| D7 | English documentation, identifiers and Lead/Peer communication; Vietnamese end-user presentation | Externalized Vietnamese UI/resource values are selected user content, not a translation of protocol commands or machine contracts. |
| D8 | Asia/Ho_Chi_Minh business timezone, Vietnam UTC+7 | Never derive business date from host timezone. |
| D9 | Income/expense transaction CRUD accessible through bot | Stable IDs, recent selection, Edit/Delete and reusable create/read/update/delete capabilities. |
| D10 | Date-range overview and separate type views require x <= y | Invalid/reversed ranges do not silently query another period. |
| D11 | Single personal user; VND; exact money validation; per-operation authorization, retry-safe mutations and recoverable storage | Currency/audience are no longer questions. Correctness and recovery are required, but host/retention/cost are unselected. |
| D12 | Clear validated input saves immediately; ambiguous input requests clarification | No blanket creation-preview gate; unresolved ambiguous mutations never execute. This is shared application policy, not a parser-specific financial rule. |
| D13 | Confirmed reversible deletion and restore capability | User confirmation binds the displayed action/target. Deleted entries leave normal reads/totals; restore returns their contribution once. Purge/recovery duration is unselected, not approval of permanent deletion. |
| D14 | Editable financial date separate from immutable creation time | Explicit date correction/backdated-entry workflow is in the design. Today's rules do not gain relative-date parsing. |
| D15 | Inclusive ranges; overview totals; type totals plus paginated selectable lists, page size 10 | Summary versus individual list remain distinct capabilities, but presentation is no longer undecided. |
| D16 | Object-oriented Spring Boot/Java 25 application; Telegram AND callable application services | Cohesive domain values/entities and use cases; future tools reuse them. No separate agent platform or business logic confined to chat handlers. |
| D17 (extended) | Rules-first DeepSeek fallback with typed explicit-date read routing selected for bounded implementation | Reuse Spring AI 2.0.1; default off, capture and LIST_RANGE/CLARIFY subsets. Query evidence is checked locally; authoritative results are rendered directly, not sent to the model. No replacement, native multi-call loop or generic tool dispatcher. Same services/auth/replay and real-user action proofs; no live provider calls, credential use, spend or financial egress approved. Exact scope/evidence in CURRENT STATUS. |
| D18 | Daily allowance and advisory salary allocation are central to the full product vision, developed as future bounded slices | They are not discarded roadmap ideas and are not automatically promoted into the current CRUD release. Their percentages/eligibility/rollover/proration/priority policies remain proposals. |
| D19 | Required Java/Spring backend conventions are canonical in Backend rules | Google style; mapper-based DTOs; service interfaces/Javadoc/stereotypes; JPA/Hibernate; record/validation/OpenAPI DTOs; Flyway SQL; documented future controllers; shared paging; appropriate Lombok; injected beans; Java 25 virtual threads. These are documentation policies, not installed libraries or HTTP permission. |
| D20 | PostgreSQL is the application database, replacing H2 in all application environments | JPA/Hibernate and Flyway stay required; no H2 dev/test default. Version/host/credentials are not selected; no installation, connection or migration permission. |
| D21 | Project guidance belongs under .agents with a small root routing entrypoint | A manually supplied read-only backend skill now routes to Backend rules. Fuller relocation remains blocked by managed policy; proposed four-reference layout is not installed. |
| D22 (extended) | Bounded remaining first-release L1–L3 code and synthetic/local build/smoke checks are authorized | Exact scopes/owners/candidates are in CURRENT STATUS. Local L2/L3 code may advance on inspected stable contracts without claiming durable L1/runtime acceptance. Inspected Git stage/commit/push only with usable permitted metadata and verified identity/destination/content. No live messaging/finance egress, DB/container provisioning/connection/migrations, paid/LLM calls, deployment, private-state access or protected-path workaround. |

The former P1 user-count, P2 currency, P4 create confirmation, P10 delete semantics,
P11 range/list/page-size and P12 editable-date proposals are resolved by D11–D15.
Do not reopen those approvals. No backup/retention schedule was approved:
recoverable storage does not approve a schedule, retention or purge window.

## Lead design choices for this documentation

- L1: Keep [docs/WORK_STATUS.md](WORK_STATUS.md) as the sole status source. Status
  never moved; capitalized draft paths were reconciled earlier without losing work.
- L2: [README](../README.md) is the context index; [AGENTS](../AGENTS.md) is a small
  mandatory routing entrypoint. Requested .agents guidance relocation is blocked;
  retained existing docs are not claimed as fulfilling the requested installation.
- L3: Retain Context/Container plus focused Component C4 source views. No diagram
  toolchain, code-level/deployment view or claimed infrastructure.
- L4: [Services](SERVICES.md) owns the OOP responsibility and future-tool matrix;
  [Domain](DOMAIN.md) owns input/money/date semantics. Two selected cohesive
  application services suffice; no generic handler/class hierarchy or tool framework.
- L5: Move ledger acceptance and roadmap into the [feature tree](features/README.md),
  preserving R1–R17 and moving future R18–R19 to adapters. Shared Domain/Services/C4
  stay canonical at docs root; WORK_STATUS stays the sole status source. Feature
  folders and provisional scopes are not Peer assignments or accepted inputs.

## Selected technical baseline

These Lead decisions close prior P2/P3/P4/P5/P8/P12 design gaps; they are not
Human approval of infrastructure, retention or future feature policy. Exact values/
payloads live in their canonical documents, not duplicated specifications here.

| ID | Selected design and reason | Canonical contract |
| --- | --- | --- |
| T1 | Finite Vietnamese/sign grammar, exact whole VND 1–10^12, bounded input/note, fixed compatible categories. Predictable capture instead of guessing unsupported language. | [Domain](DOMAIN.md#selected-first-release-grammar) |
| T2 / PE1 | Edit → immutable preview → actual user's Save → commit stored after-state; Cancel/expiry/stale target makes no change. Update tool cannot mint proof or bypass Save; creation remains immediate. | [Action lifecycle](SERVICES.md#action-lifecycle-and-replay) |
| T3 / PE2 | Event/action identity replays one outcome; distinct valid messages remain distinct purchases. No semantic duplicate prompt. This preserves fast capture and two identical real purchases. | [Replay](SERVICES.md#action-lifecycle-and-replay) |
| T4 (revised by D20) | Spring Data JPA/Hibernate over approved PostgreSQL and Flyway-owned SQL schema; prior H2 route superseded. One bot instance plus DB-server dependency; atomic record/action/receipt/inbox and synchronous commit retained. pgJDBC/Flyway-module versions and combined stack unverified. | [Persistence](SERVICES.md#persistence-and-query-consistency) |
| T5 | Version-1 tagged input/result/error shapes, positive stable IDs/revisions, exact string-encoded numbers at JSON boundaries; distinguish rollback from outcome unknown. Prevent silent overflow, stale writes and duplicate recovery. | [Payloads](SERVICES.md#typed-inputs-and-results) |
| T6 | Ten-minute actions and bounded ephemeral cursors, deterministic keyset ordering, same-snapshot page+summary, generation refresh after changed writes. Small personal-bot navigation without a reporting/snapshot subsystem. | [Queries](SERVICES.md#persistence-and-query-consistency) |
| T7 | Telegram long polling and explicit selected-entry/field controls; no public webhook/API needed now. Identity/interaction checks remain mandatory; actual allowlist/token values are operational/private. | [C4](ARCHITECTURE.md), [controls](SERVICES.md#telegram--application--future-llm-capability-matrix) |
| T8 | Financial dates 1900-01-01–9999-12-31, explicit future dates permitted as labels, no scheduling; immutable creation/message times. Avoid an invented future-date prohibition and preserve backdating. | [Dates](DOMAIN.md#amount-and-date-rules) |
| T9 | Separate business revision from provider-managed @Version; short owner-head PESSIMISTIC_WRITE lock before atomic control/financial commands and multi-query reads. Simple per-owner consistency without assuming default isolation, holding human/network locks or pessimistically locking every entity. | [Persistence](SERVICES.md#persistence-and-query-consistency) |
| T10 | One transport-independent cursor contract; future HTTP preserves Telegram keyset/filter/generation/summary semantics, no public Page/Pageable. Avoid incompatible pagination per feature. | [Pagination](BACKEND_RULES.md#shared-pagination-first--requirement-9) |
| T11 | Validate Hibernate mappings against Flyway SQL, disable other schema initializers; service-owned proxied transactions and injected Boot configuration. Enable managed Java 25 virtual threads plus keep-alive; retain connection limits and thread-bound transactions. | [Backend rules](BACKEND_RULES.md) |
| T12 | Use MapStruct 1.6.3 with the existing version property and Lombok binding for Spring-managed representation mapping; keep validation, exact financial conversions and replay construction explicit. Add the missing API dependency, not duplicate processor configuration. Candidate compatibility/injection evidence belongs in CURRENT STATUS. | [Mapping rules](BACKEND_RULES.md#style-mappers-and-services--requirements-13-5) |

Superseded: direct-update alternative and unresolved update completion path; old
five-minute semantic-duplicate proposal; TBD engine/transport and limit/cursor
mechanics. A future semantic prompt needs Human approval because it would change
user-visible fast saving. It is not pending work needed for the first release.
Two services and one persistence adapter remain sufficient, not a class explosion.
The PostgreSQL assignment supersedes T4's H2 proposal and embedded/one-process
database assumption, not the public lifecycle/locking/paging contracts. Supported
PostgreSQL 18 is a version recommendation, not an actual selection or new product
gate. .agents relocation remains a host-permission dependency, not a different
approved location. T12 now selects MapStruct; D19 alone did not select it.
HTTP/OpenAPI exposure and further springdoc binding remain separately scoped;
existing POM artifacts are not permission to expose an endpoint. It does not reverse PE1/PE2, the
fast-create policy, public revision semantics or approved list UX.

## Open choices and proposals

Only finite product/operations choices remain. Return at the affected phase, not
as new development approval gates or reasons to reopen settled CRUD defaults.

| ID | Remaining choice / recommendation | Owner and return event |
| --- | --- | --- |
| P1 | Host, private allowlist values, secret/key custody and operational access. Recommend one private local process first; no public exposure by default. | Human before live access/deployment; not required to write isolated implementation/tests after authorization. |
| P6 | Backup destination/cadence/retention, recovery objectives and deleted-record recovery/purge duration. Recommend isolated restore rehearsal and no automatic purge before explicit policy. No retention duration approved. | Human before live operation/recovery policy; development can exercise synthetic isolated recovery. |
| P7/P14 | Classification/eligibility and daily policy. Recommend explicit independent labels, no category guessing, override-first everyday-variable spend, unknowns visibly included, no rollover/proration, same-day whole-day limit changes and no silent historic rewrite. | Human before C1/B1; exact package and worked examples in [classification](features/spending-classification/README.md) / [budget](features/daily-budget/README.md). Not a CRUD dependency. |
| P15 | Salary plan base/targets/residual/infeasibility; later percentage rounding. Recommend one selected received salary, user-owned fixed targets + Other residual, no automatic priority, reject/edit shortfalls; S2 rounds percentages down with explained residual. | Human before S1/S2; [salary package](features/salary-allocation/README.md). No invented personal percentages, fund movement or dependency on budget. |
| P13 | DeepSeek rules-first capture code is selected; live endpoint/model/processor, cost/privacy/egress and credential custody remain unresolved. Fake-response/local checks only; no LifeOS harness. | Human before paid/provider evaluation or external activation; [adapter](features/adapters/README.md) / [research](research/LIFEOS.md). Real-data use needs separate applicable-term clarification/approval. |

Environment precedence remains a documented [convention proposal](DELIVERY.md#environment-convention-proposal),
never a claim that Spring/Vite or a shell loads files now. Exact Boot/Hibernate/
Flyway/PostgreSQL/pgJDBC/build/Telegram transport versions, further mapper/OpenAPI
dependency selection, SQL migration syntax and Java package names
are implementation bindings for Lead after authority; verify official availability/
compatibility and reopen on evidence, not invent another product decision.
Refund/transfer/debt behavior, semantic duplicate prompting and expanded categories/
language are excluded expansions, not missing first-release completion paths.

## Reopen conditions

Reopen technical details on evidence of an incompatible dependency, unsafe input
interpretation or a needed contract change. Product expansion (multi-user, different
currency, permanent purge, broader financial operations or integrations) needs new
explicit direction. Do not turn hypothetical expansion into a first-release blocker,
silently substitute a stack or resurrect Excel requirements.
