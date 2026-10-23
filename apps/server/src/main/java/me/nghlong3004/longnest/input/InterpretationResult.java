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

  /** Parser coverage is not an authoritative domain failure, regardless of legacy UI status. */
  enum FailureKind { RECOGNITION, VALIDATION }

  enum Code {
    UNKNOWN_PHRASE,
    UNSUPPORTED_INPUT,
    MISSING_AMOUNT,
    MISSING_TYPE,
    AMBIGUOUS_AMOUNT,
    CONFLICTING_TYPE,
    INVALID_AMOUNT,
    INVALID_FIELD,
    QUERY_CLARIFICATION
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

  record Problem(Status status, Code code, String field, String example, FailureKind failureKind)
      implements InterpretationResult {
    public Problem(Status status, Code code, String field, String example) {
      this(status, code, field, example, switch (code) {
        case INVALID_AMOUNT, AMBIGUOUS_AMOUNT, CONFLICTING_TYPE, INVALID_FIELD -> FailureKind.VALIDATION;
        default -> FailureKind.RECOGNITION;
      });
    }

    public Problem {
      Objects.requireNonNull(status, "status");
      Objects.requireNonNull(code, "code");
      Objects.requireNonNull(field, "field");
      Objects.requireNonNull(example, "example");
      Objects.requireNonNull(failureKind, "failureKind");
      if (status == Status.READY || field.isBlank() || example.isBlank()) {
        throw new IllegalArgumentException("A problem needs a non-ready status, field and example");
      }
    }
  }
}
