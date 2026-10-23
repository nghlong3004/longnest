package me.nghlong3004.longnest.conversation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import me.nghlong3004.longnest.domain.DateRange;
import me.nghlong3004.longnest.domain.RangeFilter;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;

class ConversationMemoryTests {
  private static final Instant START = Instant.parse("2026-10-23T00:00:00Z");

  @Test
  void retainsOnlyLastEightUserMessagesWithoutAssistantCallsOrToolResults() {
    var memory = new ConversationMemory(Clock.fixed(START, ZoneOffset.UTC));
    var messages = new ArrayList<Message>();
    for (int index = 0; index < 10; index++) {
      messages.add(new UserMessage("user-" + index));
    }
    messages.add(AssistantMessage.builder().content("private model output")
        .toolCalls(List.of(new AssistantMessage.ToolCall("call-1", "function", "clarify", "{}")))
        .build());
    messages.add(ToolResponseMessage.builder().responses(List.of(
        new ToolResponseMessage.ToolResponse("call-1", "clarify", "private tool result")))
        .build());
    memory.add("owner:chat", messages);
    assertThat(memory.get("owner:chat")).extracting(message -> ((UserMessage) message).getText())
        .containsExactly("user-2", "user-3", "user-4", "user-5", "user-6", "user-7",
            "user-8", "user-9");
  }

  @Test
  void ttlExpiresMessagesAndValidatedMetadataAtTenMinutes() {
    var now = new AtomicReference<>(START);
    var clock = mock(Clock.class);
    when(clock.instant()).thenAnswer(ignored -> now.get());
    var memory = new ConversationMemory(clock);
    memory.add("owner:chat", List.of(new UserMessage("prior")));
    var previous = memory.state("owner:chat");
    previous.query = new ConversationMemory.Query(DateRange.parse("2026-10-01", "2026-10-23"),
        RangeFilter.FOOD);
    previous.target = new ConversationMemory.Target("42", "3");
    now.set(START.plusSeconds(599));
    assertThat(memory.get("owner:chat")).hasSize(1);
    assertThat(memory.state("owner:chat")).isSameAs(previous);
    now.set(START.plusSeconds(1199));
    assertThat(memory.get("owner:chat")).isEmpty();
    assertThat(memory.state("owner:chat")).isNotSameAs(previous);
    assertThat(memory.state("owner:chat").query).isNull();
    assertThat(memory.state("owner:chat").target).isNull();
  }

  @Test
  void seventeenthConversationEvictsOldestMessagesAndMetadata() {
    var memory = new ConversationMemory(Clock.fixed(START, ZoneOffset.UTC));
    for (int index = 0; index < 16; index++) {
      memory.add("chat-" + index, List.of(new UserMessage("user-" + index)));
    }
    var oldest = memory.state("chat-0");
    oldest.target = new ConversationMemory.Target("42", "3");
    memory.add("chat-16", List.of(new UserMessage("new")));
    assertThat(memory.get("chat-16")).hasSize(1);
    assertThat(memory.get("chat-0")).isEmpty();
    assertThat(memory.state("chat-0")).isNotSameAs(oldest);
    assertThat(memory.state("chat-0").target).isNull();
  }
}
