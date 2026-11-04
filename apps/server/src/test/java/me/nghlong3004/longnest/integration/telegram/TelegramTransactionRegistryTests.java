package me.nghlong3004.longnest.integration.telegram;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import me.nghlong3004.longnest.model.dto.TransactionFilter;
import me.nghlong3004.longnest.model.enums.TransactionType;
import org.junit.jupiter.api.Test;

class TelegramTransactionRegistryTests {
  private final Clock clock = mock(Clock.class);
  private final TelegramTransactionRegistry registry = new TelegramTransactionRegistry(clock);
  private final TransactionFilter filter =
      new TransactionFilter(
          TransactionType.EXPENSE, LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 4));
  private final Instant now = Instant.parse("2026-11-04T00:00:00Z");

  @Test
  void evictedAndExpiredQueryPagesCannotBeUsedAndDoNotReturnTheirCardActions() {
    when(clock.instant()).thenReturn(now);
    var id = UUID.randomUUID();
    for (int i = 1; i <= 21; i++) {
      var view = registry.openView("123", i, i * 10, filter);
      registry.recordCard(view, "card-" + i, id, i * 10 + 1, "page-" + i);
    }
    assertThat(registry.activePage("123", "page-1", 11)).isFalse();
    assertThat(registry.find("123", "card-1")).isNull();
    assertThat(registry.activePage("123", "page-21", 211)).isTrue();
    assertThat(registry.activePage("789", "page-21", 211)).isFalse();
    when(clock.instant()).thenReturn(now.plusSeconds(3600));
    assertThat(registry.activePage("123", "page-21", 211)).isFalse();
    assertThat(registry.find("123", "card-21")).isNull();
  }

  @Test
  void repeatedRefreshRetiresViewsOnceButRetainsBoundedOldCardsForDeletion() {
    when(clock.instant()).thenReturn(now);
    var id = UUID.randomUUID();
    for (int i = 1; i <= 250; i++) {
      var view = registry.openView("123", 42, i * 10, filter);
      registry.recordCard(view, "card-" + i, id, i * 10 + 1, "page-" + i);
      var retired = registry.retireViews("123");
      assertThat(retired).hasSize(1);
      assertThat(retired.getFirst().summary()).isEqualTo(i * 10);
      assertThat(registry.activePage("123", "page-" + i, i * 10 + 1)).isFalse();
      assertThat(registry.find("123", "card-" + i)).isNull();
    }
    assertThat(registry.cards("123", id)).hasSize(200);
    assertThat(registry.cards("123", id).getFirst().messageId()).isEqualTo(511);
    assertThat(registry.cards("123", id).getLast().messageId()).isEqualTo(2501);
    registry.forgetCards("123", id);
    assertThat(registry.cards("123", id)).isEmpty();
    assertThat(registry.retireViews("123")).isEmpty();
  }

  @Test
  void leastRecentlyUsedChatEvictionRejectsOldActionsWithoutARevokedTokenSet() {
    when(clock.instant()).thenReturn(now);
    var id = UUID.randomUUID();
    for (int i = 1; i <= 129; i++) {
      var view = registry.openView("chat-" + i, 42, 100, filter);
      registry.recordCard(view, "card", id, 101, "page");
    }
    assertThat(registry.find("chat-129", "card")).isNotNull();
    assertThat(registry.activePage("chat-129", "page", 101)).isTrue();
    assertThat(registry.find("chat-1", "card")).isNull();
    assertThat(registry.activePage("chat-1", "page", 101)).isFalse();
  }
}
