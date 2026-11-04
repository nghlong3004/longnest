package me.nghlong3004.longnest.integration.telegram;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import me.nghlong3004.longnest.model.dto.TransactionFilter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Bounded process-local UI state. Durable ledger receipts remain in the services. */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "longnest.telegram", name = "enabled", havingValue = "true")
public class TelegramTransactionRegistry {
  private static final int MAX_CHATS = 128;
  private static final int MAX_VIEWS = 20;
  private static final int MAX_CARDS = 200;
  private static final Duration VIEW_TTL = Duration.ofHours(1);
  private final Clock ledgerClock;
  private final Map<String, ChatState> chats = new LinkedHashMap<>(16, .75f, true);
  private final Object[] locks = IntStream.range(0, 64).mapToObj(i -> new Object()).toArray();

  record Card(UUID id, int messageId) {}

  record Input(String key, TelegramEditSession session) {}

  static final class View {
    private final String chat;
    private final int original;
    private final int summary;
    private final TransactionFilter filter;
    private final Instant expires;
    private final List<Card> cards = new ArrayList<>();
    private final Map<Integer, String> pages = new LinkedHashMap<>();

    private View(
        String chat, int original, int summary, TransactionFilter filter, Instant expires) {
      this.chat = chat;
      this.original = original;
      this.summary = summary;
      this.filter = filter;
      this.expires = expires;
    }

    String chat() {
      return chat;
    }

    int original() {
      return original;
    }

    int summary() {
      return summary;
    }

    TransactionFilter filter() {
      return filter;
    }

    List<Card> cards() {
      return List.copyOf(cards);
    }
  }

  private static final class ChatState {
    final Map<String, TelegramEditSession> sessions = new LinkedHashMap<>();
    final Deque<View> views = new ArrayDeque<>();
    final Map<Integer, Card> cards = new LinkedHashMap<>();
    String input;
  }

  Object lock(String chat) {
    return locks[Math.floorMod(chat.hashCode(), locks.length)];
  }

  static String token() {
    return TelegramTransactionActions.PREFIX + UUID.randomUUID().toString().replace("-", "");
  }

  synchronized View openView(String chat, int original, int summary, TransactionFilter filter) {
    var state = state(chat);
    var view = new View(chat, original, summary, filter, ledgerClock.instant().plus(VIEW_TTL));
    state.views.addLast(view);
    while (state.views.size() > MAX_VIEWS) drop(state, state.views.removeFirst());
    return view;
  }

  synchronized void recordCard(View view, String token, UUID id, int messageId, String nextPage) {
    var state = state(view.chat);
    if (!state.views.contains(view)) return;
    var card = new Card(id, messageId);
    view.cards.add(card);
    if (nextPage != null) view.pages.put(messageId, nextPage);
    state.cards.put(messageId, card);
    state.sessions.put(token, TelegramEditSession.card(view.chat, id, messageId, view.expires));
    while (state.cards.size() > MAX_CARDS) {
      state.cards.remove(state.cards.keySet().iterator().next());
    }
  }

  synchronized TelegramEditSession find(String chat, String key) {
    return state(chat).sessions.get(key);
  }

  synchronized Input input(String chat) {
    var state = state(chat);
    var session = state.sessions.get(state.input);
    return session == null ? null : new Input(state.input, session);
  }

  synchronized void begin(String key, TelegramEditSession session) {
    var state = state(session.chat());
    abandonEdit(state);
    state.sessions.put(key, session);
    state.input = key;
  }

  synchronized void replace(String oldKey, String newKey, TelegramEditSession session) {
    var state = state(session.chat());
    if (!state.sessions.containsKey(oldKey)) throw new IllegalArgumentException("Expired edit");
    state.sessions.remove(oldKey);
    state.sessions.put(newKey, session);
    if (oldKey.equals(state.input)) state.input = newKey;
  }

  synchronized void remove(String chat, String key) {
    var state = state(chat);
    state.sessions.remove(key);
    if (key.equals(state.input)) state.input = null;
  }

  synchronized boolean activePage(String chat, String data, int messageId) {
    return state(chat).views.stream().anyMatch(view -> data.equals(view.pages.get(messageId)));
  }

  synchronized List<View> retireViews(String chat) {
    var state = state(chat);
    var views = List.copyOf(state.views);
    state.views.clear();
    state.sessions.clear();
    state.input = null;
    return views;
  }

  synchronized List<Card> cards(String chat, UUID id) {
    return state(chat).cards.values().stream().filter(card -> card.id().equals(id)).toList();
  }

  synchronized void forgetCards(String chat, UUID id) {
    state(chat).cards.values().removeIf(card -> card.id().equals(id));
  }

  private ChatState state(String chat) {
    var state = chats.computeIfAbsent(chat, ignored -> new ChatState());
    var now = ledgerClock.instant();
    while (!state.views.isEmpty() && !state.views.getFirst().expires.isAfter(now)) {
      drop(state, state.views.removeFirst());
    }
    while (chats.size() > MAX_CHATS) chats.remove(chats.keySet().iterator().next());
    return state;
  }

  private void drop(ChatState state, View view) {
    var anchors = view.cards.stream().map(Card::messageId).toList();
    state
        .sessions
        .values()
        .removeIf(
            session ->
                session.phase() == TelegramEditSession.Phase.CARD
                    && anchors.contains(session.anchor()));
  }

  private void abandonEdit(ChatState state) {
    state.sessions.values().removeIf(session -> session.phase() != TelegramEditSession.Phase.CARD);
    state.input = null;
  }
}
