# Persistent clarification flow

**Goal:** Continue ASK_CLARIFICATION across multiple messages and restarts, then
commit one transaction or cancel the draft using Telegram's inline Hủy button.

**Scope agreed in chat:** One pending draft per owner/channel/chat. Plain text
continues that draft; native Reply can address an earlier linked question.
Unrelated chat keeps the draft. `/cancel` or the button cancels without deleting
the DB row. The button deletes its bot question. The later UX update sends one
confirmation replying to the original user message after the first cancellation;
repeated clicks only show an already-cancelled callback toast.

**Implementation:**
- [x] Prove missing draft persistence with a failing PostgreSQL integration test.
- [x] Add V2 with original message timestamp, draft version and a small table of
  draft/message ID links. Links route old questions and deduplicate every round;
  they do not store full message history. Preserve applied V1.
- [x] Add a shared transactional identity resolver; reuse it in transaction and
  draft services. Keep the LLM outside database transactions.
- [x] Return `ChatReply` with text and optional pending draft ID; pass reply-to
  metadata from Telegram and persist sent question IDs after delivery.
- [x] Send original text, known draft fields and current question as LLM context.
  Merge only supplied fields; preserve original-day defaults and known values.
- [x] Check owner, channel, chat, pending status and version before mutation.
  Create the transaction and confirm its draft in one DB transaction.
- [x] Add Hủy inline callbacks: validate owner, cancel in DB, delete only the bot
  question for a cancelled draft, answer the callback and send one descriptive
  confirmation replying to the original user message on the first transition.
- [x] Test multiple rounds, replay/old questions, terminal drafts, ownership,
  rollback, stale context, date boundaries, callback/delete failure and typing.
- [x] Review, run all Maven tests and diff checks, update docs, restart dev bot.

No automatic commit. Confirmations use committed values. On invalid model output
or DB failure, keep the pending draft intact. Unknown explicit Reply targets do
not silently become new transactions. While a draft is pending, the prompt asks
the user to cancel before starting a clearly different transaction.

Verification: the final full `mvn -o -B -ntp test` run passed 110 tests with
no failures/errors/skips. This includes V1-to-V2 legacy-data backfill, multi-round
continuation, cancellation, original dates, ownership, stale/parallel output,
rollback, and receipts retained after either terminal state. Review findings were
reproduced by failing tests, fixed, and rechecked; the final atomic recording path
had no remaining Important findings. Changed Java files were formatted with
google-java-format 2.0.0. `git diff --check` passed. The development bot applied
V2 and started successfully; live Telegram interaction is ready for the owner.

Follow-up UX verification: 114 tests passed after adding native ReplyParameters
to all chat responses and a named cancellation confirmation. Cancellation replay
does not send a second confirmation; long Unicode descriptions remain intact in
DB while the notification fits one Telegram message. Research findings about
client-controlled Thanos Snap are recorded in docs/telegram-chatbot.md.
