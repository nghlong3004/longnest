package me.nghlong3004.longnest.conversation;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import lombok.RequiredArgsConstructor;
import me.nghlong3004.longnest.domain.DateRange;
import me.nghlong3004.longnest.domain.RangeFilter;
import me.nghlong3004.longnest.domain.Money;
import me.nghlong3004.longnest.input.TransactionInterpreter.AmountlessCapture;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;

/** Spring window storage plus application expiry and validated scope; never stores tool results. */
@RequiredArgsConstructor
public final class ConversationMemory implements ChatMemory {
  private static final Duration TTL = Duration.ofMinutes(10);
  private static final int CAPACITY = 16;
  private final Clock clock;
  private final ChatMemory window = MessageWindowChatMemory.builder().maxMessages(8).build();
  private final LinkedHashMap<String, State> states = new LinkedHashMap<>();

  record Query(DateRange range, RangeFilter filter) {}
  record Target(String id, String revision) {}
  record PendingAmount(String sourceEvent, AmountlessCapture facts, LocalDate date, Instant expiry) {}
  record AmountAttempt(String event, PendingAmount source, Money amount) {}

  static final class State {
    private Instant expiry;
    Query query;
    Target target;
    PendingAmount pendingAmount;
    AmountAttempt amountAttempt;
    Integer amountQuestion;
    final LinkedHashSet<String> amountSources = new LinkedHashSet<>();

    State(Instant expiry) { this.expiry = expiry; }
  }

  /** Adapter dispatch is sequential; model workers never read or write amount metadata. */
  synchronized void abandonAmount(String id) {
    State state = states.get(id);
    if (state != null) {
      state.pendingAmount = null;
      state.amountAttempt = null;
      state.amountQuestion = null;
    }
  }

  synchronized State state(String id) {
    Instant now = clock.instant();
    var expired = states.entrySet().stream()
        .filter(entry -> !now.isBefore(entry.getValue().expiry)).map(entry -> entry.getKey()).toList();
    expired.forEach(this::clear);
    if (!states.containsKey(id)) {
      if (states.size() >= CAPACITY) clear(states.firstEntry().getKey());
      states.put(id, new State(now.plus(TTL)));
    }
    State state = states.get(id);
    state.expiry = now.plus(TTL);
    return state;
  }

  @Override
  public synchronized void add(String id, List<Message> messages) {
    state(id);
    // Native assistant tool calls/results must never be replayed as financial history to DeepSeek.
    var users = messages.stream().filter(UserMessage.class::isInstance).toList();
    if (!users.isEmpty()) window.add(id, users);
  }

  @Override
  public synchronized List<Message> get(String id) {
    state(id);
    return window.get(id);
  }

  @Override
  public synchronized void clear(String id) {
    window.clear(id);
    states.remove(id);
  }
}
