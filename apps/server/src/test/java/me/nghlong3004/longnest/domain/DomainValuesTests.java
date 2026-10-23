package me.nghlong3004.longnest.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class DomainValuesTests {
  @ParameterizedTest
  @CsvSource({
    "1, 1", "1000000000000, 1000000000000", "45, 45", "45000đ, 45000",
    "45000 vnd, 45000", "45000 VND, 45000", "45000Đ, 45000", "45k, 45000",
    "25TR, 25000000", "1.5tr, 1500000", "'1,5tr', 1500000", "0.5k, 500",
    "0.01k, 10", "0.01tr, 10000", "00045, 45", "000.01k, 10",
    "1000000000k, 1000000000000", "1000000tr, 1000000000000"
  })
  void parsesExactWholeDong(String input, long expected) {
    var money = Money.parse(input);
    assertThat(money.amountMinor()).isEqualTo(expected);
    assertThat(money.canonicalAmount()).isEqualTo(Long.toString(expected));
    assertThat(Money.CURRENCY).isEqualTo("VND");
  }

  @ParameterizedTest
  @ValueSource(strings = {
    "", "0", "000", "0.00k", "1000000000001", "1000000.01tr", "NaN", "1e6",
    "9223372036854775808", "9999999999999999999999999999999999999999999",
    "+45", "-45", "--45", "45+5", "45 k", "1.234tr", "1,234tr", "45.000",
    "45,000", "1 000 000", "1.5", "1.50vnd", "45kđ", "45k vnd", "45usd",
    "$45", "45€", "４５", "٤٥", "−45", "＋45", " 45", "45 ", ".5k", "1.k"
  })
  void rejectsInvalidAmountWithoutRounding(String input) {
    assertThatIllegalArgumentException().isThrownBy(() -> Money.parse(input));
  }

  @ParameterizedTest
  @ValueSource(longs = {Long.MIN_VALUE, -1, 0, 1_000_000_000_001L, Long.MAX_VALUE})
  void directConstructionCannotBypassMoneyBounds(long amount) {
    assertThatIllegalArgumentException().isThrownBy(() -> new Money(amount));
  }

  @Test
  void normalizesNotesWithoutApplyingMessageGrammar() {
    assertThat(new Note("ca\u0300 phe\u0302 42; + note").value()).isEqualTo("cà phê 42; + note");
    assertThat(new Note("").value()).isEmpty();
    assertThat(new Note("😀".repeat(256)).value().length()).isEqualTo(512);
  }

  @Test
  void enforcesCanonicalNfcNoteLimitIndependentlyOfMessageInputLimit() {
    assertThatIllegalArgumentException().isThrownBy(() -> new Note("😀".repeat(257)));
    assertThatIllegalArgumentException().isThrownBy(() -> new Note("\u0344".repeat(129)));
    assertThat(new Note("e\u0301".repeat(256)).value()).isEqualTo("é".repeat(256));
    assertThatIllegalArgumentException().isThrownBy(() -> new Note("e\u0301".repeat(257)));
  }

  @ParameterizedTest
  @ValueSource(strings = {
    "\n", "\r", "\t", "\u0000", "\u007f", "\u0085", "\u2028", "\u2029", "\ud800"
  })
  void rejectsControlsLineBreaksAndMalformedUnicode(String unsafe) {
    assertThatIllegalArgumentException().isThrownBy(() -> new Note("before" + unsafe + "after"));
  }

  @Test
  void allCategoriesEnforceCompatibilityInDirectDraftConstruction() {
    for (var category : Category.values()) {
      for (var type : TransactionType.values()) {
        boolean expected = switch (category) {
          case FOOD, GROCERIES, TRANSPORT, RENT -> type == TransactionType.EXPENSE;
          case SALARY, OTHER_INCOME -> type == TransactionType.INCOME;
          case UNCATEGORIZED -> true;
        };
        assertThat(category.supports(type)).isEqualTo(expected);
        if (expected) {
          assertThat(new TransactionDraft(type, new Money(1), category, new Note("")))
              .isNotNull();
        } else {
          assertThatIllegalArgumentException()
              .isThrownBy(() -> new TransactionDraft(type, new Money(1), category, new Note("")));
        }
      }
    }
  }

  @Test
  void mandatoryDomainValuesCannotBeNull() {
    assertThatNullPointerException().isThrownBy(() -> Money.parse(null));
    assertThatNullPointerException().isThrownBy(() -> new Note(null));
    assertThatNullPointerException().isThrownBy(() -> Category.UNCATEGORIZED.supports(null));
    assertThatNullPointerException()
        .isThrownBy(() -> new TransactionDraft(null, new Money(1), Category.FOOD, new Note("")));
    assertThatNullPointerException()
        .isThrownBy(
            () -> new TransactionDraft(TransactionType.EXPENSE, null, Category.FOOD, new Note("")));
    assertThatNullPointerException()
        .isThrownBy(
            () -> new TransactionDraft(TransactionType.EXPENSE, new Money(1), null, new Note("")));
    assertThatNullPointerException()
        .isThrownBy(
            () -> new TransactionDraft(TransactionType.EXPENSE, new Money(1), Category.FOOD, null));
  }
}
