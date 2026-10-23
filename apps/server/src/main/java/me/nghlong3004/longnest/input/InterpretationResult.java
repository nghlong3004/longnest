package me.nghlong3004.longnest.input;

import java.util.Objects;
import me.nghlong3004.longnest.domain.TransactionDraft;

/** Internal interpretation only; ready means validated input, never a saved transaction. */
public sealed interface InterpretationResult {
  Status status();

  enum Status {
    READY,
    UNSUPPORTED,
    NEEDS_CLARIFICATION,
    REJECTED
  }

  enum Code {
    UNKNOWN_PHRASE,
    UNSUPPORTED_INPUT,
    MISSING_AMOUNT,
    MISSING_TYPE,
    AMBIGUOUS_AMOUNT,
    CONFLICTING_TYPE,
    INVALID_AMOUNT,
    INVALID_FIELD
  }

  record Ready(TransactionDraft draft) implements InterpretationResult {
    public Ready {
      Objects.requireNonNull(draft, "draft");
    }

    @Override
    public Status status() {
      return Status.READY;
    }
  }

  record Problem(Status status, Code code, String field, String example)
      implements InterpretationResult {
    public Problem {
      Objects.requireNonNull(status, "status");
      Objects.requireNonNull(code, "code");
      Objects.requireNonNull(field, "field");
      Objects.requireNonNull(example, "example");
      if (status == Status.READY || field.isBlank() || example.isBlank()) {
        throw new IllegalArgumentException("A problem needs a non-ready status, field and example");
      }
    }
  }
}
