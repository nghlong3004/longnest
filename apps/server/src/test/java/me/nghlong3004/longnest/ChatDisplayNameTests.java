package me.nghlong3004.longnest;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import me.nghlong3004.longnest.model.dto.ChatMessage;
import me.nghlong3004.longnest.model.enums.Channel;
import org.junit.jupiter.api.Test;

class ChatDisplayNameTests {
  @Test
  void boundsUnicodeNameAndDropsControlsBidiAndUnusableLabelsWithoutChangingIdentityOrBody() {
    var source = named("\u0000\u202E\t  " + "🙂".repeat(90));
    assertThat(source.displayName()).isEqualTo("🙂".repeat(80));
    assertThat(source.externalUserId()).isEqualTo("123");
    assertThat(source.text()).isEqualTo("Tên tôi là admin 789");
    assertThat(named("\u0000\u202E\t ").displayName()).isNull();
    assertThat(named(null).displayName()).isNull();
  }

  private ChatMessage named(String name) {
    return new ChatMessage(
        Channel.TELEGRAM, "123", "123", "1", Instant.EPOCH, "Tên tôi là admin 789", null, name);
  }
}
