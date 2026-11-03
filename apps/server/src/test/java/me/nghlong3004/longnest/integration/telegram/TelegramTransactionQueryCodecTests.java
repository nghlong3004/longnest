package me.nghlong3004.longnest.integration.telegram;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import me.nghlong3004.longnest.model.dto.TransactionCursor;
import me.nghlong3004.longnest.model.dto.TransactionFilter;
import me.nghlong3004.longnest.model.dto.TransactionPage;
import me.nghlong3004.longnest.model.dto.TransactionTotals;
import me.nghlong3004.longnest.model.enums.QueryPeriod;
import me.nghlong3004.longnest.model.enums.TransactionType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class TelegramTransactionQueryCodecTests {

  private final TelegramTransactionQueryCodec codec = new TelegramTransactionQueryCodec();

  @Test
  void roundTripsFullUuidDatesTypeAndLargestMessageIdWithinTelegram64ByteBudget() {
    var from = LocalDate.of(1, 1, 1);
    var to = LocalDate.of(9999, 12, 31);
    var cursor = new TransactionCursor(to, UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff"));
    var page =
        new TransactionPage(
            new TransactionFilter(TransactionType.INCOME, from, to),
            List.of(),
            new TransactionTotals(BigDecimal.ZERO, 0),
            cursor);
    var token = codec.encode(page, Integer.MAX_VALUE);
    assertThat(token.getBytes(StandardCharsets.UTF_8)).hasSize(64);
    var decoded = codec.decode(token);
    assertThat(decoded.query().type()).isEqualTo(TransactionType.INCOME);
    assertThat(decoded.query().period()).isEqualTo(QueryPeriod.CUSTOM);
    assertThat(decoded.query().from()).isEqualTo(from);
    assertThat(decoded.query().to()).isEqualTo(to);
    assertThat(decoded.cursor()).isEqualTo(cursor);
    assertThat(decoded.messageId()).isEqualTo(Integer.MAX_VALUE);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "q:",
        "q:E:20261101:20261102:20261102:bad:1",
        "q:UNKNOWN:20261101:20261102:20261102:AAAAAAAAAAAAAAAAAAAAAA:1",
        "q:E:20261102:20261101:20261102:AAAAAAAAAAAAAAAAAAAAAA:1",
        "q:E:20261101:20261102:20261031:AAAAAAAAAAAAAAAAAAAAAA:1",
        "q:E:20261101:20261102:20261102:AAAAAAAAAAAAAAAAAAAAAA:0",
        "q:E:20261101:20261102:20261102:AAAAAAAAAAAAAAAAAAAAAA:-1",
        "q:E:20261101:20261102:20261102:AAAAAAAAAAAAAAAAAAAAAA:2147483648",
        "q:E:00001101:20261102:20261102:AAAAAAAAAAAAAAAAAAAAAA:1",
        "q:E:20260230:20261102:20261102:AAAAAAAAAAAAAAAAAAAAAA:1",
        "q:E:20261101:20261102:20261102:AAAAAAAAAAAAAAAAAAAAAA:1:extra"
      })
  void rejectsMalformedOrTamperedTokens(String token) {
    assertThatThrownBy(() -> codec.decode(token)).isInstanceOf(IllegalArgumentException.class);
  }
}
