# Feature slices and delivery order

This replaces the former root roadmap. **Slice plans, not assignments**: bounded
backend implementation is authorized and local bootstrap/input/service-store code
is accepted only as recorded in [CURRENT STATUS](../WORK_STATUS.md). That source alone records actual
owners/candidates/evidence. Product scope remains in [Product](../PRODUCT.md).

## Map and smallest useful path

| Feature folder | Bounded usable slices | Priority and required accepted implementation inputs |
| --- | --- | --- |
| [ledger/](ledger/README.md) | L1 capture/retrieve; L2 correct/delete/restore; L3 date-range views | Current first release. Local L1 code has bounded acceptance; durable PostgreSQL and authenticated transport evidence remain outstanding. L2 needs accepted durable L1; L3 needs accepted L2. Exact work authority stays in CURRENT STATUS. |
| [spending-classification/](spending-classification/README.md) | C1 classify and explicitly override budget eligibility on an owned entry | Future; accepted L1–L3 ledger behavior plus agreed classification/eligibility semantics. Existing category names are not those semantics. |
| [daily-budget/](daily-budget/README.md) | B1 set/change allowance → expense receipt/status → correction-aware daily view | Central future product; accepted C1 and ledger mutation/query behavior plus Human budget policy choice. Not a first-release gate. |
| [salary-allocation/](salary-allocation/README.md) | S1 explainable editable/rejectable salary plan; S2 reusable versioned rule profile | Central future product. S1 needs accepted ledger source/revision semantics and allocation policy; S2 needs accepted S1. No dependency on a budget engine or bank API. |
| [adapters/](adapters/README.md) | A1 future LLM interpreter/tool adapter; A2 later web client | Future optional adapters. Accepted reusable ledger services for those actions; each additional feature's service must be accepted before exposing it. Provider/privacy and web authentication choices remain separate. |

Smallest path: authorized Telegram entry → interpreter → shared application service
→ durable ledger → saved ID/recent/get (L1), then complete personal correction and
reversible deletion (L2), then inclusive range totals/type lists (L3). L1 alone is
not the finished CRUD release. R1–R17 are canonical in
[ledger acceptance](ledger/ACCEPTANCE.md).

Current bounded authority also permits synthetic-only L2/L3 implementation over
inspected stable contracts while PostgreSQL durability is unverified (see CURRENT
STATUS). This advances ready code, not acceptance of durable L1 or waiver of the
integration dependencies above; real transaction/reopen and live UI evidence remain
outstanding. Keep each increment's exact scope/base/disposition explicit.

Then propose C1 → B1 for daily-budget usefulness; S1 can follow accepted ledger
inputs independently of B1/C1, then S2. Salary allocation does not wait for a
classification engine. This is an order proposal, not permission to start blocked
work or to create foundation-only tasks.

## Contracts and ownership

[Domain](../DOMAIN.md), [Services](../SERVICES.md) and
[C4](../ARCHITECTURE.md) remain canonical shared contracts; feature files link to
them instead of copying the ledger schema, tool matrix or financial invariants.
Each slice below specifies its own outcomes, candidate inputs, provisional write
scope, evidence and decisions. Re-read actual handoffs/status before any dispatch:
accepted documentation is not an accepted service or running bot.

Potential scopes are planning boundaries, **not write grants** or predetermined
package/file lists. A future brief must name a real base/candidate and paths, one
writer, dependencies and reopening conditions. Lead owns shared docs/contracts;
a Peer proposes shared changes before writing them. No simultaneous writers in
common Transaction, Services, Telegram routing or persistence files. Even input-
independent salary/classification work needs separated real scopes or sequenced
integration; this table does not grant parallel ownership.

## Full vision without first-release inflation

Daily allowance and salary cash-flow advice are central to Longnest's full vision,
not discarded optional ideas. Their material policy choices are recorded in their
feature folders. Current implementation priority remains the approved ledger bot.
Later web/LLM, Excel/export and recurring automation are not mandatory prerequisites.
Advisory plans never become real expenses, balance claims or money-moving actions.

## Evidence and readiness

Release-one design is selected; future policy/slice scope is proposed. Product
acceptance criteria remain outstanding. Synthetic bootstrap/input/unit/proxy
behavior is verified, not PostgreSQL durability or Telegram usability. No working
ledger/bot, classification, budget, allocation or live adapter is established. Ready future inputs
must be established by exact accepted candidates, not a completed roadmap row.
Human chooses material feature policy; selected technical behavior is canonical
in Domain/Services, with artifact binding/verification during later implementation.
[Delivery](../DELIVERY.md) owns verification/release/
operations conventions. Optional [LifeOS research](../research/LIFEOS.md) informs
context/evidence boundaries, not product dependencies or a new agent platform.
