# Longnest

Personal-finance capture and review in Telegram, backed by an authoritative durable
ledger. Design documentation and a manually supplied backend starter are present.
Backend bootstrap, pure input/domain handling and local create/get/recent service,
JPA/Flyway code are accepted. An opt-in TelegramBots SDK capture/get/recent adapter,
including bounded Next/Previous and current-record selection, is implemented with
synthetic boundaries; its current candidate/disposition is in
CURRENT STATUS. PostgreSQL execution remains unverified.
Vietnamese MessageSource presentation, explicit operator-only owner initialization,
edit/Save/Cancel/confirmed Delete/Restore, and inclusive range summaries/lists now
have local synthetic acceptance. First-release deterministic flows are implemented;
PostgreSQL durability, live Telegram UX and release acceptance remain unverified.
See [server instructions](apps/server/README.md) and CURRENT STATUS for exact
evidence and limits. No working ledger/bot, frontend or deployment is established.

## Context tree — start here

- [CURRENT STATUS](docs/WORK_STATUS.md): assignment, owners, evidence and next frontier.
- Product and domain
  - [Product intent and scope](docs/PRODUCT.md)
  - [Vietnamese input and normalized transaction contract](docs/DOMAIN.md)
- Feature paths — outcomes, dependencies, provisional scopes and evidence
  - [Slice map and proposed order](docs/features/README.md)
  - [Ledger CRUD/query](docs/features/ledger/README.md) and [acceptance](docs/features/ledger/ACCEPTANCE.md)
  - [Spending classification](docs/features/spending-classification/README.md)
  - [Daily budget](docs/features/daily-budget/README.md)
  - [Salary allocation advice](docs/features/salary-allocation/README.md)
  - [Later LLM/web adapters](docs/features/adapters/README.md)
- Design and delivery
  - [C4 architecture and stack preferences](docs/ARCHITECTURE.md)
  - [OOP application services and future tool contracts](docs/SERVICES.md)
  - [Existing backend conventions and blocked .agents routing plan](docs/BACKEND_RULES.md)
  - [Decisions, proposals and unresolved choices](docs/DECISIONS.md)
  - [Proposed verification, release and operations](docs/DELIVERY.md)
- Optional research
  - [LifeOS reuse and DeepSeek candidate assessment](docs/research/LIFEOS.md)
- [Agent entrypoint](AGENTS.md): mandatory instructions and context routing.
- [Disposable PostgreSQL dev definition](deploy/README.md): usage/storage limits;
  not a running database, deployment or permission to start containers.
- [Backend dev launcher](scripts/README.md): automatic Maven package/tests then
  explicit root `.env.dev` runtime loading;
  normal ledger startup can run Flyway and requires separate database scope.

Read the relevant branch, not every document. Links point to canonical sources;
this tree is an index, not write ownership or evidence that a design exists in code.

For authorized backend work: read CURRENT STATUS and local/shared
rules first, then Backend rules → ledger/README → Domain → Services → ledger/ACCEPTANCE; consult
C4 for boundaries and Delivery for verification. This is the selected release-one
specification, not a working bot. Future budget/salary/LLM policy approval is not
needed to begin that bounded ledger path once implementation itself is authorized.

## Context maintenance — pending relocation

The manually supplied [backend skill](.agents/skills/longnest-backend/SKILL.md)
routes to the existing Backend rules. Managed read-only policy still blocks its
modification and the fuller [routing relocation](docs/BACKEND_RULES.md#project-local-routing-proposal--blocked).
Existing guidance remains here/in Backend rules pending permission; the proposed
four-reference layout is not claimed as installed.

- Read mandatory instructions, status/real ownership and only the relevant branch
  in full. Product/Domain/Services/C4/Decisions and feature acceptance remain
  canonical; planning folders and context indexes are not assignments.
- After material decisions, interface/scope changes, handoffs or findings, update
  the owning source/affected links. Lead maintains sole WORK_STATUS; Peers report
  handoffs unless explicitly assigned otherwise. Link rather than copy contracts.
- Distinguish proposal/decision, design/implementation and verified/failed/unknown.
  Check exact-case links, examples and C4 source boundaries, not runtime behavior
  separately from application checks. No extra maintenance scripts or tracker.
- Preserve unrelated/private state; no private conversations/session IDs or secrets
  in artifacts. Do not read/overwrite .env/.env.dev or modify global configuration.
  Stable candidate submission stops writes until a new bounded assignment.

## Target repository layout

This is the requested target, **not a claim that every path exists**. Empty runtime
areas are documented rather than padded with placeholders.

```text
longnest/
├── README.md                Context entrypoint
├── AGENTS.md                Small mandatory routing entrypoint
├── docs/                    Shared contracts, decisions and sole WORK_STATUS.md
│   ├── features/            Ledger, classification, budget, salary, later adapters
│   └── research/            Optional source-backed observations, not requirements
├── apps/
│   ├── server/              Java 25 / Spring Boot; local input/services/JPA/Flyway code
│   └── client/              Future React / TypeScript 7 / Vite / Tailwind client
├── deploy/                  PostgreSQL-only disposable dev Compose; no deployment
├── scripts/                 Explicit backend dev launcher and synthetic checks
├── .env                     Private local runtime values; never commit
├── .env.example             Optional safe placeholders/documentation only
├── .env.dev                 Private development overrides; never commit
└── .agents/                 Read-only backend skill; fuller guidance relocation blocked
```

Directory names are deliberately lowercase. `docs/WORK_STATUS.md` remains the sole
status source; no status migration is needed. The existing server starter is
preserved. Only the disposable dev database's public placeholders are defined in
Compose. The explicit backend launcher can load private `.env.dev` values; no
private values were read/configured and no database/backend runtime was started.
Deployment remains unconfigured. Environment roles and partly proposed
precedence are in [Delivery](docs/DELIVERY.md#environment-convention-proposal).

## SDLC Lite

Keep one short chain: product intent → domain/requirements → design/decisions →
authorized implementation → recorded verification → separately authorized release.
Bounded initial backend implementation and synthetic/local checks are authorized
under the named scopes in CURRENT STATUS. Acceptance examples are future
checks, not passing tests. Future web/LLM work and Excel/export are not release-one
dependencies. Advice never authorizes moving money.
