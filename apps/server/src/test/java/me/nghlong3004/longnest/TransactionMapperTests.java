package me.nghlong3004.longnest;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import me.nghlong3004.longnest.mapper.TransactionMapper;
import me.nghlong3004.longnest.model.dto.TransactionData;
import me.nghlong3004.longnest.model.enums.TimeOfDay;
import me.nghlong3004.longnest.model.enums.TransactionCategory;
import me.nghlong3004.longnest.model.enums.TransactionType;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class TransactionMapperTests {
  private final TransactionMapper mapper = Mappers.getMapper(TransactionMapper.class);

  @Test
  void mapsCompleteProposalWithSuppliedStableIdWithoutInventingValues() {
    var id = UUID.fromString("4cf4f284-41c9-4f87-9c30-56203812cfe9");
    var data =
        new TransactionData(
            TransactionType.EXPENSE,
            new BigDecimal("12.50"),
            TransactionCategory.FOOD,
            "Ăn trưa",
            "Quán cơm",
            LocalDate.of(2026, 11, 2),
            TimeOfDay.NOON);
    var result = mapper.toResponse(data, id);
    assertThat(result.id()).isEqualTo(id);
    assertThat(result.type()).isEqualTo(TransactionType.EXPENSE);
    assertThat(result.amount()).isEqualTo(new BigDecimal("12.50"));
    assertThat(result.category()).isEqualTo(TransactionCategory.FOOD);
    assertThat(result.description()).isEqualTo("Ăn trưa");
    assertThat(result.location()).isEqualTo("Quán cơm");
    assertThat(result.date()).isEqualTo(LocalDate.of(2026, 11, 2));
    assertThat(result.timeOfDay()).isEqualTo(TimeOfDay.NOON);
  }

  @Test
  void mappingPreservesNullsAndDoesNotApplyIncomeRulesOrDateDefaults() {
    var id = UUID.randomUUID();
    var result =
        mapper.toResponse(
            new TransactionData(
                TransactionType.INCOME, null, TransactionCategory.FOOD, null, null, null, null),
            id);
    assertThat(result.id()).isEqualTo(id);
    assertThat(result.type()).isEqualTo(TransactionType.INCOME);
    assertThat(result.category()).isEqualTo(TransactionCategory.FOOD);
    assertThat(result.amount()).isNull();
    assertThat(result.description()).isNull();
    assertThat(result.location()).isNull();
    assertThat(result.date()).isNull();
    assertThat(result.timeOfDay()).isNull();
    assertThat(mapper.toResponse((TransactionData) null, null)).isNull();
  }
}
