# Longnest agent entrypoint

Resolve the project path; first read the shared contract at
`/home/nghlong3004/projects/slp/config/workflow/WORKSPACE_PROTOCOL.md` and
`docs/WORKSPACE_PROTOCOL.md` if present. Then read [CURRENT STATUS](docs/WORK_STATUS.md),
[context tree](README.md#context-tree--start-here) and only the relevant branch.
Reconcile actual ownership/current handoff before affected writes or dispatch.

Project guidance is required under .agents, but that path is read-only (0555 plus
managed policy). For backend work, read
[longnest-backend](.agents/skills/longnest-backend/SKILL.md)
and follow its relevant branches. Do not bypass permissions or alter private
state. [Proposed routing and exact permission gap](docs/BACKEND_RULES.md#project-local-routing-proposal--blocked)
is a plan, not fulfillment; existing [backend conventions](docs/BACKEND_RULES.md)
remain in place pending authorized relocation. Selected branches must be read fully.

Current authority includes remaining first-release L1–L3 implementation and
synthetic/local build/smoke checks under named scopes in CURRENT STATUS. Missing
durable/runtime evidence is not waived by this code authority.
DS-FALLBACK-1 additionally authorizes default-off DeepSeek interpretation code and
fake-provider/local verification, not provider activation, credentials or egress.
No live messaging/data egress, paid/LLM calls, deployment, DB installation or broad
infrastructure/credential changes. Inspected project baseline/increment Git stage,
commit and push are authorized only with usable existing permissions and verified
identity, branch/remote/upstream and outbound content; no force push or protected
.git bypass. Project coordination/docs and code identifiers remain English;
end-user UI is Vietnamese through externalized message bundles. Vietnamese
examples/resource values are domain/UI content.
[Context maintenance](README.md#context-maintenance--pending-relocation) links the
retained rules; README is an index, not ownership authority. Submit a stable bounded
handoff and stop candidate writes until reassigned.
