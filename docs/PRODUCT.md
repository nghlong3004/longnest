# Product intent and scope

State: approved first-release specification with selected Lead technical contracts,
not blanket implementation acceptance. [Decisions](DECISIONS.md) owns approvals/open details;
[Status](WORK_STATUS.md) owns current work/evidence.

## Purpose

Make personal income/expense recording fast enough for daily Telegram use. Accept
bounded Vietnamese language/shorthand such as `ăn trưa 45k` and `nhận lương 25tr`.
The bot's durable ledger is authoritative. This is not unrestricted language
comprehension, an LLM requirement or an authorization to operate a live service.

## Approved current release

- One personal user, VND, Asia/Ho_Chi_Minh business timezone (Vietnam UTC+7).
- Vietnamese end-user presentation; project coordination/docs and code identifiers
  remain English. Stable commands, IDs and machine contracts are not translated.
- Valid unambiguous input saves immediately with stable transaction ID and full
  receipt; ambiguous/missing facts request clarification without a write.
- Full create/read/update/delete, recent-entry selection and Edit/Delete actions.
  Delete is confirmed and reversible; Restore is a reusable application capability.
- Editable financial date distinct from immutable creation time; explicit date
  correction supports forgotten entries without a broad relative-date parser.
- Inclusive valid date ranges, overview income/expense/net totals, and type-filtered
  totals plus selectable lists paginated at 10 entries.
- Exact money validation, retry-safe mutations, authorization on every operation
  and recoverable storage. These are required, not optional later hardening.

Minimum journey: send `ăn trưa 45k` → saved #42/Edit/Delete → retrieve/update Date or
amount → inspect overview and expense total/list for a date range → request Delete
→ confirm the displayed entry → delete → Restore. Totals reflect each committed
change and state survives restart/recovery.

## Both Telegram UX and reusable services

The design is **both**, not a choice between them. Telegram is an adapter rendering
buttons, receipts, pages and clarification. OOP application services expose the
same create/get/recent/update/date-edit/delete/restore and range-query capabilities
to callers; financial logic is not confined to chat handlers.
[Services](SERVICES.md) is the canonical responsibility and Telegram → use case →
future-tool matrix. [Domain](DOMAIN.md) owns exact input/money/date rules.
Release-one application contracts have local synthetic implementation acceptance,
not durable/runtime or HTTP API acceptance. Edit now
has a complete preview → real-user Save or Cancel path; clear creation still saves
immediately. Same event/action replay returns its original result, while distinct
clear purchase messages save independently. See Services for failure/revision rules.

Default-off NATIVE-AI-1 replaces bespoke capture/explicit-date interpreters with
Spring AI native tools and short-lived memory. Grounded reads/follow-ups support
relative dates; “this month” is month-to-date and “food” is exactly FOOD, not groceries
or note search. Capture, edit previews, Delete requests and Restore reuse the same
services. No unlimited/fuzzy agent behavior or live language quality is claimed;
[Services](SERVICES.md#native-conversational-interpretation) defines the boundaries
and CURRENT STATUS owns acceptance.
A model can request an action but not supply authorization or human delete proof.
No integration or transmission of financial data is authorized now.

## Central full-product features, after the ledger release

1. [Daily budget](features/daily-budget/README.md): configurable default allowance
   and remaining/overspent feedback, with date and correction effects explained.
2. [Spending classification](features/spending-classification/README.md): fixed
   versus variable and mandatory versus discretionary, with explicit user overrides.
3. [Salary allocation](features/salary-allocation/README.md): explainable, editable
   or rejectable advice for savings, rent, food and other envelopes; never expenses
   or money movement merely because a plan exists.

These are central to the vision, not dropped or promoted into release one. Their
policy packages are proposals awaiting material choices. The [feature map](features/README.md)
separates independently usable slices and dependencies; salary advice need not
wait for the budget engine. [Web and LLM adapters](features/adapters/README.md)
are later options over the same reusable services.

Excel/templates/workbook charts/export remain optional later work, never a current
dependency. No round-trip editing. No bank integration, money movement or paid
external service is authorized. No separate agent platform, OCR/voice parser,
multi-user tenancy, client scaffolding or distributed architecture is implied.
The exact LifeOS reference has a [bounded read-only review](research/LIFEOS.md),
not an adopted dependency. “MCP REA” is unidentified and optional; no installation
or exact-identity guess is needed for this product.

## Genuine remaining decisions

Host and operational access/key custody, backup cadence/destination/retention,
recovery/purge duration, live operation and any external service costs are unselected.
Refund/transfer semantics are outside the current normal-income/expense grammar.
Release-one grammar/limits/update completion/store boundary/transport are selected
in [Decisions](DECISIONS.md#selected-technical-baseline). Exact dependency artifacts
and Java 25 runtime compatibility need later implementation evidence, not another
product choice. Future LLM strategy,
provider and privacy/cost activation remain later decisions. Do not ask again for
the approved user/currency/save/delete/date/range/list defaults.
