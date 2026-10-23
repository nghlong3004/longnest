# Salary allocation — central future S1/S2

Salary cash-flow advice is central to the full vision, **not current ledger release
scope**. It is advisory planning, never money movement or expense creation. Reuse
[Domain values](../../DOMAIN.md#future-finance-domain-contracts) and
[application boundaries](../../SERVICES.md#future-finance-capabilities). Nothing
here approves percentages, household priorities or external financial actions.

## Received salary is recorded money; allocation is a separate suggestion

`nhận lương 25tr` saves one income transaction through the approved ledger path.
Propose a separate “Suggest allocation” action on that saved salary ID. It reads
the selected active income/revision; it does not save the salary again or classify
every income as salary. Proposed default base is that one recorded received salary,
not gross salary, total account balance or inferred unspent cash. Multiple paychecks/
period pooling is an alternative requiring an explicit product decision.

Example only, with user-entered targets (not recommended personal percentages):

| Envelope | Configured rule | Explanation for salary 25,000,000 VND |
| --- | --- | --- |
| Savings | Fixed 5,000,000 | User's stated target, not a transfer. |
| Rent | Fixed 8,000,000 | User's stated target; not proof rent is unpaid. |
| Food | Fixed 4,000,000 | Advisory envelope, not an expense/category reclassification. |
| Other | Explicit residual | 25,000,000 − 17,000,000 = 8,000,000. |

User edits food to 5,000,000 → Other becomes 7,000,000. Keep plan saves an advisory
plan; Reject discards/rejects the suggestion. In both cases ledger income stays
25,000,000 and no expense/transfer appears. A later real rent payment is recorded
once as an ordinary expense; keeping the plan never records a second payment.

Infeasible example: received salary 10,000,000 against the same fixed targets
17,000,000 → shortfall 7,000,000. Show requested amounts and shortfall; no negative
Other, no silently reduced savings/rent/food and no money move. Ask the user to edit
targets or reject. **Recommend no automatic priority/optimization** until explicitly
chosen; “savings first” versus “rent first” is not a technical default to invent.

## Explainable configurable rules — proposed bounded model

- One rule per envelope: fixed VND amount OR an explicitly user-specified percentage
  of the selected salary base; at most one designated residual envelope. No default
  percentages and no mixed “fixed then percent” ordering hidden in the algorithm.
- Validate targets as nonnegative exact whole VND; zero is permitted for a plan
  target, not a salary transaction. Propose each percentage is within 0–100 and
  evaluated exactly; the conditional technical representation is integer basis
  points 0–10000 (at most two decimal percentage places), never binary float.
  The combined targets can still overcommit and must be checked together.
- S1 starts with per-suggestion fixed targets and Other residual. S2 adds saved,
  versioned rules reusable on the next selected salary; percentages need an agreed
  rounding policy before that mode is enabled. User-entered 20% of 25,000,000 gives
  5,000,000 as an illustration, not a recommended savings rate.
- Propose rounding each percentage target down to whole VND, explaining any rounded
  dong and placing the remainder in the designated residual. Exact conservation
  must hold. This rounding/residual rule is a proposal, not approved policy.
- If proposed amounts exceed base, return Infeasible with shortfall and conflicting
  rules; do not auto-scale or rank envelopes. If no residual is configured and
  targets leave money, show unassigned money and require an explicit choice rather
  than pretend the plan fully allocates salary. Recommend one Other residual.
- Recommend Keep only for a feasible, reviewed plan with source still current;
  infeasible results remain editable/rejectable, not a falsely completed plan.
  For a feasible plan, envelope amounts plus explicit unassigned money equal the base;
  no negative values, hidden rounding loss or additional financial transactions.
- Each row explains input rule, salary base, resulting amount and rule version.
  Plan edits affect only that draft unless the user separately saves a reusable rule
  profile. Editing a profile affects later suggestions, not existing plans silently.

## Revision and lifecycle semantics

An advisory draft records source transaction ID/revision, salary/base amount,
rule snapshot/version, editable results and state (proposed draft/kept/rejected/
outdated). Keep/Reject/Edit/Regenerate are authorized, retry-safe plan operations,
not financial writes. Names/states are proposed contracts, not instantiated classes.

Revalidate the source before Keep: if salary amount/type/date changed, show the
changed basis and regenerate/review instead of keeping stale
advice. Deleted/non-income source cannot produce a current plan. An existing kept
plan becomes visibly outdated, not an auto-adjusted ledger entry. Restore of salary
does not silently revive a plan based on an old revision. Read-time revision checks
can provide this safely; no event bus/scheduler is required by this document.
Changing a reusable profile does not invalidate an existing plan's frozen rule
snapshot: Keep explicitly shows the version used, and Regenerate is required to
adopt a newer profile. Editing the draft itself creates a new reviewed revision.
Infeasible output lists requested targets/shortfall, not a valid allocation whose
rows somehow sum to the salary; Keep is unavailable until corrected.

S2 worked rounding criterion, **if the recommended policy is approved**: base
1,000,003 VND, user-entered 20% savings, fixed rent 300,000 and food 200,000 →
savings floor(200,000.6)=200,000, Other=300,003. Explain the rounded fraction;
sum remains 1,000,003. A 100% savings rule plus fixed rent 1 VND is infeasible by
1 VND, never silently scaled. No percentage default is recommended here.

## S1 — one salary to an editable, rejectable explanation

**Outcome:** saved salary #51 → Suggest allocation → enter fixed targets → see
explained suggestion → Edit/Keep/Reject. Show infeasible targets without inventing
priority. A retained plan can be reopened by its own ID and shows its salary basis.

**Accepted inputs needed:** exact accepted L1–L3 candidate, including owned get,
income identity, revisions, delete/restore and date semantics. Stable shared plan
boundary and Human choice of base/target/residual/no-auto-priority behavior are
needed; none is accepted implementation now. No C1/B1 dependency.

**Provisional scope:** AllocationService one-off suggest/edit/keep/reject/get,
advisory-plan persistence, bounded Telegram flow and focused tests. No percentage
profile, recurring salary detection, budget integration or transfers. Shared routing/
Money/Transaction changes need Lead sequencing; a folder is not a write grant.

**Evidence:** actual 25-million example, Edit/Keep/Reject and 10-million shortfall;
no new ledger expenses and unchanged income totals; source edit/delete/restore
staleness; unauthorized access, repeated actions and restart/reopen. Record exact
candidate/inputs, actual results and limitations in a future handoff.

## S2 — save a reusable rule profile and explain the next suggestion

**Outcome:** configure named fixed/percentage/residual rules, save version, select
a subsequent salary, generate explained results, then edit/reject as in S1.

**Accepted inputs needed:** accepted S1 candidate and approved percentage base,
rounding/residual and reusable-rule semantics. Do not dispatch on a completion
message alone; verify retained plan/rule contracts exist.

**Provisional scope:** versioned allocation-rule set/get/update and percentage
evaluation in the existing AllocationService, configuration UX and tests. Do not
rewrite salary capture or add a separate planner framework/LLM dependency.

**Evidence:** unchanged S1 rules/results, correct exact-VND rounding/conservation,
fixed-plus-percent overcommit, rule-version explanation, profile change leaving
old plans intact, explicit regeneration and retries/authorization/reopen. No real
financial action is performed by testing a plan.

## Only material missing choices

Human: selected salary versus pooled/unspent base; what user targets/rules mean;
residual/rounding; infeasible-plan/priority policy. Recommend single received salary,
user-owned fixed targets initially, Other residual, no automatic prioritization,
and explicit regeneration on changed inputs. Envelope-to-actual-spending tracking,
automatic salary splits and bank integrations are separate future scope, not S1/S2.
Operational retention/privacy choices remain unapproved. Actual owners/readiness
live only in [CURRENT STATUS](../../WORK_STATUS.md).
