# Later adapters — A1 LLM and A2 web

A1 has a bounded default-off native conversation implementation; broader A1/A2 remain later scope, not
prerequisites for current CRUD, daily budget or salary advice.
[Services](../../SERVICES.md) owns typed capabilities/tool mappings;
[Domain](../../DOMAIN.md#input-to-normalized-contract) owns the normalized boundary;
[C4](../../ARCHITECTURE.md) owns architecture. Do not copy financial rules here.
NATIVE-AI-1 supersedes DS-FALLBACK-1/NL-QUERY-1 under CURRENT STATUS.
A2 and an unbounded general agent/tool loop do not exist.

The deterministic ledger's L1–L3 code now has bounded synthetic/local acceptance;
see CURRENT STATUS for exact artifacts and the remaining PostgreSQL/live evidence.
The shared create/get/recent/edit-preview/Save/Delete/Restore and range boundaries
are implemented inputs for later adapter work, not permission to activate a model.
Native Spring AI conversation is selected; live endpoint/model/privacy/cost remain
specific unresolved decisions; settled money/date/auth/confirmation behavior
is not reopened. No model may manufacture the host-only Save/Delete/Cancel proof.

## A1 — native conversation through accepted capabilities

NATIVE-AI-1 replaces historical capture/explicit-date JSON interpretation with existing
Spring AI 2.0.1 ChatClient, native @Tool/ToolCallingManager and bounded Spring memory.
Default remains off. [Services](../../SERVICES.md#native-conversational-interpretation)
owns grounding, identity/context, native capability and recovery contracts;
[server configuration](../../../apps/server/README.md#opt-in-deepseek-conversation)
owns activation, usage examples and concrete limits. No separate LifeOS runtime,
agent platform, provider HTTP codec or parallel legacy interpreter remains.

The useful path is grounded Vietnamese query → authoritative full-range summary/
selectable page → follow-up period/FOOD scope → current selected record/standard
controls. Month-to-date and FOOD-only semantics are settled. Native creates retain
immediate save for clear intent; edits produce preview, Delete requests confirmation,
Restore uses the normal authorized mutation. Save/Confirm/Cancel are never model
capabilities. Service results are rendered directly, not sent back to the model.

User/history data would be egress if enabled. Local/fake-model tests cannot establish
Vietnamese quality, arbitrary prompt-injection resistance, PostgreSQL durability or
live Telegram UX. Endpoint/model, permitted history, privacy/terms, credentials and
monetary limits remain separate live decisions. No provider call is authorized.

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
clarification and approval. Fake-response evaluation/code now exists; no provider
evaluation, credential use or API call has been performed.

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
