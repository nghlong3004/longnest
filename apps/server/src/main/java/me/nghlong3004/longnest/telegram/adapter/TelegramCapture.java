package me.nghlong3004.longnest.telegram.adapter;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.LinkedHashSet;
import me.nghlong3004.longnest.domain.DateRange;
import me.nghlong3004.longnest.domain.RangeFilter;
import me.nghlong3004.longnest.telegram.model.TelegramNavigation.RangeSelection;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import me.nghlong3004.longnest.conversation.ConversationAssistant;
import me.nghlong3004.longnest.input.InterpretationResult;
import me.nghlong3004.longnest.input.InputInterpreter;
import me.nghlong3004.longnest.ledger.adapter.LedgerClient;
import me.nghlong3004.longnest.ledger.dto.LedgerResult;
import me.nghlong3004.longnest.ledger.dto.ConfirmationRequired;
import me.nghlong3004.longnest.ledger.dto.ControlBinding;
import me.nghlong3004.longnest.ledger.dto.FieldPrompt;
import me.nghlong3004.longnest.ledger.dto.MutationReceipt;
import me.nghlong3004.longnest.ledger.dto.TransactionPage;
import me.nghlong3004.longnest.ledger.dto.TransactionView;
import me.nghlong3004.longnest.ledger.dto.RangeSummary;
import me.nghlong3004.longnest.ledger.mapper.TransactionMapper;
import me.nghlong3004.longnest.ledger.model.HostContext;
import me.nghlong3004.longnest.ledger.model.ControlProofs;
import me.nghlong3004.longnest.ledger.model.EditField;
import me.nghlong3004.longnest.ledger.model.LedgerException.Code;
import me.nghlong3004.longnest.ledger.model.PersonalOwner;
import me.nghlong3004.longnest.telegram.mapper.TelegramReplies;
import me.nghlong3004.longnest.telegram.model.TelegramFailure;
import me.nghlong3004.longnest.telegram.model.TelegramNavigation;
import me.nghlong3004.longnest.telegram.model.TelegramNavigation.Route;
import me.nghlong3004.longnest.ledger.repository.LedgerStore.ControlKind;
import me.nghlong3004.longnest.telegram.model.TelegramSettings;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.chat.Chat;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

/** Sequential trusted-host adapter. Acknowledge only terminal handling plus successful reply. */
@RequiredArgsConstructor
public class TelegramCapture {
  private final TelegramClient client;
  private final TelegramSettings settings;
  private final PersonalOwner owner;
  private final InputInterpreter interpreter;
  private final TransactionMapper mapper;
  private final LedgerClient ledger;
  private final TelegramReplies replies;
  private final Clock clock;
  private final TelegramNavigation navigation;
  private final ControlProofs proofs;
  @Setter private ConversationAssistant conversation;
  private final Set<Integer> conversationalReplies = new LinkedHashSet<>();
  private String warnedOperation;

  public boolean handle(Update update) {
    if (update != null && update.getCallbackQuery() != null) {
      return callback(update, update.getCallbackQuery());
    }
    var message = update == null ? null : update.getMessage();
    // Never trust usernames, forwarded actor text, group chat identity or a bot sender.
    if (message == null || !authorized(message.getFrom(), message.getChat())) {
      return true;
    }
    if (update.getUpdateId() == null || update.getUpdateId() < 0
        || message.getMessageId() == null || message.getMessageId() <= 0) {
      throw new TelegramFailure(false, 3);
    }
    Instant sentAt = message.getDate() == null || message.getDate() <= 0
        ? null : Instant.ofEpochSecond(message.getDate());
    var reference = sentAt == null ? clock.instant() : sentAt;
    var context = new HostContext(owner.principal(), settings.getNamespace() + update.getUpdateId(),
        reference, sentAt, scope());
    String text = message.getText();
    if (text == null || text.equals("/start") || text.equals("/help")) {
      send(message.getChat().getId(), replies.help());
      return true;
    }
    if (text.startsWith("/show ")) {
      var result = ledger.get(context, text.substring(6).trim());
      return respondEntry(context, result);
    }
    if (text.startsWith("/delete ")) {
      String id = text.substring(8).trim();
      return sendPreview(context, ledger.requestDeleteCurrent(context, id));
    }
    String[] query = text.trim().split("\\s+");
    if (Set.of("/overview", "/income", "/expenses", "/transactions").contains(query[0])) {
      if (query.length != 3) {
        send(settings.getUser(), replies.invalidRange());
        return true;
      }
      DateRange range;
      try {
        range = DateRange.parse(query[1], query[2]);
      } catch (IllegalArgumentException invalid) {
        send(settings.getUser(), replies.invalidRange());
        return true;
      }
      RangeFilter filter = switch (query[0]) {
        case "/income" -> RangeFilter.INCOME;
        case "/expenses" -> RangeFilter.EXPENSE;
        default -> RangeFilter.ALL;
      };
      if (query[0].equals("/overview")) {
        return respond(context, ledger.aggregate(context, range, filter), replies::summary);
      }
      return browse(context, Route.first(new RangeSelection(range, filter)));
    }
    if (text.equals("/recent") || text.startsWith("/recent ")) {
      Optional<String> cursor = text.equals("/recent") ? Optional.empty()
          : Optional.of(text.substring(8).trim());
      return browse(context, new Route(cursor, List.of(), Optional.empty()));
    }
    if (text.equals("/reset") && conversation != null) {
      conversation.reset(context);
      conversationalReplies.clear();
      send(settings.getUser(), replies.conversation("conversation.reset"));
      return true;
    }
    if (text.startsWith("/")) {
      send(message.getChat().getId(), replies.help());
      return true;
    }
    if (message.getReplyToMessage() != null && !(conversation != null
        && (conversationalReplies.contains(message.getReplyToMessage().getMessageId())
            || navigation.knownMessage(settings.getUser(),
                message.getReplyToMessage().getMessageId())))) {
      var replied = message.getReplyToMessage();
      if (replied.getMessageId() == null || replied.getMessageId() <= 0) {
        send(settings.getUser(), replies.stalePrompt());
        return true;
      }
      var preview = ledger.previewFieldReply(context, replied.getMessageId(), text);
      if (preview instanceof LedgerResult.Failure<ConfirmationRequired> failure
          && !failure.retryable()) {
        send(settings.getUser(), failure.code() == Code.ACTION_EXPIRED
            || failure.code() == Code.ACTION_COMPLETED ? replies.stalePrompt()
            : replies.invalidPrompt(failure));
        return true;
      }
      return sendPreview(context, preview);
    }
    var interpreted = interpreter.interpret(text);
    if (conversation != null && interpreted instanceof InterpretationResult.Problem problem
        && problem.status() != InterpretationResult.Status.REJECTED
        && problem.code() != InterpretationResult.Code.INVALID_AMOUNT
        && problem.code() != InterpretationResult.Code.AMBIGUOUS_AMOUNT
        && problem.code() != InterpretationResult.Code.CONFLICTING_TYPE) {
      return conversationReply(context, conversation.reply(context, text), message.getFrom());
    }
    if (interpreted instanceof InterpretationResult.Problem problem) {
      send(message.getChat().getId(), replies.problem(problem));
      return true;
    }
    var draft = ((InterpretationResult.Ready) interpreted).draft();
    return respondReceipt(context, ledger.create(context, mapper.input(draft)));
  }

  private boolean authorized(User user, Chat chat) {
    return user != null && chat != null && Boolean.FALSE.equals(user.getIsBot())
        && Long.valueOf(settings.getUser()).equals(user.getId())
        && Long.valueOf(settings.getUser()).equals(chat.getId())
        && "private".equals(chat.getType());
  }

  private boolean callback(Update update, CallbackQuery callback) {
    if (!(callback.getMessage() instanceof Message message)
        || !authorized(callback.getFrom(), message.getChat())) return true;
    if (callback.getId() == null || callback.getId().isBlank()
        || update.getUpdateId() == null || update.getUpdateId() < 0
        || message.getMessageId() == null || message.getMessageId() <= 0) return true;
    try {
      // A late callback answer must not strand the SDK batch; browse results are independent reads.
      client.execute(new AnswerCallbackQuery(callback.getId()));
    } catch (TelegramApiException unavailable) {
      // Old query IDs may be unanswerable; never log SDK text or retry a mutation here.
    }
    if (callback.getData() == null) return true;
    var context = new HostContext(owner.principal(),
        settings.getNamespace() + update.getUpdateId(), clock.instant(), null, scope());
    if (callback.getData().startsWith("a:") || callback.getData().startsWith("p:")) {
      return actionCallback(context, callback, message);
    }
    if (callback.getData().startsWith("v:")) {
      return entryCallback(context, callback.getData(), message);
    }
    if (!callback.getData().startsWith("b:") && !callback.getData().startsWith("r:")) return true;
    Route route;
    try {
      var resolved = navigation.resolve(callback.getData(), settings.getUser(), message.getMessageId());
      if (resolved.isEmpty() && callback.getData().startsWith("r:")) {
        send(settings.getUser(), replies.invalidRange());
        return true;
      }
      route = resolved.orElseGet(Route::first);
    } catch (SecurityException copiedControl) {
      return true;
    }
    if (route.entryId().isPresent()) {
      return respondEntry(context, ledger.get(context, route.entryId().orElseThrow()));
    }
    return browse(context, route);
  }

  private String scope() {
    return settings.getNamespace() + settings.getUser();
  }

  private boolean respondEntry(HostContext context, LedgerResult<TransactionView> result) {
    if (result instanceof LedgerResult.Ok<TransactionView> ok) {
      TransactionView entry = ok.data();
      if (conversation != null) conversation.selected(context, entry);
      if (entry.state() == TransactionView.State.DELETED) {
        return sendRestoreEntry(context, entry, replies.view(entry));
      }
      String token = navigation.openEntry(settings.getUser(), entry);
      Message sent = send(settings.getUser(), replies.view(entry),
          replies.entryKeyboard(entry, token));
      navigation.bind(token, sent.getMessageId());
      warnedOperation = null;
      return true;
    }
    return respond(context, result, replies::view);
  }

  private boolean respondReceipt(HostContext context, LedgerResult<MutationReceipt> result) {
    if (result instanceof LedgerResult.Ok<MutationReceipt> ok) {
      var receipt = ok.data();
      if (!receipt.replayed()) {
        if (conversation != null) conversation.selected(context, receipt.transaction());
        if (receipt.transaction().state() == TransactionView.State.DELETED) {
          return sendRestoreEntry(context, receipt.transaction(), replies.receipt(receipt));
        }
        String token = navigation.openEntry(settings.getUser(), receipt.transaction());
        Message sent = send(settings.getUser(), replies.receipt(receipt),
            replies.entryKeyboard(receipt.transaction(), token));
        navigation.bind(token, sent.getMessageId());
      } else {
        send(settings.getUser(), replies.receipt(receipt));
      }
      warnedOperation = null;
      return true;
    }
    return respond(context, result, replies::receipt);
  }

  private boolean entryCallback(HostContext context, String data, Message message) {
    TelegramNavigation.EntryControl target;
    try {
      target = navigation.resolveEntry(data, settings.getUser(), message.getMessageId())
          .orElse(null);
    } catch (SecurityException copied) {
      return true;
    }
    if (target == null) {
      send(settings.getUser(), replies.stalePrompt());
      return true;
    }
    String command = target.command();
    if (command.equals("e")) {
      var found = ledger.get(context, target.id());
      if (found instanceof LedgerResult.Ok<TransactionView> ok
          && ok.data().revision().equals(target.revision())
          && ok.data().state() == TransactionView.State.ACTIVE) {
        String token = navigation.openEntry(settings.getUser(), ok.data());
        Message sent = send(settings.getUser(), replies.fieldMenu(ok.data()),
            replies.fieldKeyboard(ok.data(), token));
        navigation.bind(token, sent.getMessageId());
        return true;
      }
      send(settings.getUser(), replies.stalePrompt());
      return true;
    }
    if (command.startsWith("f") && command.length() == 2
        && Character.isDigit(command.charAt(1))) {
      int ordinal = command.charAt(1) - '0';
      if (ordinal >= EditField.values().length) return true;
      return sendPrompt(context, ledger.openFieldPrompt(context, target.id(), target.revision(),
          EditField.values()[ordinal], null));
    }
    if (command.equals("d")) {
      return sendPreview(context, ledger.requestDelete(context, target.id(), target.revision()));
    }
    return true;
  }

  private boolean actionCallback(HostContext context, CallbackQuery callback, Message message) {
    String[] parts = callback.getData().split(":", -1);
    if ((parts.length != 3 && !(parts.length == 4 && parts[0].equals("p")
        && parts[2].equals("v"))) || !parts[1].matches("[0-9a-f]{32}")) return true;
    String id = parts[1];
    var inspected = ledger.controlBinding(context, id);
    if (!(inspected instanceof LedgerResult.Ok<ControlBinding> ok)) {
      send(settings.getUser(), replies.stalePrompt());
      return true;
    }
    var binding = ok.data();
    if (binding.displayMessage() == null
        || binding.displayMessage() != message.getMessageId().longValue()
        || !((parts[0].equals("a") && binding.kind() != ControlKind.PROMPT)
            || (parts[0].equals("p") && binding.kind() == ControlKind.PROMPT))) return true;
    var proof = proofs.issue(scope(), message.getMessageId(), callback.getId(), id,
        binding.targetId(), binding.expectedRevision());
    String command = parts[2];
    if (command.equals("c")) {
      var canceled = ledger.cancel(context, id, proof);
      return respond(context, canceled, unused -> replies.canceled());
    }
    if (parts[0].equals("p") && command.equals("x")) {
      return sendPreview(context, ledger.previewFieldReply(context, message.getMessageId(), ""));
    }
    if (parts[0].equals("p") && parts.length == 4 && command.equals("v")) {
      return sendPreview(context,
          ledger.previewFieldReply(context, message.getMessageId(), parts[3]));
    }
    if (parts[0].equals("a") && command.equals("s")) {
      LedgerResult<MutationReceipt> result = switch (binding.kind()) {
        case UPDATE -> ledger.update(context, id, proof);
        case DELETE -> ledger.confirmDelete(context, id, proof);
        case RESTORE -> new LedgerResult.Failure<>(Code.INVALID_ACTION_PROOF,
            Optional.empty(), false);
        case PROMPT -> throw new IllegalStateException("Unexpected prompt control");
      };
      return respondReceipt(context, result);
    }
    if (parts[0].equals("a") && command.equals("r")
        && binding.kind() == ControlKind.RESTORE) {
      return respondReceipt(context, ledger.restoreAction(context, id, proof));
    }
    if (parts[0].equals("a") && command.length() == 2 && command.charAt(0) == 'f'
        && Character.isDigit(command.charAt(1)) && binding.kind() == ControlKind.UPDATE) {
      int ordinal = command.charAt(1) - '0';
      if (ordinal >= EditField.values().length) return true;
      return sendPrompt(context, ledger.openFieldPrompt(context,
          Long.toString(binding.targetId()), Long.toString(binding.expectedRevision()),
          EditField.values()[ordinal], id));
    }
    return true;
  }

  private boolean sendPrompt(HostContext context, LedgerResult<FieldPrompt> result) {
    if (result instanceof LedgerResult.Ok<FieldPrompt> ok) {
      FieldPrompt prompt = ok.data();
      if (prompt.displayMessage() != null) return true;
      Message sent = send(settings.getUser(), replies.prompt(prompt),
          replies.promptKeyboard(prompt));
      var bound = ledger.bindPrompt(context, prompt.promptId(), sent.getMessageId());
      return respondBinding(context, bound);
    }
    return respond(context, result, replies::prompt);
  }

  private boolean sendPreview(HostContext context, LedgerResult<ConfirmationRequired> result) {
    if (result instanceof LedgerResult.Ok<ConfirmationRequired> ok) {
      var preview = ok.data();
      if (preview.displayMessage() != null) return true;
      Message sent = send(settings.getUser(), replies.preview(preview),
          replies.previewKeyboard(preview));
      var bound = ledger.bindAction(context, preview.actionId(), sent.getMessageId());
      return respondBinding(context, bound);
    }
    return respond(context, result, replies::preview);
  }

  private boolean sendRestoreEntry(HostContext context, TransactionView entry, String text) {
    // A display is a separate durable intent; its button ID, not callback delivery ID, replays.
    var display = new HostContext(context.principal(), "restore-view:" + context.operationId(),
        context.businessReference(), context.messageAt(), context.interactionScope());
    var result = ledger.openRestoreButton(display, entry.id(), entry.revision());
    if (result instanceof LedgerResult.Ok<ConfirmationRequired> ok) {
      ConfirmationRequired action = ok.data();
      if (action.displayMessage() != null) return true;
      Message sent = send(settings.getUser(), text, replies.restoreKeyboard(action.actionId()));
      return respondBinding(display, ledger.bindAction(display, action.actionId(), sent.getMessageId()));
    }
    return respond(display, result, replies::preview);
  }

  private boolean respondBinding(HostContext context, LedgerResult<Boolean> result) {
    if (result instanceof LedgerResult.Ok<Boolean>) return true;
    var failure = (LedgerResult.Failure<Boolean>) result;
    if (failure.retryable()) return false;
    send(settings.getUser(), replies.stalePrompt());
    return true;
  }

  private boolean browse(HostContext context, Route requested) {
    Route route = requested;
    LedgerResult<TransactionPage> result = queryPage(context, route);
    if (result instanceof LedgerResult.Failure<TransactionPage> failure
        && (failure.code() == Code.STALE_CURSOR || failure.code() == Code.INVALID_CURSOR)) {
      route = route.refresh();
      result = queryPage(context, route);
    }
    if (result instanceof LedgerResult.Ok<TransactionPage> ok) {
      return sendPage(ok.data(), route);
    }
    return respond(context, result, replies::page);
  }

  private LedgerResult<TransactionPage> queryPage(HostContext context, Route route) {
    return route.selection().map(selection -> ledger.list(context, selection.range(),
        selection.filter(), route.cursor())).orElseGet(() -> ledger.recent(context, route.cursor()));
  }

  private boolean sendPage(TransactionPage page, Route route) {
    String token = navigation.open(settings.getUser(), page, route);
    Message sent = send(settings.getUser(), replies.page(page),
        replies.keyboard(page, token, route.cursor().isPresent()));
    navigation.bind(token, sent.getMessageId());
    warnedOperation = null;
    return true;
  }

  private boolean conversationReply(HostContext context, ConversationAssistant.Reply reply,
      User presentation) {
    if (reply.messageKey() != null) {
      Message sent = send(settings.getUser(), replies.conversation(reply.messageKey(),
          presentation.getFirstName(), presentation.getLastName()), null);
      if (conversationalReplies.size() >= 100) {
        conversationalReplies.remove(conversationalReplies.iterator().next());
      }
      conversationalReplies.add(sent.getMessageId());
      return true;
    }
    if (reply.result() instanceof LedgerResult.Failure<?> failure) {
      return respond(context, failure, ignored -> "");
    }
    Object value = ((LedgerResult.Ok<?>) reply.result()).data();
    if (value instanceof MutationReceipt receipt) return respondReceipt(context, new LedgerResult.Ok<>(receipt));
    if (value instanceof ConfirmationRequired preview) return sendPreview(context, new LedgerResult.Ok<>(preview));
    if (value instanceof TransactionView view) return respondEntry(context, new LedgerResult.Ok<>(view));
    if (value instanceof TransactionPage page) {
      Route route = page.summary().map(summary -> Route.first(
          new RangeSelection(summary.range(), summary.filter()))).orElseGet(Route::first);
      return sendPage(page, route);
    }
    if (value instanceof RangeSummary summary) {
      send(settings.getUser(), replies.summary(summary));
      return true;
    }
    throw new IllegalStateException("Unknown authoritative conversation result");
  }

  /** Resume bounded frozen creates even when their Telegram update has expired. */
  public void recoverPending(BooleanSupplier active) {
    var host = new HostContext(owner.principal(), null, null, null);
    while (true) {
      requireActive(active);
      var pending = ledger.pendingCreates(host, settings.getNamespace());
      if (!(pending instanceof LedgerResult.Ok<List<String>> ready)) {
        throw new TelegramFailure(false, 5);
      }
      if (ready.data().isEmpty()) return;
      if (ready.data().size() > 10) throw new TelegramFailure(true, 3);
      for (String operation : ready.data()) {
        requireActive(active);
        if (operation == null || !operation.startsWith(settings.getNamespace())) {
          throw new TelegramFailure(true, 3);
        }
        var context = new HostContext(owner.principal(), operation, null, null);
        var result = ledger.recover(context);
        if (result instanceof LedgerResult.Failure<?> failure) {
          send(settings.getUser(), replies.recoveryFailure(failure));
          throw new TelegramFailure(!failure.retryable(), 5);
        }
        respond(context, result, replies::receipt);
      }
    }
  }

  private static void requireActive(BooleanSupplier active) {
    if (!active.getAsBoolean() || Thread.currentThread().isInterrupted()) {
      throw new TelegramFailure(false, 3);
    }
  }

  private <T> boolean respond(HostContext host, LedgerResult<T> result,
      Function<T, String> render) {
    if (result instanceof LedgerResult.Ok<T> ok) {
      send(settings.getUser(), render.apply(ok.data()));
      warnedOperation = null;
      return true;
    }
    var failure = (LedgerResult.Failure<T>) result;
    if (!failure.retryable() || !host.operationId().equals(warnedOperation)) {
      send(settings.getUser(), replies.failure(failure));
      warnedOperation = host.operationId();
    }
    return !failure.retryable();
  }

  private void send(long chat, String text) {
    send(chat, text, null);
  }

  private Message send(long chat, String text, InlineKeyboardMarkup keyboard) {
    if (text == null || text.isBlank() || text.length() > 4096) {
      throw new TelegramFailure(false, 3);
    }
    try {
      var sent = client.execute(SendMessage.builder().chatId(Long.toString(chat))
          .text(text).replyMarkup(keyboard).disableWebPagePreview(true).build());
      if (sent == null || sent.getMessageId() == null || sent.getMessageId() <= 0
          || sent.getChat() == null || !Long.valueOf(chat).equals(sent.getChat().getId())) {
        throw new TelegramFailure(false, 3);
      }
      return sent;
    } catch (TelegramApiException failure) {
      throw new TelegramFailure(false, 3);
    }
  }
}
