package me.nghlong3004.longnest.integration.telegram;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import me.nghlong3004.longnest.model.dto.TransactionData;
import me.nghlong3004.longnest.model.dto.TransactionResponse;
import me.nghlong3004.longnest.model.dto.TransactionRevision;
import me.nghlong3004.longnest.model.enums.TransactionField;
import me.nghlong3004.longnest.model.enums.TransactionType;
import org.junit.jupiter.api.Test;

class TelegramEditSessionTests {
  @Test
  void onlyACompleteFieldPreviewCanCarrySaveDataAndRetryRemovesThatAuthority() {
    var id = UUID.randomUUID();
    var now = Instant.parse("2026-11-04T00:00:00Z");
    var row =
        new TransactionResponse(
            id,
            TransactionType.EXPENSE,
            new BigDecimal("45000"),
            null,
            "Lunch",
            null,
            LocalDate.of(2026, 11, 4),
            null);
    var card = TelegramEditSession.card("123", id, 100, now.plusSeconds(3600));
    var menu = card.openMenu(new TransactionRevision(row, 7), 101, now.plusSeconds(900));
    var input = menu.chooseFields().requestInput(TransactionField.AMOUNT);
    var data =
        new TransactionData(
            TransactionType.EXPENSE,
            new BigDecimal("50000"),
            null,
            "Lunch",
            null,
            row.date(),
            null);
    assertThatThrownBy(() -> menu.preview(data, 102)).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> input.chooseFields()).isInstanceOf(IllegalArgumentException.class);
    var preview = input.preview(data, 102);
    assertThat(preview.phase()).isEqualTo(TelegramEditSession.Phase.PREVIEW);
    assertThat(preview.proposed().amount()).isEqualByComparingTo("50000");
    var retry = preview.retryInput();
    assertThat(retry.proposed()).isNull();
    assertThat(retry.phase()).isEqualTo(TelegramEditSession.Phase.INPUT);
    assertThat(retry.before().version()).isEqualTo(7);
    assertThat(retry.field()).isEqualTo(TransactionField.AMOUNT);
    assertThat(retry.expired(now.plusSeconds(900))).isTrue();
    assertThat(retry.expired(now.plusSeconds(899))).isFalse();
  }
}
