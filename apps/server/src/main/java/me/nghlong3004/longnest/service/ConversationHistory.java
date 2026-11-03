package me.nghlong3004.longnest.service;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import me.nghlong3004.longnest.model.dto.ChatMessage;
import me.nghlong3004.longnest.model.enums.Channel;
import me.nghlong3004.longnest.model.enums.TransactionType;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;

/** Process-local interpretation context, never financial authority or a replay receipt. */
public final class ConversationHistory {
  private static final Comparator<ChatMessage> ORDER =
      Comparator.comparing(ChatMessage::sentAt)
          .thenComparing(
              ConversationHistory::numericId, Comparator.nullsLast(Comparator.naturalOrder()));

  private record Key(Channel channel, String user, String chat) {}

  private record Exchange(
      ChatMessage source, String user, String reply, TransactionType queryType) {}

  private final Map<Key, List<Exchange>> conversations = new LinkedHashMap<>(16, .75f, true);
  private final Object[] locks = IntStream.range(0, 64).mapToObj(i -> new Object()).toArray();

  private Key key(ChatMessage message) {
    return new Key(message.channel(), message.externalUserId(), message.chatId());
  }

  public Object lock(ChatMessage message) {
    return locks[Math.floorMod(key(message).hashCode(), locks.length)];
  }

  public synchronized List<Message> messages(ChatMessage message) {
    var result = new ArrayList<Message>();
    for (var exchange : previous(message)) {
      result.add(new UserMessage(exchange.user()));
      result.add(new AssistantMessage(exchange.reply()));
    }
    return result;
  }

  public synchronized TransactionType queryType(ChatMessage message) {
    var previous = previous(message);
    return previous.isEmpty() ? null : previous.getLast().queryType();
  }

  private List<Exchange> previous(ChatMessage message) {
    return conversations.getOrDefault(key(message), List.of()).stream()
        .filter(e -> !e.source().messageId().equals(message.messageId()))
        .filter(e -> ORDER.compare(e.source(), message) <= 0)
        .toList();
  }

  public synchronized void remember(ChatMessage message, String reply, TransactionType queryType) {
    var exchanges = conversations.computeIfAbsent(key(message), k -> new ArrayList<>());
    if (exchanges.stream().anyMatch(e -> e.source().messageId().equals(message.messageId()))) {
      return;
    }
    // Store bounded text only, not the original unbounded ChatMessage text/name.
    var source =
        new ChatMessage(
            message.channel(),
            message.externalUserId(),
            message.chatId(),
            message.messageId(),
            message.sentAt(),
            "history");
    exchanges.add(new Exchange(source, limit(message.text()), limit(reply), queryType));
    exchanges.sort(Comparator.comparing(Exchange::source, ORDER));
    while (exchanges.size() > 6) {
      exchanges.removeFirst();
    }
    while (conversations.size() > 128) {
      conversations.remove(conversations.keySet().iterator().next());
    }
  }

  private static BigInteger numericId(ChatMessage message) {
    // Telegram message IDs order arrivals within its second-resolution timestamps.
    return message.messageId().matches("[0-9]+") ? new BigInteger(message.messageId()) : null;
  }

  private String limit(String value) {
    return value.codePointCount(0, value.length()) > 1000
        ? value.substring(0, value.offsetByCodePoints(0, 1000))
        : value;
  }
}
