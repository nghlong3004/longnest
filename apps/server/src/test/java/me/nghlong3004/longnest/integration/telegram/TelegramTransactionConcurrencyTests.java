package me.nghlong3004.longnest.integration.telegram;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import me.nghlong3004.longnest.mapper.TransactionMapper;
import me.nghlong3004.longnest.model.dto.ChatReply;
import me.nghlong3004.longnest.model.dto.TransactionFilter;
import me.nghlong3004.longnest.model.dto.TransactionPage;
import me.nghlong3004.longnest.model.dto.TransactionResponse;
import me.nghlong3004.longnest.model.dto.TransactionTotals;
import me.nghlong3004.longnest.model.enums.TransactionType;
import me.nghlong3004.longnest.presentation.TransactionActionPresentation;
import me.nghlong3004.longnest.presentation.TransactionMessageFormatter;
import me.nghlong3004.longnest.service.TransactionMutationService;
import me.nghlong3004.longnest.service.TransactionQueryService;
import org.junit.jupiter.api.Test;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.generics.TelegramClient;

class TelegramTransactionConcurrencyTests {
  @Test
  void blockedTelegramDeliveryInOneChatDoesNotHoldTheCoordinatorMonitorForEveryChat()
      throws Exception {
    var client = mock(TelegramClient.class);
    var blocked = new CountDownLatch(1);
    var release = new CountDownLatch(1);
    when(client.execute(any(SendMessage.class)))
        .thenAnswer(
            call -> {
              SendMessage request = call.getArgument(0);
              if (request.getChatId().equals("123")) {
                blocked.countDown();
                if (!release.await(10, TimeUnit.SECONDS))
                  throw new IllegalStateException("test deadline");
              }
              return Message.builder().messageId(100).build();
            });
    var clock = Clock.systemUTC();
    var formatter = new TransactionMessageFormatter();
    var presentation = new TransactionActionPresentation(formatter);
    var actions =
        new TelegramTransactionActions(
            new TelegramMessageSender(client),
            mock(TransactionMutationService.class),
            mock(TransactionQueryService.class),
            formatter,
            new TelegramTransactionQueryCodec(),
            clock,
            mock(TransactionMapper.class),
            presentation,
            new TelegramTransactionRegistry(clock),
            new TelegramTransactionKeyboard(presentation));
    var date = LocalDate.of(2026, 11, 4);
    var row =
        new TransactionResponse(
            UUID.randomUUID(),
            TransactionType.EXPENSE,
            new BigDecimal("45000"),
            null,
            "Lunch",
            null,
            date,
            null);
    var reply =
        new ChatReply(
            "Summary",
            null,
            new TransactionPage(
                new TransactionFilter(TransactionType.EXPENSE, date, date),
                List.of(row),
                new TransactionTotals(new BigDecimal("45000"), 1),
                null));
    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      var slow = executor.submit(() -> actions.sendPage("123", 42, reply));
      try {
        assertThat(blocked.await(5, TimeUnit.SECONDS)).isTrue();
        var other = executor.submit(() -> actions.sendPage("789", 42, reply));
        assertThat(other.get(5, TimeUnit.SECONDS).getMessageId()).isEqualTo(100);
      } finally {
        release.countDown();
      }
      slow.get(5, TimeUnit.SECONDS);
    }
  }
}
