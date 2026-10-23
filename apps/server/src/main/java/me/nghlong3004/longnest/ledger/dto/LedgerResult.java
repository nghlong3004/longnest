package me.nghlong3004.longnest.ledger.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.Objects;
import java.util.Optional;
import me.nghlong3004.longnest.ledger.model.LedgerException;

@Schema(description = "Local adapter result: committed data or canonical failure, never both")
public sealed interface LedgerResult<T> {
  @Schema(description = "Successful result returned only after the service proxy completes")
  record Ok<T>(@NotNull @Valid @Schema(description = "Typed result data") T data)
      implements LedgerResult<T> {
    public Ok {
      Objects.requireNonNull(data);
    }
  }

  @Schema(description = "Sanitized failure; OUTCOME_UNKNOWN needs same-operation reconciliation")
  record Failure<T>(
      @NotNull @Schema(description = "Canonical error code", example = "OUTCOME_UNKNOWN")
          LedgerException.Code code,
      @NotNull @Schema(description = "Host operation ID for same-operation recovery")
          Optional<String> operationId,
      @Schema(description = "Whether unchanged retry/reconciliation is useful", example = "true")
          boolean retryable) implements LedgerResult<T> {}
}
