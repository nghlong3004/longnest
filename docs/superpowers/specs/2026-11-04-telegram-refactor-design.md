# Telegram interaction refactor

Approved scope: the user's “làm đi em” accepts the preceding code review and its State, Adapter and Command/Router proposal.

Keep Java 25, Boot 4.1.1, existing dependencies, JPA/Flyway and ledger contracts. Preserve authorization, source replay receipts, financial validation, optimistic versions, creation drafts and commit-before-delivery behavior. No migration, real Telegram/provider calls or dev startup. The user subsequently authorized committing and normally pushing the verified work.

Split the current coordinator into a typed edit-session model, bounded chat registry, Telegram sender adapter, keyboard renderer and callback handlers/router. Plain-text action presentation belongs in presentation, with typed TransactionField values shared with the mutation service; domain parsing and constraints remain in that service. Keep the existing formatter and pure display-name utility.

Menu/field transitions activate only after successful delivery. A fresh field input retires an earlier preview before parsing/delivery. Partial preview delivery never enables Save. Preserve explicit committed-ledger versus failed-display outcomes.

Use bounded process-local state: at most 128 chats, 20 active query pages and 200 historical card references per chat. Query views expire after one hour; edit sessions after 15 minutes. Unknown/evicted/expired pagination is rejected and requests a fresh query; no unbounded revoked-token/tombstone sets. Eviction affects UI state only, never financial rows or durable replay receipts. Refresh retires active views once and retains bounded old card references for later deletion. Per-chat striped locking replaces holding the singleton monitor across I/O; no new background execution.

Approaches: moving methods alone leaves lifecycle faults intact; a general event bus or one Strategy class per field adds unnecessary machinery. Focused collaborators with explicit transitions and a small callback-handler interface address the actual problems.
