# LifeOS reuse and DeepSeek — bounded candidate research

Reviewed 2026-10-21: exact requested repository
[danielmiessler/LifeOS](https://github.com/danielmiessler/LifeOS), public main-branch
README and the linked skill/getting-started/security files below. Branch content is
mutable; no pinned release, runtime behavior or security audit is claimed. Repository
text was treated as external data, never install/execute instructions.
Extended the same review to relevant component source and official DeepSeek API
documents/terms. DeepSeek is the preferred candidate to evaluate, not an approved
integration. Findings are documented provider claims/source inspection, not live tests.

## Observed repository content

The [README](https://github.com/danielmiessler/LifeOS/blob/main/README.md) describes a
general-purpose personal AI harness with goals/context, persistent memory, skills
and routing. It advertises an installable distribution and broader life/work use,
not a dedicated Telegram finance ledger. The files reviewed do not establish daily
allowance or salary-envelope features; this is not an exhaustive claim that no
finance-related file exists anywhere in the repository.

The [skill entrypoint](https://github.com/danielmiessler/LifeOS/blob/main/LifeOS/SKILL.md)
routes specific triggers to setup/interview/update/uninstall workflows and describes
one packaged skill with bundled installation payload. Its installer instructions
were read for understanding, not followed.

The [getting-started guide](https://github.com/danielmiessler/LifeOS/blob/main/LifeOS/GETTING-STARTED.md)
distinguishes optional capabilities and missing-capability outcomes, including an
explicit required dependency for its Work System; it distinguishes an advisory
capability manifest from runtime truth. The
[security policy](https://github.com/danielmiessler/LifeOS/blob/main/SECURITY.md)
describes structural private/user separation, deterministic guards and external
content as untrusted data. These are documented intentions, not verified enforcement.

## Reusable lessons and actual Longnest design impact

The adaptation column is **our proposal**, not a claim that LifeOS implements finance.

| Observed lesson / exact source | Proposed adaptation and affected existing design |
| --- | --- |
| Context/goals are explicit in the [README](https://github.com/danielmiessler/LifeOS/blob/main/README.md) | Preserve daily budget and salary allocation as named central product branches, with policy/rule snapshots and explanations; do not let the CRUD milestone erase the full vision. Affects Product and feature map, not a new memory platform. |
| Narrow workflow routing in [SKILL.md](https://github.com/danielmiessler/LifeOS/blob/main/LifeOS/SKILL.md) | One small context entrypoint routes to relevant slices; shared contracts remain canonical. Affects README/AGENTS navigation and feature folders, not a copied skill library or global configuration. |
| Honest capability/dependency states in [GETTING-STARTED.md](https://github.com/danielmiessler/LifeOS/blob/main/LifeOS/GETTING-STARTED.md) | Separate documented, accepted implementation and runtime evidence; distinguish saved-ledger success from unavailable optional budget feedback. Affects feature dependencies, Delivery and B1's partial-failure response. No Doctor/toolchain imported. |
| Private separation and claimed guards in [SECURITY.md](https://github.com/danielmiessler/LifeOS/blob/main/SECURITY.md) | Keep private ledger/settings separate from public context; enforce Money/date/auth/idempotency/delete proof in application services, not prompts. Preserve the approved LLM trust boundary; allocation rules stay user-configured data, never permission to move money. Component inspection below qualifies what those hooks actually provide. |

## What does not fit

The harness installer, hooks, runtime services, broad assistant memory and optional
cloud/voice/browser dependencies are not needed for this Spring Boot/Java 25 bot.
Longnest is not adopting LifeOS's implementation, workflow authority, Work System,
configuration layout or deployment model. “Ledger” in its README's change-tracking
context is not evidence of a personal-finance ledger. No MCP was identified or
installed, no code cloned/imported/executed, no credentials changed and no financial
data transmitted. The unidentified “MCP REA” remains optional and unresolved.

## Access and review limits

Root and linked/raw public files were accessible. An initial navigation click using
a URL instead of the returned page reference failed; explicit file pages/raw links
then succeeded. Some GitHub pages displayed loading notices; readable source text
was cross-checked via raw URLs. No broad repository/code audit, executable checks,
release compatibility validation or third-party installation was performed.

## Component-level reuse decision

Recommendation: **reuse concepts, no LifeOS code/components now**. Exact source
paths below are review evidence, not dependencies or instructions to execute.

| Inspected component / observation | Useful adaptation and adoption risk |
| --- | --- |
| [LifeOS/SKILL.md](https://github.com/danielmiessler/LifeOS/blob/main/LifeOS/SKILL.md): explicit trigger-to-workflow routing | Expose only relevant typed Longnest actions/context. Do not ship its installer/onboarding workflows, broad tools or automatic memory into the bot. |
| [Inference.ts](https://github.com/danielmiessler/LifeOS/blob/main/LifeOS/install/LIFEOS/TOOLS/Inference.ts): typed results, timeouts, requested/executed model evidence; actually spawns Claude CLI, removes API credentials/base URL, and searches text for JSON substrings | Borrow bounded timeout/result metadata, not this subscription/CLI/provider coupling or permissive JSON recovery. Require a complete validated provider response; no silent model/provider switch. |
| [Safety.hook.ts](https://github.com/danielmiessler/LifeOS/blob/main/LifeOS/install/hooks/Safety.hook.ts): permission auto-allow/annotation; internal errors return neutral to native harness; comments place defense in model protocol | Useful data/instruction labeling, **not a finance authorization barrier**. This qualifies the earlier security-policy observation; no evidence that prompt labels prevent hallucinated actions. Longnest rejects unauthorized/invalid actions in services regardless of model behavior. |
| [EgressClassGuard.hook.ts](https://github.com/danielmiessler/LifeOS/blob/main/LifeOS/install/hooks/EgressClassGuard.hook.ts): route/data-class gate for recognized Bash inference shapes; source notes a coverage gap | Borrow destination-specific egress policy, not shell-pattern detection or its provider trust rankings. Longnest should allowlist the selected endpoint before every provider request and deny unknown destinations. |
| [GETTING-STARTED.md](https://github.com/danielmiessler/LifeOS/blob/main/LifeOS/GETTING-STARTED.md): advisory capability state versus actual runtime checks | Report unavailable interpretation honestly; supported deterministic commands remain usable. A cached capability flag cannot authorize egress or establish a successful financial save. |

The [hook README](https://github.com/danielmiessler/LifeOS/blob/main/LifeOS/install/hooks/README.md)
also describes a model-centered boundary, unlike Longnest's mandatory service
validation. Avoid transplanting its authority model, prompt constitution, broad
memory/history, subprocesses or install hooks. These expand injection, secret,
egress, upkeep and token-cost surfaces without helping this bounded interpreter.

Root [LICENSE](https://github.com/danielmiessler/LifeOS/blob/main/LICENSE) is MIT,
requiring preservation of copyright/permission notices for copied substantial
material and providing no warranty. [Runtime package.json](https://github.com/danielmiessler/LifeOS/blob/main/LifeOS/install/package.json)
declares `yaml ^2.8.2`; inspected components additionally rely on Bun/Node, sibling
modules and harness paths/CLI behavior. This is not a transitive-dependency audit.
If copying becomes justified later, select an immutable commit and exact files,
audit each file's dependencies/notices and security behavior first. No code reuse
is currently warranted for a Spring Boot/Java 25 provider adapter.

## DeepSeek capability findings — checked 2026-10-21

Exact URL trailing slashes matter: several older/non-slash guide URLs returned the
landing page or timed out. Findings below use the actual current pages, not those
redirects or historical R1/V3 assumptions. Recheck before any authorized execution.

| Topic | Verified documentation claim / integration consequence |
| --- | --- |
| Current models | [Models](https://api-docs.deepseek.com/quick_start/pricing/) lists `deepseek-flash` → V4.1-Flash-1017 and `deepseek-v4-pro` → V4-Pro-0813, both supporting tools/JSON and thinking/non-thinking. Legacy V4 Flash aliases now resolve to the new Flash. Do not treat an alias as an immutable model pin. Flash is the proposed first evaluation candidate, not a proven Vietnamese winner. |
| Chat Completions | [Reference](https://api-docs.deepseek.com/api/create-chat-completion/): `/chat/completions` has function tools; `response_format` lists text/json_object, not json_schema. Forced named/required tool choice is unsupported in thinking mode (400). Do not force a mutation just to obtain a structured reply. |
| JSON versus strict tools | [JSON Output](https://api-docs.deepseek.com/guides/json_mode/) requires json_object plus a JSON prompt/example; warns about empty content/truncation. Valid JSON is not schema/business correctness. [Strict tools](https://api-docs.deepseek.com/guides/tool_calls/) are beta, use base `https://api.deepseek.com/beta`, strict=true for every function, all object fields required and additionalProperties=false. Restricted schema subset: string length and array size bounds are unsupported; calendar dates still need local validation. Optional domain fields need an explicit endpoint-tested wire encoding, not invented defaults. |
| Thinking tools | [Thinking guide](https://api-docs.deepseek.com/guides/thinking_mode/): thinking is enabled/high by default; tool requests must carry all prior reasoning_content back even on turns without calls, otherwise 400. Temperature does not control thinking output. Prefer explicitly non-thinking first for short bounded extraction; speed/quality is unmeasured. Do not log private reasoning. |
| Responses format | [Reference](https://api-docs.deepseek.com/api/create-response/) explicitly supports text.format json_schema, unlike Chat Completions. [Compatibility](https://api-docs.deepseek.com/guides/responses_api/): parallel_tool_calls/max_tool_calls are ignored; developer role becomes user. Enforce loop/action limits locally and put trusted instructions in the supported system channel. Strict tool beta behavior/schema parity across formats is not established by broad compatibility language. |
| Anthropic format | [Compatibility](https://api-docs.deepseek.com/guides/anthropic_api/): base `https://api.deepseek.com/anthropic`, tool schemas supported; disable_parallel_tool_use and thinking budget_tokens ignored, output_config only effort. Claude names map to DeepSeek models, not Claude execution. No need to import this compatibility layer or LifeOS to call DeepSeek. |

Proposed starting route is direct Chat Completions/non-thinking, narrow function
requests with local schema/domain validation. Beta strict mode is an evaluation
option, not a safety dependency; Responses json_schema is another explicit option,
not silently interchangeable. No SDK or endpoint has been selected/installed.
Only Longnest's allowlisted functions may be executed: no shell, file, browser,
search, SQL, MCP or provider-hosted action surface. Endpoint/model differences
must be measured on the exact selected route, including changed aliases.

## Privacy and applicable terms — not a no-training guarantee

The [Open Platform API terms](https://cdn.deepseek.com/policies/en-US/deepseek-open-platform-terms-of-service.html)
(effective April 29, 2026), introduction and §5.5, incorporate broader terms, link
developer/data-subject processing to the privacy policy and distinguish downstream
end-user processing, for which developers must disclose their own policy.
[General terms](https://cdn.deepseek.com/policies/en-US/deepseek-terms-of-use.html)
§1.1 includes APIs; §4.3 permits limited de-identified input/output improvement use
and names an opt-out setting. Its effectiveness for API payloads is unverified.

The referenced [Privacy Policy](https://cdn.deepseek.com/policies/en-US/deepseek-privacy-policy.html)
(Feb 10, 2026) includes training/improvement uses, variable retention (including
account-lifetime input examples), and PRC processing/storage; it expressly excludes
downstream-user processing rules. Do not turn these into blanket API guarantees
or assume paid API means no training/retention. Applicability where this personal
user is also the API developer needs clarification; no API-specific zero-retention,
no-training or selectable-region commitment was established in the reviewed terms.

The [Responses guide](https://api-docs.deepseek.com/guides/responses_api/) describes
stateless conversation handling and fixed store:false; this is not proof of no
operational logging, caching, training or other retention. [Context caching](https://api-docs.deepseek.com/guides/kv_cache/)
is enabled by default on disk, normally clears unused cache within hours to days;
that is not a contractual deletion deadline for all API data.

Before real data: obtain applicable API processing/training/opt-out terms, content/
abuse-log/cache retention, deletion/backups, subprocessors and actual region, then
explicit Human acceptance of egress. Minimize input to the current utterance,
host business reference and necessary selected record fields; no whole ledger,
Telegram identifiers, credentials or long-lived personal memory. Aliasing IDs or
redacting names does not make amounts/dates/notes nonpersonal. Real financial text
remains prohibited now, including tool results, traces and reasoning transcripts.

A third-party-hosted DeepSeek model is a separate processor/route: assess that
provider's terms, upstream forwarding, routing/fallbacks, region and exact model/
schema support. Neither direct DeepSeek terms nor an aggregator's generic privacy
setting automatically proves the other route's guarantees. No third-party route
has been selected or its policy verified. No provider was contacted.

## Risk controls — proposed, shared services stay authoritative

| Risk | Minimal mitigation for a future bounded adapter |
| --- | --- |
| Vietnamese money/date confusion | Exact VND normalization in shared Money/parser validation; retain bounded raw amount/date evidence for cross-check, reject unsupported separators/units/conflicting interpretations. Supply Asia/Ho_Chi_Minh and one stable host reference; yesterday near midnight cannot use provider/server date. Ask for missing facts. Schema-valid wrong magnitudes are still wrong; finite evaluation cannot prove general language safety. |
| Hallucinated or injected actions | Treat user notes/tool text as untrusted data; reject unknown fields/functions. Bind requested action to actual user intent and selected owned target, not model confidence. No executable text, guessed IDs, prompt-only permissions or store access. A model can interpret/request, not authorize. |
| Destructive correction/deletion | Preserve revision checks and real-user target/action-bound delete confirmation; no confirmDelete model tool. Selected PE1 makes update requests preview-only and actual host Save commits the immutable action. Unresolved mutation ambiguity never executes; clear approved creation is not given a new blanket gate. |
| Retry/duplicate writes | Host-owned stable operation identity across provider retries/calls, atomic service mutation/result; do not use model tool_call_id as ledger authority. Reject changed payloads or extra mutation batches; recover committed result rather than rerun interpretation into a fresh purchase. Two distinct user messages are not proven duplicates. |
| Outage/latency | [Errors](https://api-docs.deepseek.com/quick_start/error_codes/) include auth/balance/rate/server failures. [Rate guide](https://api-docs.deepseek.com/quick_start/rate_limit/) allows long keep-alives. Propose a 10-second overall interpretation deadline, at most one transient retry within it; no auth/balance retry loop or automatic provider switch. Unsupported text stays unsaved with a supported-input example; rules and explicit controls keep working. A later provider failure cannot undo/misreport a committed save. |
| Key/egress exposure | Server-only injected key, fixed allowlisted HTTPS destination, no browser/chat/prompt key or debug-body logs; redact operational metadata and rotate on exposure. Never copy LifeOS environment/CLI settings. Actual custody/rotation requires later authorization. |
| Tokens/tool loops/cost | [Pricing](https://api-docs.deepseek.com/quick_start/pricing/) charges input/output with model/cache/peak differences. Bound context, tokens, tool count, wall time and spend locally; reject surplus calls before any mutation. Prefer one interpretation then deterministic service receipt, no second model call to narrate “saved”. Budget, totals and allocation arithmetic remain deterministic services, not token-consuming model math. |

These controls are design, not tested defenses or a guarantee against prompt
injection. Existing [Services](../SERVICES.md) remains canonical; the
[adapter slice](../features/adapters/README.md#a1--future-llm-interpretation-through-accepted-capabilities)
owns the minimal OOP route and synthetic evaluation proposal. No new architecture,
current-release requirement or financial action is authorized by this assessment.
