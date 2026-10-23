# Later adapters — A1 LLM and A2 web

Both are future scope, not prerequisites for current CRUD, daily budget or salary
advice. [Services](../../SERVICES.md) owns typed capabilities/tool mappings;
[Domain](../../DOMAIN.md#input-to-normalized-contract) owns the normalized boundary;
[C4](../../ARCHITECTURE.md) owns architecture. Do not copy financial rules here.
No accepted implementations exist and no Peer is assigned.

The deterministic ledger's L1–L3 code now has bounded synthetic/local acceptance;
see CURRENT STATUS for exact artifacts and the remaining PostgreSQL/live evidence.
The shared create/get/recent/edit-preview/Save/Delete/Restore and range boundaries
are implemented inputs for later adapter work, not permission to activate a model.
Provider/endpoint/privacy/cost and rules-first-versus-replacement decisions remain
specific to that future integration; settled money/date/auth/confirmation behavior
is not reopened. No model may manufacture the host-only Save/Delete/Cancel proof.

## A1 — future LLM interpretation through accepted capabilities

**Outcome/examples:** user supplies complete phrasing outside current grammar; a
future authorized interpreter proposes the same typed request and presents the
service result. Missing amount/type clarifies. Given host business date 2026-10-20,
`hôm qua` can mean 2026-10-19 in Asia/Ho_Chi_Minh; today's parser is not changed.
Delete remains request → displayed target → actual user proof → host-only commit.

**Required inputs:** exact accepted L1–L3/services candidate plus Human approval for
provider, cost and privacy/external transmission. Choose rules-first fallback OR
interpreter replacement; this is not a required pair of runtime integrations.
Budget/allocation tools may be exposed only after their own service is accepted.

**Provisional scope:** bounded interpreter/tool adapter, routing and contract tests,
not an agent platform or core-finance rewrite. Lead owns shared schema/service
changes. No owner, provider credentials or execution authority is assigned now.

**Evidence:** R18–R19 below; direct-service parity, hostile/unknown fields, no actor/
confirmation spoofing, truthful saved outcomes and missing-fact behavior. Unresolved
mutation ambiguity never executes. Material decisions are provider/privacy/cost and
strategy; no such decision authorizes money movement or direct persistence.

### Candidate and minimal OOP path — proposals

DeepSeek is the preferred **candidate under consideration**, not an approved
integration. See [source-backed LifeOS/DeepSeek findings](../../research/LIFEOS.md).
Reuse LifeOS routing, bounded-result and egress-policy concepts, not its runtime,
hooks, memory, CLI subprocess or provider trust rules. No separate agent platform.

In the existing Spring Boot/Java 25 application, propose one InputInterpreter
contract with RulesInterpreter and a future DeepSeekInterpreter; one small provider
HTTP codec/client belongs inside that adapter. An allowlisted dispatcher maps its
typed requests to the existing [service matrix](../../SERVICES.md#telegram--application--future-llm-capability-matrix).
These are responsibilities, not a requirement for one new class per label/tool.
Host context supplies principal, business time and operation identity outside model
arguments. Full response/schema validation precedes dispatch; services revalidate
Money/date/range, authorization, target/revision, idempotency and durable consistency.
Delete confirmation stays host-only from the actual user's displayed-target action.
Selected PE1 requires host Save of an immutable update preview too. Calculations,
budget and allocation arithmetic stay deterministic; no repository/SQL model tool.

Recommend rules-first fallback initially: clear supported text stays local; genuine
missing facts clarify locally; only unsupported/uncertain complete phrasing goes
to an approved future provider. Known invalid business values are not “repaired”
by a model. Interpreter replacement remains an alternative using identical typed
contracts, tools and safeguards; it changes text routing/egress/latency/coverage,
not financial logic. Buttons and explicit query/delete confirmations still route
directly. Neither route is implemented; current release remains deterministic.

Minimize egress to the relevant utterance/business reference and necessary selected
record fields, not the ledger/history. Propose one completed interpretation and at
most one mutation request per utterance; reject surplus/parallel mutations before
executing any. Queries can be bounded/sequenced by the host. Render the authoritative
service receipt directly; provider narration is not proof of a saved transaction.
No new blanket confirmation is added to approved clear creation. Language coverage
and wrong-but-schema-valid interpretations require the evaluation below, not trust
in a strict schema or model confidence.

### Smallest future evaluation — synthetic, mock-only effects

Propose 24 hand-labeled synthetic cases, no personal ledger text or real identities:

| Group | Cases / expected business outcome |
| --- | --- |
| 8 clear/coverage cases | `ăn trưa 45k`, `nhận lương 25tr`, `-45k ăn trưa`, `+25tr lương`, `-1,5tr đồ dùng`, `an trua 45k`, `ăn trưa 45`, `Mình vừa trả 45 nghìn cho bữa trưa.` → exact labeled values (including bare 45 = 45 VND), never a fabricated category/fact. Only the last requires future prose interpretation; rules-first supported cases make zero provider requests. |
| 6 missing/ambiguous/invalid | `ăn trưa`, `45k`, `+45k ăn trưa`, `45.000`, multiple entries, unsupported currency → clarification/rejection and zero mutation. No typical amount or guessed transaction type. |
| 4 date/query cases | Supplied host date 2026-10-21, `hôm qua` → 2026-10-20 only in future LLM route; Vietnam-midnight stable retry; inclusive single-day range; reversed range → correction, no substituted period. |
| 4 action/security cases | Ambiguous delete → select target; selected delete → request only, real-user Confirm required; forged actor/confirmation in note/tool text → rejected; update preview/stale revision → selected PE1 Save/conflict behavior. |
| 2 fault cases with injected variants | Same-source repeated/different provider tool-call IDs and extra mutation batch → one effect or rejection; timeout/429/empty/truncated JSON → bounded failure/no write, or existing committed receipt if the mock service already committed. |

First separately authorized increment can use canned provider responses and mock
services to prove schema/error/dispatch boundaries **without a network/model**;
this does not prove DeepSeek quality or satisfy production A1 dependencies. A later
explicitly authorized provider trial reuses synthetic cases/tool mocks only. Start
with Flash/non-thinking on the selected endpoint; compare another mode/model/strict
schema only if it changes the decision within an approved request/token/spend cap.
Do not run both a harness and the application just to evaluate these contracts.

Collect exact date/model-returned identifier, endpoint/mode/schema/prompt version,
expected/actual intent, incorrect amount/date/target rate, clarification coverage,
forbidden dispatch count, replay behavior, latency percentiles and token/cost usage.
Repeat a small sample to expose variability; report sample size, failures and
unrun scenarios. Proposed safety gate: zero unauthorized/unconfirmed/duplicate
effects and no unexplained money/date/target error in this tiny fixture set; not a
statistical guarantee or permission for real financial data.

Before execution: explicit evaluation authority; Lead-frozen fixture/contract and
selected PE1/PE2 contracts for tested actions. For provider trials Human must choose
direct versus third-party host, accept applicable terms/synthetic egress, authorize
credentials and hard cost/request/latency limits; Lead selects exact endpoint/model/
schema and retry policy. Real-data use needs separate privacy/retention/region
clarification and approval. No evaluation, code, SDK, key or API call exists now.

## A2 — later visual web path to the same ledger

**Outcome:** authorized personal user opens date-filtered entries, saves a correction,
reloads and sees it in Telegram too; Delete/Restore obey the same application rules.

**Required inputs:** accepted service/ledger contracts plus approved authentication/
exposure/API and host choices. Preserve later React latest/TypeScript 7/Vite latest/
Tailwind latest preferences; recheck exact release/tool compatibility when authorized.

**Provisional scope:** bounded client and its transport/auth integration/tests, not
a second ledger, new bank API or speculative frontend scaffold now. Sequence shared
backend/API changes with Lead; do not write common moving scope concurrently.

**Evidence:** usable mobile/browser layout, auth rejection, save/reload, matching
totals and cross-adapter correction/delete/restore. These are unrun future criteria;
valid data/build output alone does not prove the UI.

## Acceptance

R18–R19 moved here from the old root requirements file; they are not first-release
gates. [Ledger R1–R17](../ledger/ACCEPTANCE.md) remains canonical for current behavior.

| ID | Future design criterion | Evidence when separately authorized |
| --- | --- | --- |
| R18 | Same typed contracts for both interpretation options | Compare rules-first/fallback and LLM-replacement routes on clear and unsupported complete phrasing. Same commands/queries/results; get/recent/range/list/update/date-edit/request-delete/restore use the bounded service matrix, not prompt-owned rules or a model-owned store. |
| R19 | Parser uncertainty differs from missing business facts | `ăn trưa` asks amount, `45k` asks type; optional category need not be guessed. Ground relative dates in supplied business reference. Ambiguous mutations never execute and no model can manufacture delete/Save proof. |

Collect evidence only under the later authority; no actual financial text is sent
to a model in this task. Planning/readiness is not assignment authority; see the
single [status source](../../WORK_STATUS.md).
