package me.nghlong3004.longnest.ledger.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigInteger;
import me.nghlong3004.longnest.domain.DateRange;
import me.nghlong3004.longnest.domain.RangeFilter;

@Schema(description = "Full inclusive financial-date range totals, never a visible-page subtotal")
public record RangeSummary(
    @NotNull @Valid @Schema(description = "Inclusive Asia/Ho_Chi_Minh financial-date range")
        DateRange range,
    @NotNull @Schema(description = "ALL, INCOME, EXPENSE or Category.FOOD expenses")
        RangeFilter filter,
    @NotNull @Schema(description = "Currency", example = "VND") String currency,
    @NotNull @Pattern(regexp = "0|[1-9][0-9]*") @Schema(description = "Exact income VND total")
        String incomeTotal,
    @NotNull @Pattern(regexp = "0|[1-9][0-9]*") @Schema(description = "Income entry count")
        String incomeCount,
    @NotNull @Pattern(regexp = "0|[1-9][0-9]*") @Schema(description = "Exact expense VND total")
        String expenseTotal,
    @NotNull @Pattern(regexp = "0|[1-9][0-9]*") @Schema(description = "Expense entry count")
        String expenseCount,
    @NotNull @Pattern(regexp = "0|-?[1-9][0-9]*") @Schema(description = "Income minus expense VND")
        String net,
    @NotNull @Pattern(regexp = "0|[1-9][0-9]*") @Schema(description = "Observed owner generation")
        String generation) {
  public RangeSummary {
    if (range == null || filter == null || !"VND".equals(currency)
        || invalidUnsigned(incomeTotal) || invalidUnsigned(incomeCount)
        || invalidUnsigned(expenseTotal) || invalidUnsigned(expenseCount)
        || net == null || !net.matches("0|-?[1-9][0-9]*")
        || invalidUnsigned(generation)) {
      throw new IllegalArgumentException("Invalid range summary");
    }
    if (!new BigInteger(incomeTotal).subtract(new BigInteger(expenseTotal))
        .equals(new BigInteger(net))) {
      throw new IllegalArgumentException("Invalid range net");
    }
  }

  private static boolean invalidUnsigned(String value) {
    return value == null || !value.matches("0|[1-9][0-9]*");
  }
}
