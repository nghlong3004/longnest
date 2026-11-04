package me.nghlong3004.longnest.model.enums;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class TransactionFieldTests {
  @ParameterizedTest
  @CsvSource({
    "AMOUNT,amount",
    "DATE,date",
    "TYPE,type",
    "CATEGORY,category",
    "DESCRIPTION,description",
    "LOCATION,location",
    "TIME_OF_DAY,timeOfDay"
  })
  void preservesExistingCallbackValues(TransactionField field, String callbackValue) {
    assertThat(TransactionField.fromValue(callbackValue)).isEqualTo(field);
    assertThat(field.getValue()).isEqualTo(callbackValue);
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(
      strings = {"owner", "userId", "AMOUNT", "TimeOfDay", "time_of_day", " amount", "amount "})
  void rejectsValuesOutsideTheExactEditableFieldBoundary(String value) {
    assertThatThrownBy(() -> TransactionField.fromValue(value))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
