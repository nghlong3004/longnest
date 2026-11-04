package me.nghlong3004.longnest.integration.telegram;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import me.nghlong3004.longnest.mapper.TransactionMapper;
import me.nghlong3004.longnest.model.dto.ChatMessage;
import me.nghlong3004.longnest.model.dto.ChatReply;
import me.nghlong3004.longnest.model.dto.TransactionFilter;
import me.nghlong3004.longnest.model.dto.TransactionPage;
import me.nghlong3004.longnest.model.dto.TransactionResponse;
import me.nghlong3004.longnest.model.dto.TransactionRevision;
import me.nghlong3004.longnest.model.dto.TransactionTotals;
import me.nghlong3004.longnest.model.enums.Channel;
import me.nghlong3004.longnest.model.enums.TransactionType;
import me.nghlong3004.longnest.presentation.TransactionActionPresentation;
import me.nghlong3004.longnest.presentation.TransactionMessageFormatter;
import me.nghlong3004.longnest.service.TransactionMutationService;
import me.nghlong3004.longnest.service.TransactionQueryService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.chat.Chat;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.generics.TelegramClient;

class TelegramTransactionClockTests {
  @Test
  void actionSourceUsesConfiguredClockWithoutChangingTrustedIdentityOrAnchor() throws Exception {
    var client = mock(TelegramClient.class);
    var mutations = mock(TransactionMutationService.class);
    var formatter = new TransactionMessageFormatter();
    var instant = Instant.parse("2001-02-03T04:05:06Z");
    var actions =
        new TelegramTransactionActions(
            new TelegramMessageSender(client),
            mutations,
            mock(TransactionQueryService.class),
            formatter,
            new TelegramTransactionQueryCodec(),
            Clock.fixed(instant, ZoneOffset.UTC),
            mock(TransactionMapper.class),
            new TransactionActionPresentation(formatter),
            new TelegramTransactionRegistry(Clock.fixed(instant, ZoneOffset.UTC)),
            new TelegramTransactionKeyboard(new TransactionActionPresentation(formatter)));
    var sequence = new AtomicInteger(100);
    var requests = new ArrayList<SendMessage>();
    when(client.execute(any(SendMessage.class)))
        .thenAnswer(
            call -> {
              requests.add(call.getArgument(0));
              return message(sequence.incrementAndGet());
            });
    var id = UUID.randomUUID();
    var date = LocalDate.of(2001, 2, 3);
    var item =
        new TransactionResponse(
            id, TransactionType.EXPENSE, new BigDecimal("45000"), null, "Lunch", null, date, null);
    when(mutations.load(any(ChatMessage.class), eq(id)))
        .thenReturn(new TransactionRevision(item, 0));
    actions.sendPage(
        "123",
        42,
        new ChatReply(
            "Summary",
            null,
            new TransactionPage(
                new TransactionFilter(TransactionType.EXPENSE, date, date),
                List.of(item),
                new TransactionTotals(new BigDecimal("45000"), 1),
                null)));
    var callback = new CallbackQuery();
    callback.setId("synthetic-callback");
    callback.setFrom(new User(123L, "Synthetic", false));
    var controls = (InlineKeyboardMarkup) requests.get(1).getReplyMarkup();
    callback.setData(controls.getKeyboard().getFirst().getFirst().getCallbackData());
    var card = message(102);
    callback.setMessage(card);
    actions.handleCallback(callback, card);
    var source = ArgumentCaptor.forClass(ChatMessage.class);
    verify(mutations).load(source.capture(), eq(id));
    assertThat(source.getValue().sentAt()).isEqualTo(Instant.parse("2001-02-03T04:05:06Z"));
    assertThat(source.getValue().channel()).isEqualTo(Channel.TELEGRAM);
    assertThat(source.getValue().externalUserId()).isEqualTo("123");
    assertThat(source.getValue().chatId()).isEqualTo("123");
    assertThat(source.getValue().messageId()).isEqualTo("102");
  }

  private Message message(int id) {
    return Message.builder()
        .messageId(id)
        .chat(new Chat(123L, "private"))
        .date(981173106)
        .from(new User(999L, "Bot", true))
        .build();
  }
}
