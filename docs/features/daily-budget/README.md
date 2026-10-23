# Daily budget — central future B1

Daily allowance is central to Longnest's full vision, **not current first-release
scope**. This document makes the next usable feature bounded without approving its
policy details. [Domain](../../DOMAIN.md#future-finance-domain-contracts) and
[Services](../../SERVICES.md#future-finance-capabilities) own shared types/contracts;
do not create a second expense ledger or make the interpreter enforce the budget.

## Recommended policy package — proposals requiring Human choice

- Everyday-variable spending basis: count active expenses except fixed costs;
  explicit Include/Exclude overrides win. Mandatory-variable spending counts.
  Unknowns count conservatively with a warning. This changes the **earlier proposal**
  of discretionary-only counting, not any approved daily-budget behavior; see
  [classification rationale](../spending-classification/README.md).
- User sets a nonnegative whole-VND default; zero is a real zero cap, unset is
  “allowance not configured”, not zero or unlimited. No invented default amount.
- No rollover or proration. Every business day is independent in Asia/Ho_Chi_Minh.
- Set/change takes effect for the **whole current local financial day and onward**,
  including already recorded eligible spending. Show the effect before Save;
  changing a limit never resets spent-to-date. Retain effective-date/revision
  history so earlier days are not rewritten by today's new default.
- Do not block an expense that exceeds the allowance. Save follows approved ledger
  behavior, then report remaining/overspent. Optional budget status failure must
  not turn a durable expense into “not saved”; show “saved; allowance unavailable”.

Other coherent choices (discretionary-only basis, next-day default changes,
rollover or proration) are alternatives, not silently selected requirements.
Human must settle the package or its material changes before B1 dispatch.

## B1 — configure allowance and get correction-aware daily feedback

**User outcome:** set/view/change a default allowance, save an eligible expense,
receive remaining/overspent feedback and retrieve a dated status after corrections.
Proposed controls: `/budget set 100k`, `/budget`, `/budget 2026-10-20`, Edit Limit.
These controls do not expand today's transaction text grammar automatically.

Illustrative scenario, assuming the recommended package is accepted:

```text
Set default: 100,000 VND; effective business date 2026-10-20.
Eligible lunch #42: 45,000 → spent 45,000; remaining 55,000.
Eligible purchase #43: 70,000 → spent 115,000; overspent 15,000.
Fixed rent: 8,000,000 → expense total changes, daily eligible spend stays 115,000.
Change default to 120,000 today → same spend 115,000; remaining 5,000.
Edit #43 from 70,000 to 50,000 → spent 95,000; remaining 25,000.
Confirm-delete #43 → spent 45,000; remaining 75,000.
Restore #43 → spent 95,000; remaining 25,000, once only.
```

The rent example assumes the user explicitly marked it fixed; a rent category
alone never supplies that label. Defaults/settings use exact 0–10^12 whole VND,
with revision checks. Invalid configuration keeps the old limit; a repeated Save
of the same policy action never resets spend or creates another policy revision.
Zero cap plus lunch 45,000 → overspent 45,000; unset cap → no remaining/overspent
number. These are worked criteria under the recommended package, not approvals.

Feedback names the financial date, limit and why an entry counts. Saving an excluded
expense can say “excluded: fixed cost”; the real ledger total still includes it.
No configured limit → receipt plus “allowance not configured”, never invented
remaining money. Net allowance is not a bank balance or salary envelope balance.

## Date, correction and consistency rules — proposed feature semantics

- Query active eligible expenses by editable transactionDate in the shared business
  timezone, not createdAt. At local midnight use the new date; no carry from prior
  day under the recommended package. No timer job is necessary to calculate a day.
- Backdate #43 from Oct 20 to Oct 19 → remove it from Oct 20, add it to Oct 19 using
  Oct 19's effective allowance/classification. Show affected dates, not a misleading
  “today” receipt. If no allowance existed on Oct 19, show that explicitly.
  Concrete branch after the restored scenario above: Oct 19 had an 80,000 limit
  and 30,000 other eligible spending; moving #43 (50,000) there leaves Oct 20 spent
  45,000/remaining 75,000 and Oct 19 spent 80,000/remaining 0. At next local midnight
  a new day starts with the 120,000 default and zero spent unless dated entries exist.
- Amount/type/date/classification changes, confirmed delete and restore recompute
  affected days from active authoritative records. An expense changed to income
  ceases contributing; restored entries contribute once. Repeat events do nothing extra.
- A failed ledger mutation leaves spending unchanged. A committed mutation with
  failed status rendering remains saved; later `/budget DATE` recovers the view.
- Proposed historical correction rule: a metadata edit changes eligibility for the
  transaction's own financial date; other records keep their labels. Changing a
  general classification policy applies from the next local financial date under the
  recommended C1 package; keep old policy versions for old days. No bulk historical
  rewrite/editor is part of B1. A cap change may recalculate today's whole day;
  that is not permission to rewrite yesterday's policy or labels.

## Inputs, scope and acceptance

**Required accepted inputs:** exact L1–L3 and C1 candidates: owned/revisioned ledger,
date/type/active-state query semantics and a stable eligibility explanation contract.
Human budget-policy choices above and shared extension signatures must be agreed.
None is an accepted implementation today; B1 is not ready to dispatch.

**Provisional write scope:** allowance policy/settings persistence, BudgetService
methods, budget Telegram controls and post-success expense feedback, tests and its
bounded integration. No salary allocations, rollover engine, bank actions, broad
classifier or recurring scheduler. Lead sequences shared mutation/routing changes;
no concurrent writer owns those shared files just because B1 has a folder.

**Evidence to collect:** actual set/view/change/expense interactions; values in the
scenario above; midnight/effective-date/backdate cases; excluded and unknown entry
reasons; zero/unset cap; invalid amount; update/delete/restore/reclassification;
direct service authorization, action replay, restart/reopen and injected rendering
failure after a successful save. Compare with ledger recomputation, not a separate
counter. Assertions are unrun designs, not evidence already obtained.

**Material missing choices:** eligibility basis/unknown handling; same-day versus
next-day setting changes; rollover/proration. Recommend the package above, no
automatic salary-derived cap, and no historical rewriting. Exact settings/revision
storage is Lead's technical choice after policy. [Status](../../WORK_STATUS.md)
alone records real ownership/readiness, not this planned brief.
