# Longnest

Personal-finance capture and review in Telegram.

## Context maintenance — pending relocation

The manually supplied [backend skill](.agents/skills/longnest-backend/SKILL.md)
routes to the existing Backend rules.

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
