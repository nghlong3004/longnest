# Backend rules — Java / Spring

Mandatory backend-context entrypoint for backend design and any later authorized
`apps/server/` work. Read local/shared instructions and [current status](WORK_STATUS.md)
first, then these rules and the relevant [slice](features/README.md). This document
specifies conventions, not installed dependencies, executable configuration or
implementation permission. [Services](SERVICES.md) owns contracts/transactions;
[Domain](DOMAIN.md) owns money/date rules; [C4](ARCHITECTURE.md) owns boundaries.
The required relocation into .agents is **blocked**, not installed; this existing
document is retained pending permission, not offered as fulfillment of that location.
See the [routing proposal](#project-local-routing-proposal--blocked) below.

## Shared pagination first — requirement 9

Use the single [TransactionPage contract](SERVICES.md#typed-inputs-and-results)
and [cursor lifecycle](SERVICES.md#persistence-and-query-consistency) for every
feature: page size 10, opaque cursor, bound owner/scope/filter/range/generation,
keyset order and same-snapshot full-range summary. Never confuse summary with
page subtotal. Recent and range ordering deliberately differ as specified there.
Expired/invalid/stale cursors refresh page one; Telegram owns Previous history.

Selected technical convention: future HTTP uses the same cursor/filter/result
semantics, not independent offset/page-number or arbitrary-sort contracts.
Spring Data `Page`, `Slice` and `Pageable` stay internal; public request/response
records expose only the canonical values. No HTTP endpoint is authorized now.
Any future feature needing a different contract must explicitly revise the shared
contract, not quietly introduce its own paging standard.

## Style, mappers and services — requirements 1–3, 5

- Follow [Google Java Style](https://google.github.io/styleguide/javaguide.html),
  including formatting; a formatter/version may be bound during implementation,
  not installed by this document.
- Import referenced Java types and use their short names in handwritten code,
  rather than inline fully qualified names. Retain a fully qualified name only
  for an actual simple-name collision and explain that exception in the handoff.
  Do not hand-edit generated sources or reformat unrelated files for this rule.
- End-user UI uses UTF-8 Spring message bundles and injected MessageSource through
  the independent UiMessages accessor. Vietnamese is fixed for this personal bot;
  no runtime language selector. Keep text/labels separate from financial policy,
  validation and machine enums/codes/commands. Pass exact amount/date/ID strings
  to placeholders, without locale arithmetic/rounding. Telegram currently sends
  plain text with no parse mode; do not introduce markup interpretation of notes.
  Java bundles/keys can support later server adapters, but are not automatically
  browser-consumable assets. Project docs/identifiers/coordination remain English.
- Services do not manually instantiate request/response DTOs by default. Define
  corresponding cohesive mappers (for example TransactionMapper); route DTO
  construction there, including builders. Domain construction/value factories
  are still normal service/domain work. Mappers translate representations, not
  authorize, invent defaults or decide financial policy. A genuinely complex
  manual service mapping exception needs a nearby reason and coverage, not a
  blanket exemption. Patch mapping must preserve absent versus present fields.
- **Selected mapping library:** MapStruct 1.6.3, using the POM's existing
  `mapstruct.version` for its API dependency and processor, with Spring component
  model, constructor injection and unmapped-target errors. Retain the configured
  Lombok/MapStruct binding in compile/test processor paths without duplicates.
  `TransactionMapper` keeps explicit validation, money/date defaults, fingerprints
  and original-receipt reconstruction; its injected `TransactionRepresentationMapper`
  generates input/view/page/receipt construction. Exact canonical amount helpers
  and Optional/state conversions are explicit; generated copying cannot authorize,
  validate a financial action or invent defaults. Both beans require the `ledger`
  profile, including the annotation on the generated implementation. Verify
  processing, Spring injection and behavior on the actual candidate; selection
  alone is not compatibility or runtime evidence.
- Every application service has an interface and implementing class. Keep
  TransactionService / TransactionServiceImpl and LedgerQueryService /
  LedgerQueryServiceImpl cohesive; future finance services follow their slice,
  not one service per method. Each public interface method needs brief Javadoc:
  purpose, every `@param`, `@return` unless void, validation/authorization/state/
  replay/confirmation effects and relevant returned errors or thrown exceptions.
- Declare `@Service` on service implementations. Declare `@Repository` on Spring
  Data repository interfaces and custom persistence adapters as appropriate.
  Spring Data supplies repository proxies: do not handwrite redundant repository
  implementations solely to attach an annotation. Keep transport logic outside
  services and Spring Data types outside public application contracts.

## Feature package organization

Use feature-first packages with small responsibility-based subpackages, not a
global layer tree or a new Clean/Hexagonal framework. The current ledger has real
service and persistence boundaries already; organizing those classes is sufficient.
Under `me.nghlong3004.longnest.ledger`:

| Package | Existing responsibility |
| --- | --- |
| `model` | Internal validated intent, trusted host context, owner policy and business exception. |
| `model.entity` | JPA entities and embeddables; never returned from application services. |
| `dto` | Input/view/page/receipt records and tagged caller result, not persistence state. |
| `service` | Application service interfaces. |
| `service.impl` | Transactional service implementations. |
| `repository` | LedgerStore port, Spring Data repositories and JPA adapter. |
| `mapper` | TransactionMapper representation/validation boundary. |
| `pagination` | Shared bounded in-memory LedgerCursors helper with recent/range keys and bindings. |
| `config` / `adapter` | Explicit-profile bean configuration / non-network LedgerClient caller. |

Keep existing reusable `domain` values and deterministic `input` packages unchanged;
do not relocate them merely to fill this tree. Tests follow the responsibility they
exercise; cross-boundary service tests may use explicit imports. Empty packages,
new interfaces, generic base classes, compatibility facades and dependencies are
not required. This is code organization, not a newly published Java API.

Keep fields private and JPA no-arg constructors protected. Where the split requires
cross-package access, expose only existing guarded constructors/operations or
immutable snapshots; do not add setters or relax invariant checks. Preserve
annotations, bean discovery, transaction/replay/auth behavior, JPQL entity names,
SQL and opt-in smoke isolation. Package moves alone do not prove database behavior.

## Persistence and schema ownership — requirements 4, 7

Spring Data JPA/Hibernate replaces the former direct-JDBC repository design.
PostgreSQL is the approved application database for development, integration tests
and eventual operation; no H2 default/compatibility-mode substitute. Provider
pgJDBC usage underneath JPA is expected, not raw-JDBC primary repositories.
[Services](SERVICES.md#persistence-and-query-consistency) selects application
transaction ownership, atomic inbox/action/result persistence, lock order and
snapshot behavior. In particular:

- Separate provider-managed `@Version` rowVersion from business revision (starts
  at 1). Check expected business revision; optimistic locking catches stale
  persistence writes. Never manually increment provider versions. No-op business
  operations must not dirty the transaction entity or advance its revision.
- Use a short `PESSIMISTIC_WRITE` owner-head lock for the multi-record atomic
  mutation/control boundary and consistent page+summary reads, not a lock on
  every entity/query by habit. Release before Telegram/provider calls or human
  waits. Locks do not replace owner checks, unique operation constraints or
  optimistic revision checks. Timeouts/deadlocks roll back; no blind fresh-ID retry.
- Use Spring-proxied `@Transactional` service implementation entrypoints, with
  rollback on mutation failures. Self-invocation does not create a new boundary.
  Do not split action consumption, financial changes, generation and receipt into
  separate commits/REQUIRES_NEW calls. Translate failures after rollback/commit
  outcome is known; caught persistence errors must not yield an apparent success.
- Flyway **versioned SQL** owns tables, primary/foreign keys, indexes, uniqueness,
  checks and sequences. Conventional future default location:
  `apps/server/src/main/resources/db/migration/V<version>__<description>.sql`.
  The bounded L1 store uses `db/ledger/V1__create_ledger.sql`, selected only by
  the explicit `ledger` profile while runtime wiring is unfinished; see CURRENT
  STATUS for acceptance limits. This does not authorize migration execution.
  Never edit an applied permanent migration; add the next version. Migration-file
  implementation is limited to named scopes in CURRENT STATUS; execution needs
  its own authorized isolated database scope.
- Required JPA `@Id`, `@Column`, `@JoinColumn`, relationships, generation/sequence
  and `@Version` mappings mirror that schema; they are not banned. Do not rely on
  `@Table(indexes=...)`, mapping uniqueness or Hibernate to create schema objects.
  Selected deployment convention: `spring.jpa.hibernate.ddl-auto=validate`, no
  update/create/create-drop; `spring.sql.init.mode=never`, no competing schema.sql/
  data.sql/import.sql bootstrap. Flyway is the sole schema-change mechanism;
  actual migration/startup execution still requires the relevant authorization.

## DTOs and future HTTP — requirements 6, 8

Explicit Request/Response DTOs are Java records, not JPA entities. Use Jakarta
Validation and Swagger/OpenAPI `@Schema` descriptions/examples on types and each
record property. Document input/output distinctions; internal host-only context,
actor/proof and persistence state must not become public writable DTO fields.

Apply suitable constraints: required references need non-null constraints,
required text nonblank, canonical decimal-string syntax plus exact numeric bounds,
valid enums, ISO dates plus Domain bounds and cross-field range checks. `@Valid`
cascades nested validation; it does not itself mean required or validate a scalar.
Java string `@Size` alone does not enforce the Domain's Unicode-code-point limit.
Do not add `@Past` to financial dates: approved explicit future dates are labels.
Validate complete patch after-state and absent/null semantics in shared services,
not only MVC. Telegram and future tools have the same checks.

When HTTP is separately authorized, each feature endpoint documents `@Operation`
summary/description, `@ApiResponses` actual success/error statuses, `@Parameter`
descriptions for path/query parameters and schemas for payload properties. Declare
explicit successful `@ResponseStatus` consistent with its real response; bodyless
success may use 204, but a mutation receipt needs a body-bearing status. Document
no-body endpoints as such, without fabricated payloads. Use `@Valid` on structured
payloads; use actual scalar constraints and conversion/cross-field validation for
path/query values, not meaningless `@Valid` on every parameter/no-arg method.

Spring MVC's built-in method validation handles direct parameter constraints;
do not blanket-add controller class `@Validated` when selecting that mechanism
(it instead selects AOP validation). Handle both body and method validation failures
and map them to canonical business errors without leaking persistence details.
Swagger annotations describe an API; they do not implement checks/status handling.
An OpenAPI generator such as springdoc is a future dependency proposal, not an
approved HTTP/MCP/tool SDK or authorization to expose endpoints.

## Lombok and shared beans — requirements 10, 11

Use appropriate Lombok rather than handwritten boilerplate: `@RequiredArgsConstructor`
for injected final dependencies, `@Getter`, narrowly justified `@Setter` / `@Builder`
where safe. Records need no redundant Lombok. Preserve domain factory/invariant
checks: no blanket `@Data`, public setters or builder bypass on entities/aggregates.
JPA entities are nonfinal classes with a public/protected no-arg constructor
(`@NoArgsConstructor(access = PROTECTED)` is suitable), not records. Field access
does not require public setters. Choose entity equality deliberately; generated
toString/equality must not traverse lazy relationships or expose private data.
Audit model types by behavior, including domain/input values, DTOs, nested state
and configuration models, not only entity folders. For assignment-heavy entity
construction, a private Lombok builder/private generated all-args constructor
behind a guarded factory can remove raw-constructor maintenance without exposing
IDs, versions or state. A builder on a validated constructor/factory is another
option. Preserve JPA protected no-args, required defaults/conversions/defensive
copies and controlled transitions; do not replace these with a public all-field
builder. Records remain records without redundant annotations. Retained manual
construction/accessors need concrete behavioral reasons, not a blanket invariant
exception; use narrow generated getters where access and semantics are identical.

Inject shared configured infrastructure: JSON mapper, clock, data source, executor
and clients. Prefer Boot auto-configured beans/customizers; add configuration
`@Bean` only for an actual missing/custom need, not duplicate overriding beans.
Boot 4's default Jackson integration configures a Jackson 3 JsonMapper: do not
repeatedly `new ObjectMapper()` or introduce legacy Jackson 2 configuration by
copying older examples. The selected TelegramBots longpolling/client 10.3.0
transport requires its own Jackson 2 mapper at the SDK boundary; construct that
only in its opt-in bean factory, never as a replacement for Boot's Jackson 3 bean.
The SDK owns polling/API calls; application code retains authentication, replay,
managed handling/acknowledgment and resource shutdown. No Boot-3-oriented Telegram
starter or parallel raw-HTTP fallback is selected. Construction inside bean
factories and ordinary value/domain allocation remain valid. Exact dependency/
processor bindings and verified compatibility limits are in the POM/CURRENT STATUS.

## Virtual-thread blocking workload — requirement 12

Selected future activation on Java 25: `spring.threads.virtual.enabled=true` and
`spring.main.keep-alive=true`. Route blocking backend work through Boot-managed
virtual-thread execution; the setting does not convert arbitrary Telegram-library
threads or self-created executors. Inspect the selected transport binding later.
Virtual threads are not a reason to adopt reactive code or pool virtual threads.

Connection pools still bound concurrent DB work; bound independent remote workload/
queues to avoid unlimited waiting tasks and latency amplification. No redundant
DB semaphore is required merely because virtual threads exist. Never hold a DB
transaction/lock while waiting for a user, LLM or Telegram network response.
Thread-bound transactions do not propagate to newly started threads: perform
each transaction on one execution thread, without passing managed entities or
EntityManager across tasks. Pass trusted ApplicationContext explicitly; where
needed use an explicit task decorator for logging/security context, not accidental
ThreadLocal inheritance or copied transaction resources. Redact logs as in Delivery.

Java 24+ removed synchronized-block/method carrier pinning; do not impose an old
Java 21 blanket ban on synchronized. Native blocking/actual contention still
needs evidence. Virtual threads do not accelerate CPU-bound arithmetic or remove
locks/transactions. Boot's virtual-thread scheduler/executor ignores its thread-pool
sizing properties, not JDBC pool limits. Its daemon threads do not keep the JVM
alive, hence keep-alive above. SimpleAsyncTaskScheduler fixed-delay tasks share a
scheduler thread; if scheduling is later needed, choose/review semantics explicitly
(fixed-rate/cron or another suitable scheduler), not a hidden budget timer now.

## Optional additions, not new universal gates

1. Apply existing [transaction/error contracts](SERVICES.md#persistence-and-query-consistency)
   and [money/time invariants](DOMAIN.md#amount-and-date-rules) in each bounded slice;
   add no parallel exception or rounding policy.
2. Make existing [credential/log redaction and recovery](DELIVERY.md) concrete in
   the later transport/storage binding; no secrets or raw finance logs in fixtures.
3. When implementation is authorized, collect migration/validation, rollback,
   concurrency, replay and synthetic restore evidence using the selected provider,
   not only repository mocks. This refines Delivery's evidence, not extra approval
   for deployment or new product features.

## Project-local routing proposal — BLOCKED

Desired guidance location, **not an installed tree**:

```text
.agents/
├── AGENTS.md                         Small project-context/scoped routing rules
└── skills/longnest-backend/
    ├── SKILL.md                      Backend trigger + select relevant branch
    └── references/
        ├── java-services-mapping.md   Requirements 1, 2, 3, 5, 10
        ├── persistence.md            Requirements 4, 7; locks/transactions
        ├── dto-api-pagination.md     Requirements 6, 8, 9; paging first
        └── runtime.md                Requirements 11, 12
```

Proposed skill name: `longnest-backend`. Description: “Design, implement or review
Longnest Java/Spring backend services, mappings, persistence, DTO/API contracts or
runtime configuration; not frontend work or finance-policy discovery.” This is a
proposed trigger, not registered frontmatter. SKILL.md would link all four branches
with task conditions; read the selected branch **in full**, multiple branches only
when the task crosses their boundaries. Begin contract work with shared pagination
before feature DTOs; other branches link that convention rather than copy it.
Lombok belongs with Java/services; JPA-specific entity exceptions link persistence.
No scripts, UI metadata, scaffolds or separate tiny per-method skills are needed.

.agents/AGENTS.md would hold the concise context-maintenance rules currently routed
by root AGENTS/README: mandatory shared/local/status/ownership checks; relevant
branch reading; material-decision/handoff updates; canonical links; decision versus
proposal and verified versus unknown; privacy and stop-write boundaries. Root
AGENTS.md would remain a small pointer. Product/Domain/Services/C4/status stay
canonical docs, not copied into skills or replaced as ownership authority.

**Actual permission:** managed policy marks
`/home/nghlong3004/projects/longnest/.agents` read-only; observed directory mode is
0555. Only directory metadata/names were inspected; no private content was loaded,
no write probe, chmod, symlink, rename, escalation or alternate-location install.
Host operator must grant managed-policy write/create access **and** filesystem
write/create permissions for .agents/AGENTS.md and .agents/skills/longnest-backend/
with necessary parents, preserving unrelated/private paths read-only. After that,
a new bounded assignment can migrate (not duplicate) these conventions, use the
skill-creator initializer only if useful, validate with its quick validator plus
link/coverage review, and replace this document with a canonical routing link.
No skill validation/discovery is claimed now because no skill was created.

## Verified references and limits

Read-only official references consulted 2026-10-21; these establish framework
semantics, **not compatibility of an installed/pinned combined stack**:

- [MapStruct 1.6.3 reference](https://mapstruct.org/documentation/1.6/reference/html/)
  (mapping/annotation generation rechecked 2026-10-22):
  Spring component model, constructor injection, unmapped targets and Lombok binding.
  [Lombok constructors](https://projectlombok.org/features/constructor) /
  [release compatibility](https://projectlombok.org/changelog).
- [Jakarta Persistence 3.2 specification](https://jakarta.ee/specifications/persistence/3.2/jakarta-persistence-spec-3.2.html):
  entity/constructor/mapping/version rules; [Spring Data transactions](https://docs.spring.io/spring-data/jpa/reference/jpa/transactions.html)
  and [locking](https://docs.spring.io/spring-data/jpa/reference/jpa/locking.html).
- [Spring transaction annotations](https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/annotations.html),
  [transaction thread scope](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/transaction/annotation/Transactional.html)
  and [stereotypes](https://docs.spring.io/spring-framework/reference/core/beans/classpath-scanning.html).
- [Boot database initialization](https://docs.spring.io/spring-boot/how-to/data-initialization.html)
  and [Flyway versioned migrations](https://documentation.red-gate.com/flyway/flyway-concepts/migrations/versioned-migrations).
- [PostgreSQL version policy](https://www.postgresql.org/support/versioning/),
  [pgJDBC binding](https://jdbc.postgresql.org/documentation/use/) and
  [Flyway PostgreSQL module](https://documentation.red-gate.com/flyway/reference/database-driver-reference/postgresql-database).
  [Services](SERVICES.md#persistence-and-query-consistency) owns PostgreSQL-specific
  concurrency/durability/type bindings; [Delivery](DELIVERY.md) owns restore evidence.
- [MVC validation](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-validation.html),
  [Swagger annotations](https://github.com/swagger-api/swagger-core/wiki/Swagger-2.X---Annotations),
  [Boot JSON](https://docs.spring.io/spring-boot/reference/features/json.html).
- [Boot virtual threads](https://docs.spring.io/spring-boot/reference/features/spring-application.html#features.spring-application.virtual-threads),
  [task execution](https://docs.spring.io/spring-boot/reference/features/task-execution-and-scheduling.html),
  [Spring scheduling](https://docs.spring.io/spring-framework/reference/integration/scheduling.html),
  [Java 25 virtual threads](https://docs.oracle.com/en/java/javase/25/core/virtual-threads.html)
  and [Java 24 pinning change](https://docs.oracle.com/en/java/javase/25/migrate/significant-changes-jdk-24.html).

These reference checks alone establish no compilation, annotation processing,
JPA locking/isolation, migrations, HTTP generation or runtime behavior; actual
bounded implementation/build/smoke evidence is in CURRENT STATUS. Choose
compatible Boot-managed Hibernate/driver/Flyway and selected MapStruct/Lombok,
plus separately scoped OpenAPI artifacts during authorized implementation; do not
claim docs alone prove it builds.
