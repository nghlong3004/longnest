package me.nghlong3004.longnest.conversation;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import me.nghlong3004.longnest.model.dto.ChatMessage;
import me.nghlong3004.longnest.model.enums.Channel;
import me.nghlong3004.longnest.model.enums.TransactionType;
import org.junit.jupiter.api.Test;

class ConversationHistoryTests {
  private final ConversationHistory history = new ConversationHistory();

  @Test
  void boundsCompletedExchangesUnicodeAndConversationCountAndIsolatesTrustedKeys() {
    for (int i = 0; i < 9; i++) {
      history.remember(
          message("u", "c", "" + i, "🙂".repeat(1200) + i),
          "答".repeat(1200),
          TransactionType.EXPENSE);
    }
    var next = message("u", "c", "10", "current");
    var messages = history.messages(next);
    assertThat(messages).hasSize(12);
    assertThat(messages)
        .allSatisfy(
            m -> assertThat(m.getText().codePointCount(0, m.getText().length())).isEqualTo(1000));
    assertThat(messages.getFirst().getText()).isEqualTo("🙂".repeat(1000));
    assertThat(history.queryType(next)).isEqualTo(TransactionType.EXPENSE);
    assertThat(history.messages(message("other", "c", "10", "current"))).isEmpty();
    assertThat(history.messages(message("u", "other", "10", "current"))).isEmpty();
    for (int i = 0; i < 128; i++)
      history.remember(message("other" + i, "c", "1", "hello"), "reply", null);
    assertThat(history.messages(next)).isEmpty();
    assertThat(history.queryType(next)).isNull();
  }

  @Test
  void numericTelegramIdsOrderSameSecondArrivalsAndDoNotExposeLaterTurnsToAnOlderMessage() {
    history.remember(message("u", "c", "10", "later"), "later reply", null);
    var earlier = message("u", "c", "9", "earlier");
    assertThat(history.messages(earlier)).isEmpty();
    history.remember(earlier, "earlier reply", TransactionType.EXPENSE);
    var newest = message("u", "c", "11", "newest");
    assertThat(history.messages(newest))
        .extracting(m -> m.getText())
        .containsExactly("earlier", "earlier reply", "later", "later reply");
    assertThat(history.queryType(newest)).isNull();
  }

  @Test
  void chronologicalOrderIgnoresFutureAndReplayWithoutResurrectingOldQueryContext() {
    var early = message("u", "c", "1", "early");
    var late =
        new ChatMessage(Channel.TELEGRAM, "u", "c", "2", early.sentAt().plusSeconds(1), "late");
    history.remember(late, "second", null);
    history.remember(early, "first", TransactionType.EXPENSE);
    history.remember(early, "replayed", TransactionType.INCOME);
    assertThat(history.messages(message("u", "c", "3", "now")))
        .extracting(m -> m.getText())
        .containsExactly("early", "first");
    var newest =
        new ChatMessage(Channel.TELEGRAM, "u", "c", "3", early.sentAt().plusSeconds(2), "newest");
    assertThat(history.messages(newest))
        .extracting(m -> m.getText())
        .containsExactly("early", "first", "late", "second");
    assertThat(history.queryType(newest)).isNull();
  }

  private ChatMessage message(String user, String chat, String id, String text) {
    return new ChatMessage(
        Channel.TELEGRAM, user, chat, id, Instant.parse("2026-11-01T18:30:00Z"), text);
  }
}
