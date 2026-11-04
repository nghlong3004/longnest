package me.nghlong3004.longnest.integration.telegram;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.message.Message;

class TelegramCallbackRouterTests {
  @Test
  void routesToTheMatchingCommandAndLeavesUnknownCallbacksAlone() {
    var handled = new ArrayList<String>();
    var router =
        new TelegramCallbackRouter(
            List.of(command("q:", handled), command("cancel_draft:", handled)));
    var message = Message.builder().messageId(500).build();
    for (var data : List.of("q:next", "cancel_draft:one", "unknown")) {
      var callback = new CallbackQuery();
      callback.setData(data);
      router.route(callback, message);
    }
    assertThat(handled).containsExactly("q:next@500", "cancel_draft:one@500");
  }

  private TelegramCallbackHandler command(String prefix, List<String> handled) {
    return new TelegramCallbackHandler() {
      @Override
      public boolean supports(String data) {
        return data.startsWith(prefix);
      }

      @Override
      public void handleCallback(CallbackQuery callback, Message message) {
        handled.add(callback.getData() + "@" + message.getMessageId());
      }
    };
  }
}
