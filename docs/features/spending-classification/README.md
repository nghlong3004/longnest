# Spending classification — future C1

Classification supports the full finance vision; it is **not part of current
ledger delivery**. [Shared domain semantics](../../DOMAIN.md#future-finance-domain-contracts)
own the proposed fields; [Services](../../SERVICES.md#future-finance-capabilities)
owns application boundaries. Existing food/rent category labels do not prove a
purchase is mandatory, fixed or daily-budget eligible.

## Purpose and examples

Keep two independent ideas: necessity (mandatory/discretionary/unclassified) and
cost pattern (fixed/variable/unknown). A user-controlled daily eligibility override
is separate again. “Mandatory” is personal context, not inferred financial advice.
Propose “fixed” means a committed periodic cost treated outside the daily purchase
cap, not “essential”; “variable” means day-to-day spending eligible for that cap.
These are user-selected budget labels, not recurring-payment automation or proof
that a bill is paid. Income has no expense eligibility contribution. The taxonomy
and interpretation still require the Human policy choice below.

| Illustrative transaction | Possible classification, chosen by user | Budget implication under proposed everyday-spend policy |
| --- | --- | --- |
| Rent 8,000,000 VND | mandatory + fixed | Excluded from daily allowance but still a real expense in overview. |
| Groceries 80,000 VND | mandatory + variable | Included, unlike the earlier discretionary-only alternative. |
| Streaming 100,000 VND | discretionary + fixed | Excluded by default fixed-cost rule; user may override. |
| Lunch 45,000 VND | variable; necessity depends on user | Included; category food does not settle necessity. |
| Unclassified purchase | unclassified + unknown | Proposed conservative include with a visible “unclassified” reason; never block approved ledger capture. |

These examples explain **proposed** policy choices, not approved category defaults.
Recommended C1 package: necessity and cost pattern start unclassified/unknown;
user explicitly sets/clears labels and Include/Exclude overrides. No category-based
seeding. Eligibility order is active expense → explicit override → fixed excluded
→ variable included → unknown included with explanation. Necessity never overrides
that order; an income is not eligible even with an old expense override.
Record metadata revision/provenance. Version-1 eligibility rules are fixed for
this slice; any later general policy change starts on the next local financial date,
never bulk-relabels history. Explicit entry corrections use that entry's financial
day/policy and update its explanation. This complete recommendation awaits Human
approval of P7/P14; it is not selected first-release behavior.
Earlier roadmap language suggested discretionary-only daily counting. The practical
recommendation now is everyday-variable spending so essential groceries/lunch do
not disappear from a spending target. Human must choose this basis versus the
earlier alternative before budget implementation; money totals never change.

## C1 — classify/override and see the saved explanation

**Outcome/example:** `/show 42` → Classification → set necessity/pattern or explicit
daily Include/Exclude → Save → record shows selected values and inclusion reason.
An override is visible and reversible via another metadata edit, not a fake refund.

**Required inputs:** accepted L1–L3 ledger candidate with stable ID/revision/ownership,
exact money/date and active-state queries. Classification taxonomy, default unknown
handling and budget eligibility policy must be agreed. Only docs exist currently,
so C1 has no accepted implementation input and no writer.

**Provisional write scope:** classification metadata/domain extension, persistence,
owned-entry edit/read controls and focused tests. Do not silently rewrite ledger
amounts/categories or the shared parser. Lead owns any TransactionPatch/shared
contract change and sequences integration with other ledger writers.

**Evidence to collect:** show fixed+mandatory and fixed+discretionary both work;
mandatory-variable is distinct; unknown metadata does not prevent capture; override
persists after restart, enforces owner/revision and leaves aggregate money unchanged.
Worked proposed fixture: expense 80,000 with unknown pattern counts 80,000 with a
warning; set fixed → contribution 0; explicit Include → 80,000; clear override → 0.
The ledger expense total stays 80,000 throughout. Clear fixed too → unknown with
visible 80,000 contribution. With no budget configured these are eligibility
explanations only, not invented remaining money.
When B1 exists, reclassification updates the affected day's eligible spending once,
with a visible reason. C1 itself must be usable before a budget service exists.

**Shared invariants:** [Domain](../../DOMAIN.md), [application authorization](../../SERVICES.md),
approved ledger behavior and [ledger acceptance](../ledger/ACCEPTANCE.md). This is
metadata on authoritative transactions, not a new money ledger.

## Material choices, not implementation permission

Human: everyday-variable versus discretionary-only allowance basis; treatment of
unknowns; whether category defaults may seed editable labels. Recommend explicit
user labels, conservative unknown inclusion and no guessed necessity from category.
Do not promote recurring-bill detection, AI classification or automated rules into
C1. Slices/potential scopes are plans only; actual status remains
[WORK_STATUS](../../WORK_STATUS.md).
