package me.nghlong3004.longnest.integration.telegram;

import java.time.Clock;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.nghlong3004.longnest.mapper.TransactionMapper;
import me.nghlong3004.longnest.model.dto.ChatMessage;
import me.nghlong3004.longnest.model.dto.ChatReply;
import me.nghlong3004.longnest.model.dto.TransactionData;
import me.nghlong3004.longnest.model.dto.TransactionFilter;
import me.nghlong3004.longnest.model.dto.TransactionQuery;
import me.nghlong3004.longnest.model.enums.Channel;
import me.nghlong3004.longnest.model.enums.QueryPeriod;
import me.nghlong3004.longnest.presentation.TransactionActionPresentation;
import me.nghlong3004.longnest.presentation.TransactionMessageFormatter;
import me.nghlong3004.longnest.service.TransactionMutationService;
import me.nghlong3004.longnest.service.TransactionQueryService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

/** Coordinates authenticated edit commands; the services own ledger authorization and commits. */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "longnest.telegram", name = "enabled", havingValue = "true")
public class TelegramTransactionActions implements TelegramCallbackHandler {
  public static final String PREFIX = "tx:";
  private static final Duration EDIT_TTL = Duration.ofMinutes(15);
  private final TelegramMessageSender sender;
  private final TransactionMutationService mutations;
  private final TransactionQueryService queries;
  private final TransactionMessageFormatter formatter;
  private final TelegramTransactionQueryCodec queryCodec;
  private final Clock ledgerClock;
  private final TransactionMapper mapper;
  private final TransactionActionPresentation presentation;
  private final TelegramTransactionRegistry registry;
  private final TelegramTransactionKeyboard keyboard;

  @Override
  public boolean supports(String data) {
    return data != null && data.startsWith(PREFIX);
  }

  public Message sendPage(String chat, int original, ChatReply reply) throws TelegramApiException {
    synchronized (registry.lock(chat)) {
      var page = Objects.requireNonNull(reply.transactions());
      var summary = sender.send(chat, original, reply.text(), null);
      var view =
          registry.openView(
              chat, original, summary == null ? 0 : summary.getMessageId(), page.filter());
      for (int index = 0; index < page.items().size(); index++) {
        var item = page.items().get(index);
        var token = TelegramTransactionRegistry.token();
        var next =
            index == page.items().size() - 1 && page.nextCursor() != null
                ? queryCodec.encode(page, original)
                : null;
        var sent = sender.send(chat, original, formatter.entry(item), keyboard.card(token, next));
        if (sent != null && sent.getMessageId() != null) {
          registry.recordCard(view, token, item.id(), sent.getMessageId(), next);
        }
      }
      return summary;
    }
  }

  public boolean isRevokedPage(String chat, String data, int messageId) {
    synchronized (registry.lock(chat)) {
      return !registry.activePage(chat, data, messageId);
    }
  }

  @Override
  public void handleCallback(CallbackQuery callback, Message message) {
    if (!supports(callback.getData())) return;
    if (!trusted(message, callback.getFrom() == null ? null : callback.getFrom().getId())) {
      sender.acknowledge(callback.getId(), TransactionActionPresentation.EXPIRED);
      return;
    }
    synchronized (registry.lock(message.getChatId().toString())) {
      try {
        var command = TelegramTransactionCommand.decode(callback.getData());
        var session = registry.find(message.getChatId().toString(), command.key());
        if (session == null
            || session.anchor() != message.getMessageId()
            || session.expired(ledgerClock.instant())) {
          throw new IllegalArgumentException("Expired edit");
        }
        if (command.operation() == TelegramTransactionCommand.Operation.CANCEL
            && session.phase() != TelegramEditSession.Phase.CARD) {
          registry.remove(session.chat(), command.key());
          sender.edit(
              session.chat(),
              session.anchor(),
              TransactionActionPresentation.CANCELLED,
              keyboard.empty());
        } else {
          execute(command, session);
        }
        sender.acknowledge(callback.getId(), null);
      } catch (IllegalArgumentException exception) {
        sender.acknowledge(callback.getId(), TransactionActionPresentation.EXPIRED);
      } catch (TelegramApiException exception) {
        sender.acknowledge(callback.getId(), TransactionActionPresentation.DELIVERY_FAILED);
      } catch (RuntimeException exception) {
        log.warn("Transaction action could not be completed before commit");
        sender.acknowledge(callback.getId(), TransactionActionPresentation.MUTATION_UNCERTAIN);
      }
    }
  }

  private void execute(TelegramTransactionCommand command, TelegramEditSession session)
      throws TelegramApiException {
    var source = source(session.chat(), session.anchor(), "Thao tác khoản");
    var current = mutations.load(source, session.id());
    if (session.before() != null && current.version() != session.before().version()) {
      throw new IllegalArgumentException("Changed transaction");
    }
    switch (session.phase()) {
      case CARD -> {
        require(command, TelegramTransactionCommand.Operation.MENU);
        var next = TelegramTransactionRegistry.token();
        var prompt =
            sender.send(
                session.chat(),
                session.anchor(),
                presentation.menu(current.transaction()),
                keyboard.menu(next));
        if (prompt == null || prompt.getMessageId() == null)
          throw new TelegramApiException("Missing action message ID");
        registry.begin(
            next,
            session.openMenu(current, prompt.getMessageId(), ledgerClock.instant().plus(EDIT_TTL)));
      }
      case MENU -> {
        if (command.operation() == TelegramTransactionCommand.Operation.EDIT) {
          deliverTransition(
              command.key(),
              session.chooseFields(),
              TransactionActionPresentation.CHOOSE_FIELD,
              keyboard::fields);
        } else {
          require(command, TelegramTransactionCommand.Operation.DELETE);
          deliverTransition(
              command.key(),
              session.confirmDeletion(),
              presentation.deletionConfirmation(current.transaction()),
              keyboard::delete);
        }
      }
      case FIELD -> {
        require(command, TelegramTransactionCommand.Operation.FIELD);
        deliverTransition(
            command.key(),
            session.requestInput(command.field()),
            presentation.fieldPrompt(command.field()),
            keyboard::cancel);
      }
      case PREVIEW -> {
        require(command, TelegramTransactionCommand.Operation.SAVE);
        registry.remove(session.chat(), command.key());
        mutations.save(source, session.id(), session.before().version(), session.proposed());
        afterCommit(session, false);
      }
      case DELETE -> {
        require(command, TelegramTransactionCommand.Operation.CONFIRM_DELETE);
        registry.remove(session.chat(), command.key());
        mutations.delete(source, session.id(), session.before().version());
        afterCommit(session, true);
      }
      case INPUT -> throw new IllegalArgumentException("Input required");
    }
  }

  private void deliverTransition(
      String key,
      TelegramEditSession next,
      String text,
      Function<String, InlineKeyboardMarkup> controls)
      throws TelegramApiException {
    var token = TelegramTransactionRegistry.token();
    if (!sender.edit(next.chat(), next.anchor(), text, controls.apply(token))) {
      throw new TelegramApiException("Action delivery failed");
    }
    registry.replace(key, token, next);
  }

  public boolean handleText(Message message) {
    if (!trusted(message, message.getFrom() == null ? null : message.getFrom().getId()))
      return false;
    var chat = message.getChatId().toString();
    synchronized (registry.lock(chat)) {
      var input = registry.input(chat);
      if (input == null) return false;
      var key = input.key();
      var session = input.session();
      try {
        if (!mutations.consumeInput(source(chat, message.getMessageId(), message.getText())))
          return true;
      } catch (RuntimeException exception) {
        // An uncertain receipt cannot preserve authority to save a superseded preview.
        if (session.phase() == TelegramEditSession.Phase.PREVIEW) {
          var next = TelegramTransactionRegistry.token();
          session = session.retryInput();
          registry.replace(key, next, session);
          key = next;
        }
        sender.edit(
            chat,
            session.anchor(),
            TransactionActionPresentation.INPUT_FAILED,
            keyboard.cancel(key));
        return true;
      }
      if (Set.of("/cancel", "hủy", "huỷ")
          .contains(message.getText().strip().toLowerCase(Locale.ROOT))) {
        registry.remove(chat, key);
        sender.edit(
            chat, session.anchor(), TransactionActionPresentation.EDIT_CANCELLED, keyboard.empty());
        return true;
      }
      if (session.expired(ledgerClock.instant())) {
        registry.remove(chat, key);
        sender.edit(
            chat, session.anchor(), TransactionActionPresentation.EXPIRED, keyboard.empty());
        return true;
      }
      if (session.phase() != TelegramEditSession.Phase.INPUT
          && session.phase() != TelegramEditSession.Phase.PREVIEW) {
        sender.edit(
            chat,
            session.anchor(),
            TransactionActionPresentation.SELECT_FIELD_FIRST,
            keyboard.cancel(key));
        return true;
      }
      // New input always retires an earlier Save, including invalid replacements and failed
      // delivery.
      var retry = session.retryInput();
      var next = TelegramTransactionRegistry.token();
      registry.replace(key, next, retry);
      try {
        var current =
            mutations.load(source(chat, message.getMessageId(), message.getText()), session.id());
        if (current.version() != session.before().version())
          throw new IllegalArgumentException("Changed transaction");
        var proposed = mutations.preview(session.before(), session.field(), message.getText());
        var after = mapper.toResponse(proposed, session.id());
        showPreview(
            next,
            retry,
            proposed,
            presentation.previewText(
                session.id(), session.before().transaction(), session.field(), after));
      } catch (IllegalArgumentException exception) {
        sender.edit(
            chat,
            session.anchor(),
            presentation.invalidInput(session.field()),
            keyboard.cancel(next));
      } catch (RuntimeException exception) {
        sender.edit(
            chat,
            session.anchor(),
            TransactionActionPresentation.READ_FAILED,
            keyboard.cancel(next));
      }
      return true;
    }
  }

  private void showPreview(
      String key, TelegramEditSession session, TransactionData proposed, String text) {
    var controls = keyboard.save(key);
    int anchor = session.anchor();
    try {
      var parts = TelegramMessageSender.split(text);
      if (parts.size() == 1) {
        if (!sender.edit(session.chat(), anchor, text, controls))
          throw new TelegramApiException("Preview delivery failed");
      } else {
        sender.edit(
            session.chat(), anchor, TransactionActionPresentation.PREVIEW_PARTS, keyboard.empty());
        for (int i = 0; i < parts.size(); i++) {
          var sent =
              sender.send(
                  session.chat(),
                  session.anchor(),
                  parts.get(i),
                  i == parts.size() - 1 ? controls : null);
          if (sent == null || sent.getMessageId() == null)
            throw new TelegramApiException("Missing preview message ID");
          anchor = sent.getMessageId();
        }
      }
      registry.replace(key, key, session.preview(proposed, anchor));
    } catch (TelegramApiException | RuntimeException exception) {
      sender.edit(
          session.chat(),
          session.anchor(),
          TransactionActionPresentation.PREVIEW_FAILED,
          keyboard.cancel(key));
    }
  }

  private void afterCommit(TelegramEditSession session, boolean deleted) {
    boolean refreshed = false;
    try {
      refreshed = refresh(session, deleted);
    } catch (RuntimeException exception) {
      log.warn("Ledger committed; visible query refresh is incomplete");
    }
    sender.edit(
        session.chat(),
        session.anchor(),
        presentation.committed(deleted, refreshed),
        keyboard.empty());
  }

  private record RefreshKey(int original, TransactionFilter filter) {}

  private boolean refresh(TelegramEditSession session, boolean deleted) {
    boolean complete = true;
    var views = registry.retireViews(session.chat());
    if (deleted) {
      for (var card : registry.cards(session.chat(), session.id())) {
        if (!sender.delete(session.chat(), card.messageId())) {
          complete = false;
          sender.edit(
              session.chat(),
              card.messageId(),
              TransactionActionPresentation.DELETED_CARD,
              keyboard.empty());
        }
      }
      registry.forgetCards(session.chat(), session.id());
    }
    var filters = new LinkedHashMap<RefreshKey, TelegramTransactionRegistry.View>();
    for (var view : views) {
      filters.put(new RefreshKey(view.original(), view.filter()), view);
      complete &=
          sender.edit(
              view.chat(),
              view.summary(),
              TransactionActionPresentation.STALE_SUMMARY,
              keyboard.empty());
      for (var card : view.cards()) {
        if (!deleted || !card.id().equals(session.id())) {
          complete &=
              sender.edit(
                  view.chat(),
                  card.messageId(),
                  TransactionActionPresentation.STALE_CARD,
                  keyboard.empty());
        }
      }
    }
    for (var view : filters.values()) {
      try {
        var filter = view.filter();
        var page =
            queries.view(
                source(view.chat(), view.original(), "Cập nhật danh sách"),
                new TransactionQuery(filter.type(), QueryPeriod.CUSTOM, filter.from(), filter.to()),
                null);
        sendPage(view.chat(), view.original(), new ChatReply(formatter.summary(page), null, page));
      } catch (TelegramApiException | RuntimeException exception) {
        complete = false;
        log.warn("Ledger committed; refreshed Telegram query could not be delivered");
      }
    }
    return complete;
  }

  private void require(
      TelegramTransactionCommand command, TelegramTransactionCommand.Operation operation) {
    if (command.operation() != operation)
      throw new IllegalArgumentException("Unexpected transaction command");
  }

  private boolean trusted(Message message, Long senderId) {
    return message != null
        && message.getChat() != null
        && message.isUserMessage()
        && senderId != null
        && senderId.equals(message.getChatId());
  }

  private ChatMessage source(String chat, int message, String text) {
    return new ChatMessage(
        Channel.TELEGRAM, chat, chat, Integer.toString(message), ledgerClock.instant(), text);
  }
}
