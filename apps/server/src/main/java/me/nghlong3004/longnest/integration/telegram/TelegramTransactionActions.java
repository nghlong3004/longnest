package me.nghlong3004.longnest.integration.telegram;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.nghlong3004.longnest.mapper.TransactionMapper;
import me.nghlong3004.longnest.model.dto.ChatMessage;
import me.nghlong3004.longnest.model.dto.ChatReply;
import me.nghlong3004.longnest.model.dto.TransactionData;
import me.nghlong3004.longnest.model.dto.TransactionFilter;
import me.nghlong3004.longnest.model.dto.TransactionQuery;
import me.nghlong3004.longnest.model.dto.TransactionRevision;
import me.nghlong3004.longnest.model.enums.Channel;
import me.nghlong3004.longnest.model.enums.QueryPeriod;
import me.nghlong3004.longnest.service.TransactionMessageFormatter;
import me.nghlong3004.longnest.service.TransactionMutationService;
import me.nghlong3004.longnest.service.TransactionQueryService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.ReplyParameters;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

/** Transport-only edit state. It never uses creation drafts or model output. */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "longnest.telegram", name = "enabled", havingValue = "true")
public class TelegramTransactionActions {
  public static final String PREFIX = "tx:";
  private static final String EXPIRED =
      "Nút này đã hết hạn hoặc khoản đã thay đổi. Mở lại danh sách giúp em nhé.";
  private static final InlineKeyboardMarkup EMPTY = new InlineKeyboardMarkup(List.of());
  private static final Duration EDIT_TTL = Duration.ofMinutes(15);
  private static final int MAX_MESSAGE_LENGTH = 4096;
  private final TelegramClient client;
  private final TransactionMutationService mutations;
  private final TransactionQueryService queries;
  private final TransactionMessageFormatter formatter;
  private final TelegramTransactionQueryCodec queryCodec;
  private final Clock ledgerClock;
  private final TransactionMapper mapper;
  private final TelegramTransactionPresentation presentation;
  private final Map<String, Session> sessions = new HashMap<>();
  private final Map<String, String> inputs = new HashMap<>();
  private final List<View> views = new ArrayList<>();
  private final Set<String> revokedPages = new HashSet<>();
  private final Set<UUID> deletedIds = new HashSet<>();

  private enum Phase {
    CARD,
    MENU,
    FIELD,
    INPUT,
    PREVIEW,
    DELETE
  }

  private record Card(UUID id, int messageId) {}

  private record View(
      String chat,
      int original,
      int summary,
      TransactionFilter filter,
      List<Card> cards,
      Set<String> pages) {}

  private record Session(
      String chat,
      UUID id,
      int anchor,
      TransactionRevision before,
      String field,
      TransactionData proposed,
      Phase phase,
      Instant expires) {}

  public synchronized Message sendPage(String chat, int original, ChatReply reply)
      throws TelegramApiException {
    var page = Objects.requireNonNull(reply.transactions());
    var summary = send(chat, original, reply.text(), null);
    var cards = new ArrayList<Card>();
    var pages = new HashSet<String>();
    // Keep partially delivered views too, so later mutations invalidate their visible cards.
    views.add(
        new View(
            chat,
            original,
            summary == null ? 0 : summary.getMessageId(),
            page.filter(),
            cards,
            pages));
    for (int index = 0; index < page.items().size(); index++) {
      var item = page.items().get(index);
      var token = token();
      var rows = new ArrayList<InlineKeyboardRow>();
      if (index == page.items().size() - 1 && page.nextCursor() != null) {
        var next = queryCodec.encode(page, original);
        revokedPages.remove(chat + ":" + next);
        pages.add(next);
        rows.add(new InlineKeyboardRow(button("Xem thêm", next)));
      }
      rows.add(new InlineKeyboardRow(button("⋯", token + ":m")));
      var sent = send(chat, original, formatter.entry(item), new InlineKeyboardMarkup(rows));
      if (sent != null && sent.getMessageId() != null) {
        cards.add(new Card(item.id(), sent.getMessageId()));
        sessions.put(
            token,
            new Session(
                chat, item.id(), sent.getMessageId(), null, null, null, Phase.CARD, Instant.MAX));
      }
    }
    return summary;
  }

  public synchronized boolean isRevokedPage(String chat, String data) {
    return revokedPages.contains(chat + ":" + data);
  }

  public synchronized void handleCallback(CallbackQuery callback, Message message) {
    var data = callback.getData();
    if (data == null || !data.startsWith(PREFIX)) {
      return;
    }
    int separator = data.lastIndexOf(':');
    var key = data.substring(0, separator);
    var operation = data.substring(separator + 1);
    var session = sessions.get(key);
    if (!trusted(message, callback.getFrom() == null ? null : callback.getFrom().getId())
        || session == null
        || !session.chat().equals(message.getChatId().toString())
        || session.anchor() != message.getMessageId()
        || session.expires().isBefore(ledgerClock.instant())) {
      acknowledge(callback.getId(), EXPIRED);
      return;
    }
    if (operation.equals("c") && session.phase() != Phase.CARD) {
      sessions.remove(key);
      inputs.remove(session.chat());
      edit(
          session.chat(),
          session.anchor(),
          "Em đã hủy thao tác rồi nhé. Sổ và bản nháp tạo khoản không thay đổi.",
          EMPTY);
      acknowledge(callback.getId(), null);
      return;
    }
    try {
      var source = source(session.chat(), message.getMessageId(), "Thao tác khoản");
      var current = mutations.load(source, session.id());
      if (session.before() != null && current.version() != session.before().version()) {
        throw new IllegalArgumentException(EXPIRED);
      }
      switch (session.phase()) {
        case CARD -> openMenu(session, operation, current);
        case MENU -> chooseAction(key, session, operation, current);
        case FIELD -> chooseField(key, session, operation);
        case PREVIEW -> savePreview(key, session, operation, source);
        case DELETE -> confirmDelete(key, session, operation, source);
        default -> throw new IllegalArgumentException(EXPIRED);
      }
      acknowledge(callback.getId(), null);
    } catch (IllegalArgumentException exception) {
      acknowledge(callback.getId(), EXPIRED);
    } catch (TelegramApiException exception) {
      acknowledge(callback.getId(), "Em chưa gửi được phần thao tác. Mở lại ⋯ giúp em nhé.");
    } catch (RuntimeException exception) {
      log.warn("Transaction action could not be completed before commit");
      acknowledge(
          callback.getId(),
          "Em chưa xác nhận được thay đổi. Xem lại sổ trước khi thử lại giúp em nhé.");
    }
  }

  private void openMenu(Session session, String operation, TransactionRevision current)
      throws TelegramApiException {
    if (!operation.equals("m")) {
      throw new IllegalArgumentException(EXPIRED);
    }
    abandonEdit(session.chat());
    var next = token();
    var prompt =
        send(
            session.chat(),
            session.anchor(),
            formatter.entry(current.transaction()) + "\nChọn sửa hoặc xóa khoản này để em làm nhé:",
            keyboard(next, "Sửa", "e", "Xóa", "d", "Hủy", "c"));
    if (prompt != null) {
      sessions.put(
          next,
          new Session(
              session.chat(),
              session.id(),
              prompt.getMessageId(),
              current,
              null,
              null,
              Phase.MENU,
              ledgerClock.instant().plus(EDIT_TTL)));
      inputs.put(session.chat(), next);
    }
  }

  private void chooseAction(
      String key, Session session, String operation, TransactionRevision current) {
    if (operation.equals("e")) {
      var next = rotate(key, session, null, null, Phase.FIELD);
      edit(
          session.chat(),
          session.anchor(),
          "Chọn phần cần sửa giúp em nhé:",
          keyboard(
              next,
              "Số tiền",
              "amount",
              "Ngày",
              "date",
              "Loại",
              "type",
              "Danh mục",
              "category",
              "Nội dung",
              "description",
              "Địa điểm",
              "location",
              "Buổi",
              "timeOfDay",
              "Hủy",
              "c"));
    } else if (operation.equals("d")) {
      var next = rotate(key, session, null, null, Phase.DELETE);
      edit(
          session.chat(),
          session.anchor(),
          "Xác nhận để em xóa vĩnh viễn khoản này khỏi sổ? Không thể hoàn tác nhé.\nID: "
              + session.id()
              + "\n"
              + formatter.entry(current.transaction()),
          keyboard(next, "Xóa vĩnh viễn", "yes", "Hủy", "c"));
    } else {
      throw new IllegalArgumentException(EXPIRED);
    }
  }

  private void chooseField(String key, Session session, String operation) {
    var instruction = presentation.instruction(operation);
    var next = rotate(key, session, operation, null, Phase.INPUT);
    inputs.put(session.chat(), next);
    edit(
        session.chat(),
        session.anchor(),
        instruction + "\nGửi em giá trị mới ở tin tiếp theo nhé; /cancel chỉ hủy sửa khoản này.",
        keyboard(next, "Hủy", "c"));
  }

  private void savePreview(String key, Session session, String operation, ChatMessage source) {
    if (!operation.equals("save")) {
      throw new IllegalArgumentException(EXPIRED);
    }
    sessions.remove(key);
    inputs.remove(session.chat());
    mutations.save(source, session.id(), session.before().version(), session.proposed());
    // The transactional proxy has committed before any Telegram operation below.
    afterCommit(session, false);
  }

  private void confirmDelete(String key, Session session, String operation, ChatMessage source) {
    if (!operation.equals("yes")) {
      throw new IllegalArgumentException(EXPIRED);
    }
    sessions.remove(key);
    inputs.remove(session.chat());
    mutations.delete(source, session.id(), session.before().version());
    afterCommit(session, true);
  }

  public synchronized boolean handleText(Message message) {
    if (!trusted(message, message.getFrom() == null ? null : message.getFrom().getId())) {
      return false;
    }
    var chat = message.getChatId().toString();
    var key = inputs.get(chat);
    if (key == null) {
      return false;
    }
    var session = sessions.get(key);
    if (session == null) {
      inputs.remove(chat);
      return false;
    }
    try {
      if (!mutations.consumeInput(source(chat, message.getMessageId(), message.getText()))) {
        return true;
      }
    } catch (RuntimeException exception) {
      edit(
          chat,
          session.anchor(),
          "Em chưa nhận được tin sửa khoản. Sổ chưa thay đổi; thử lại hoặc bấm Hủy nhé.",
          keyboard(key, "Hủy", "c"));
      return true;
    }
    if (Set.of("/cancel", "hủy", "huỷ")
        .contains(message.getText().strip().toLowerCase(Locale.ROOT))) {
      sessions.remove(key);
      inputs.remove(chat);
      edit(
          chat,
          session.anchor(),
          "Em đã hủy sửa khoản rồi nhé. Sổ và bản nháp tạo khoản không thay đổi.",
          EMPTY);
      return true;
    }
    if (session.expires().isBefore(ledgerClock.instant())) {
      sessions.remove(key);
      inputs.remove(chat);
      edit(chat, session.anchor(), EXPIRED, EMPTY);
      return true;
    }
    if (session.phase() != Phase.INPUT && session.phase() != Phase.PREVIEW) {
      edit(
          chat,
          session.anchor(),
          "Chọn phần cần sửa bằng nút trước rồi gửi giá trị giúp em nhé, hoặc bấm Hủy.",
          keyboard(key, "Hủy", "c"));
      return true;
    }
    try {
      var current =
          mutations.load(source(chat, message.getMessageId(), message.getText()), session.id());
      if (current.version() != session.before().version()) {
        throw new IllegalArgumentException(EXPIRED);
      }
      var proposed = mutations.preview(session.before(), session.field(), message.getText());
      var after = mapper.toResponse(proposed, session.id());
      showPreview(
          key,
          session,
          proposed,
          presentation.previewText(
              session.id(), session.before().transaction(), session.field(), after));
    } catch (IllegalArgumentException exception) {
      edit(
          chat,
          session.anchor(),
          "Em chưa dùng được giá trị này, hoặc khoản đã thay đổi.\n"
              + presentation.instruction(session.field()),
          keyboard(key, "Hủy", "c"));
    } catch (RuntimeException exception) {
      edit(
          chat,
          session.anchor(),
          "Em chưa đọc được khoản này. Sổ chưa thay đổi; thử lại hoặc bấm Hủy nhé.",
          keyboard(key, "Hủy", "c"));
    }
    return true;
  }

  private void afterCommit(Session session, boolean deleted) {
    // Transport and refresh failures must never escape into the financial-failure branch.
    boolean refreshed = false;
    try {
      refreshed = refresh(session, deleted);
    } catch (RuntimeException exception) {
      log.warn("Ledger committed; visible query refresh is incomplete");
    }
    edit(
        session.chat(),
        session.anchor(),
        (deleted
                ? "Em đã xóa vĩnh viễn khoản này khỏi sổ rồi nhé."
                : "Em đã lưu thay đổi vào sổ rồi nhé.")
            + (refreshed ? "" : " Em chưa cập nhật hết tin hiển thị; xem lại sổ giúp em nhé."),
        EMPTY);
  }

  private boolean refresh(Session session, boolean deleted) {
    boolean complete = true;
    if (deleted) {
      deletedIds.add(session.id());
    }
    var affected = views.stream().filter(view -> view.chat().equals(session.chat())).toList();
    sessions.entrySet().removeIf(entry -> entry.getValue().chat().equals(session.chat()));
    inputs.remove(session.chat());
    var filters = new LinkedHashMap<String, View>();
    for (var view : affected) {
      view.pages().forEach(data -> revokedPages.add(view.chat() + ":" + data));
      filters.put(view.original() + ":" + view.filter(), view);
      complete &=
          edit(
              view.chat(),
              view.summary(),
              "Em đã cập nhật sổ. Xem danh sách mới bên dưới nhé; danh sách cũ không còn dùng"
                  + " được.",
              EMPTY);
      for (var card : view.cards()) {
        if (deletedIds.contains(card.id())) {
          boolean removed = false;
          try {
            removed =
                Boolean.TRUE.equals(
                    client.execute(new DeleteMessage(view.chat(), card.messageId())));
          } catch (TelegramApiException | RuntimeException exception) {
            log.warn("Ledger deleted; Telegram card removal refused");
          }
          if (!removed) {
            complete = false;
            edit(
                view.chat(),
                card.messageId(),
                "Em đã xóa vĩnh viễn khoản này khỏi sổ. Chưa gỡ được tin cũ này, nên em đã tắt các"
                    + " nút ở đây nhé.",
                EMPTY);
          }
        } else {
          complete &=
              edit(
                  view.chat(),
                  card.messageId(),
                  "Khoản này đã thay đổi. Xem danh sách mới bên dưới giúp em nhé; tin cũ không"
                      + " còn dùng được.",
                  EMPTY);
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

  private void abandonEdit(String chat) {
    sessions
        .entrySet()
        .removeIf(entry ->
            entry.getValue().chat().equals(chat) && entry.getValue().phase() != Phase.CARD);
    inputs.remove(chat);
  }

  private String rotate(String key, Session old, String field, TransactionData data, Phase phase) {
    sessions.remove(key);
    var next = token();
    sessions.put(
        next,
        new Session(
            old.chat(), old.id(), old.anchor(), old.before(), field, data, phase, old.expires()));
    if (key.equals(inputs.get(old.chat()))) {
      inputs.put(old.chat(), next);
    }
    return next;
  }

  private void showPreview(String key, Session session, TransactionData proposed, String text) {
    // Retire the old phase before delivery; Save is executable only after the complete preview.
    var next = rotate(key, session, session.field(), null, Phase.INPUT);
    var controls = keyboard(next, "Lưu", "save", "Hủy", "c");
    int anchor = session.anchor();
    try {
      if (text.length() <= MAX_MESSAGE_LENGTH) {
        client.execute(
            EditMessageText.builder()
                .chatId(session.chat())
                .messageId(anchor)
                .text(text)
                .replyMarkup(controls)
                .build());
      } else {
        edit(
            session.chat(),
            anchor,
            "Em gửi bản xem trước đầy đủ ở các tin bên dưới nhé; xác nhận ở tin cuối hoặc"
                + " /cancel để hủy.",
            EMPTY);
        for (int start = 0; start < text.length(); ) {
          int end = Math.min(start + MAX_MESSAGE_LENGTH, text.length());
          if (end < text.length()
              && Character.isHighSurrogate(text.charAt(end - 1))
              && Character.isLowSurrogate(text.charAt(end))) {
            end--;
          }
          var sent =
              send(
                  session.chat(),
                  session.anchor(),
                  text.substring(start, end),
                  end == text.length() ? controls : null);
          if (sent == null || sent.getMessageId() == null) {
            throw new IllegalStateException("Preview delivery has no message ID");
          }
          anchor = sent.getMessageId();
          start = end;
        }
      }
      sessions.put(
          next,
          new Session(
              session.chat(),
              session.id(),
              anchor,
              session.before(),
              session.field(),
              proposed,
              Phase.PREVIEW,
              session.expires()));
    } catch (TelegramApiException | RuntimeException exception) {
      edit(
          session.chat(),
          session.anchor(),
          "Em chưa gửi đầy đủ bản xem trước. Sổ chưa thay đổi; nhập lại giá trị hoặc bấm Hủy"
              + " nhé.",
          keyboard(next, "Hủy", "c"));
    }
  }

  private boolean trusted(Message message, Long sender) {
    return message != null
        && message.getChat() != null
        && message.isUserMessage()
        && sender != null
        && sender.equals(message.getChatId());
  }

  private ChatMessage source(String chat, int message, String text) {
    return new ChatMessage(
        Channel.TELEGRAM, chat, chat, Integer.toString(message), ledgerClock.instant(), text);
  }

  private static String token() {
    return PREFIX + UUID.randomUUID().toString().replace("-", "");
  }

  private static InlineKeyboardButton button(String label, String data) {
    return InlineKeyboardButton.builder().text(label).callbackData(data).build();
  }

  private static InlineKeyboardMarkup keyboard(String token, String... labelsAndOperations) {
    var rows = new ArrayList<InlineKeyboardRow>();
    for (int i = 0; i < labelsAndOperations.length; i += 2) {
      rows.add(
          new InlineKeyboardRow(
              button(labelsAndOperations[i], token + ":" + labelsAndOperations[i + 1])));
    }
    return new InlineKeyboardMarkup(rows);
  }

  private Message send(String chat, int original, String text, InlineKeyboardMarkup keyboard)
      throws TelegramApiException {
    var request = new SendMessage(chat, text);
    request.setReplyParameters(
        ReplyParameters.builder().messageId(original).allowSendingWithoutReply(true).build());
    request.setReplyMarkup(keyboard);
    return client.execute(request);
  }

  private boolean edit(String chat, int id, String text, InlineKeyboardMarkup keyboard) {
    if (id <= 0) {
      return false;
    }
    try {
      client.execute(
          EditMessageText.builder()
              .chatId(chat)
              .messageId(id)
              .text(text)
              .replyMarkup(keyboard)
              .build());
      return true;
    } catch (TelegramApiException | RuntimeException exception) {
      log.warn("Telegram message update failed; stale actions remain disabled on server");
      return false;
    }
  }

  private void acknowledge(String id, String text) {
    try {
      client.execute(AnswerCallbackQuery.builder().callbackQueryId(id).text(text).build());
    } catch (TelegramApiException | RuntimeException exception) {
      log.warn("Telegram action callback acknowledgment failed");
    }
  }
}
