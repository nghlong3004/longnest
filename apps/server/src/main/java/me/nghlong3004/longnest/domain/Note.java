package me.nghlong3004.longnest.domain;

import java.text.Normalizer;
import java.util.Objects;

/** Single-line NFC text; structured notes may contain digits and transaction punctuation. */
public record Note(String value) {
  public static final int MAX_CODE_POINTS = 256;

  public Note {
    Objects.requireNonNull(value, "value");
    if (value.codePoints().anyMatch(Note::isUnsafe)) {
      throw new IllegalArgumentException("Note must be single-line text without controls");
    }
    value = Normalizer.normalize(value, Normalizer.Form.NFC);
    if (value.codePointCount(0, value.length()) > MAX_CODE_POINTS) {
      throw new IllegalArgumentException("Normalized note exceeds 256 code points");
    }
  }

  private static boolean isUnsafe(int codePoint) {
    int type = Character.getType(codePoint);
    return Character.isISOControl(codePoint)
        || type == Character.LINE_SEPARATOR
        || type == Character.PARAGRAPH_SEPARATOR
        || type == Character.SURROGATE;
  }
}
