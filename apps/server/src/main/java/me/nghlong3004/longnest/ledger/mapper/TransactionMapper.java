package me.nghlong3004.longnest.ledger.mapper;

import static me.nghlong3004.longnest.ledger.model.LedgerException.Code.INVALID_AMOUNT;
import static me.nghlong3004.longnest.ledger.model.LedgerException.Code.INVALID_DATE;
import static me.nghlong3004.longnest.ledger.model.LedgerException.Code.INVALID_FIELD;

import jakarta.validation.Validator;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import me.nghlong3004.longnest.domain.Category;
import me.nghlong3004.longnest.domain.DateRange;
import me.nghlong3004.longnest.domain.RangeFilter;
import me.nghlong3004.longnest.ledger.dto.RangeSummary;
import me.nghlong3004.longnest.domain.Money;
import me.nghlong3004.longnest.domain.Note;
import me.nghlong3004.longnest.domain.TransactionDraft;
import me.nghlong3004.longnest.ledger.dto.MutationReceipt;
import me.nghlong3004.longnest.ledger.dto.OperationRecovery;
import me.nghlong3004.longnest.ledger.dto.ConfirmationRequired;
import me.nghlong3004.longnest.ledger.dto.ControlBinding;
import me.nghlong3004.longnest.ledger.dto.FieldPrompt;
import me.nghlong3004.longnest.ledger.dto.TransactionInput;
import me.nghlong3004.longnest.ledger.dto.TransactionPatch;
import me.nghlong3004.longnest.ledger.dto.TransactionPage;
import me.nghlong3004.longnest.ledger.dto.TransactionView;
import me.nghlong3004.longnest.ledger.model.CreateIntent;
import me.nghlong3004.longnest.ledger.model.EditField;
import me.nghlong3004.longnest.ledger.model.EditField;
import me.nghlong3004.longnest.ledger.model.HostContext;
import me.nghlong3004.longnest.ledger.model.LedgerException;
import me.nghlong3004.longnest.domain.TransactionType;
import me.nghlong3004.longnest.ledger.repository.LedgerStore;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/** Explicit validation/policy helpers around generated representation mapping. */
@Component
@Profile("ledger")
@RequiredArgsConstructor
public class TransactionMapper {
  private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
  private final Validator validator;
  private final TransactionRepresentationMapper representations;

  public void validateHost(HostContext context) {
    validateOperation(context);
    if (context.businessReference() == null) {
      throw new LedgerException(INVALID_FIELD);
    }
  }

  public void validateOperation(HostContext context) {
    if (context.operationId() == null || context.operationId().isBlank()
        || context.operationId().length() > 200) {
      throw new LedgerException(INVALID_FIELD);
    }
  }

  public CreateIntent normalize(TransactionInput input, Instant frozenReference) {
    if (input == null) {
      throw new LedgerException(INVALID_FIELD);
    }
    var violations = validator.validate(input);
    if (!violations.isEmpty()) {
      boolean amountInvalid = violations.stream()
          .anyMatch(v -> v.getPropertyPath().toString().equals("amountMinor"));
      throw new LedgerException(amountInvalid ? INVALID_AMOUNT : INVALID_FIELD);
    }
    Money money;
    try {
      money = new Money(Long.parseLong(input.amountMinor()));
    } catch (IllegalArgumentException exception) {
      throw new LedgerException(INVALID_AMOUNT);
    }
    LocalDate date;
    try {
      if (input.transactionDate().isPresent()) {
        String raw = input.transactionDate().orElseThrow();
        if (!raw.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}")) {
          throw new LedgerException(INVALID_DATE);
        }
        date = LocalDate.parse(raw);
      } else {
        date = frozenReference.atZone(BUSINESS_ZONE).toLocalDate();
      }
    } catch (DateTimeException exception) {
      throw new LedgerException(INVALID_DATE);
    }
    TransactionDraft draft;
    try {
      draft = new TransactionDraft(input.type(), money,
          input.category().orElse(Category.UNCATEGORIZED), new Note(input.note().orElse("")));
    } catch (IllegalArgumentException exception) {
      throw new LedgerException(INVALID_FIELD);
    }
    return new CreateIntent(draft, date);
  }

  public TransactionInput input(TransactionDraft draft) {
    return representations.input(Objects.requireNonNull(draft));
  }

  public TransactionInput input(TransactionDraft draft, LocalDate date) {
    return new TransactionInput(draft.type(), draft.amount().canonicalAmount(), Money.CURRENCY,
        Optional.of(date.toString()), Optional.of(draft.category()), Optional.of(draft.note().value()));
  }

  public OperationRecovery recovered(MutationReceipt receipt) {
    return new OperationRecovery(receipt, null);
  }

  public OperationRecovery recovered(ConfirmationRequired preview) {
    return new OperationRecovery(null, preview);
  }

  /** Applies a sparse patch to a frozen snapshot; no identity/timestamp field is editable. */
  public CreateIntent patch(CreateIntent before, TransactionPatch patch) {
    if (patch == null || (patch.amountMinor().isEmpty() && patch.type().isEmpty()
        && patch.category().isEmpty() && patch.note().isEmpty()
        && patch.transactionDate().isEmpty())) {
      throw new LedgerException(INVALID_FIELD);
    }
    var original = before.draft();
    TransactionType type = patch.type().orElse(original.type());
    Category category = patch.category().orElse(original.category());
    if (patch.type().isPresent() && patch.category().isEmpty() && !category.supports(type)) {
      category = Category.UNCATEGORIZED;
    }
    Money amount;
    try {
      amount = patch.amountMinor().map(Money::parse).orElse(original.amount());
    } catch (IllegalArgumentException invalid) {
      throw new LedgerException(INVALID_AMOUNT);
    }
    LocalDate date = before.transactionDate();
    if (patch.transactionDate().isPresent()) {
      String raw = patch.transactionDate().orElseThrow();
      try {
        if (!raw.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}")) {
          throw new DateTimeException("Invalid financial date");
        }
        date = LocalDate.parse(raw);
        if (date.getYear() < 1900 || date.getYear() > 9999) {
          throw new DateTimeException("Financial date out of range");
        }
      } catch (DateTimeException invalid) {
        throw new LedgerException(INVALID_DATE);
      }
    }
    try {
      return new CreateIntent(new TransactionDraft(type, amount, category,
          new Note(patch.note().orElse(original.note().value()))), date);
    } catch (IllegalArgumentException invalid) {
      throw new LedgerException(INVALID_FIELD);
    }
  }

  /** Converts one explicitly selected field response; full after-state validation remains in patch. */
  public TransactionPatch fieldPatch(EditField field, String value) {
    if (field == null || value == null) throw new LedgerException(INVALID_FIELD);
    return switch (field) {
      case AMOUNT -> new TransactionPatch(Optional.of(value.trim()), Optional.empty(),
          Optional.empty(), Optional.empty(), Optional.empty());
      case TYPE -> new TransactionPatch(Optional.empty(),
          Optional.of(parseEnum(TransactionType.class, value)), Optional.empty(),
          Optional.empty(), Optional.empty());
      case CATEGORY -> new TransactionPatch(Optional.empty(), Optional.empty(),
          Optional.of(parseEnum(Category.class, value)), Optional.empty(), Optional.empty());
      case NOTE -> new TransactionPatch(Optional.empty(), Optional.empty(), Optional.empty(),
          Optional.of(value), Optional.empty());
      case DATE -> new TransactionPatch(Optional.empty(), Optional.empty(), Optional.empty(),
          Optional.empty(), Optional.of(value.trim()));
    };
  }

  private static <T extends Enum<T>> T parseEnum(Class<T> type, String value) {
    try {
      return Enum.valueOf(type, value.trim().toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException invalid) {
      throw new LedgerException(INVALID_FIELD);
    }
  }

  public String fingerprint(CreateIntent intent) {
    var draft = intent.draft();
    var framed = new StringBuilder();
    var fields = List.of("create", draft.type().name(), draft.amount().canonicalAmount(),
        Money.CURRENCY, draft.category().name(), draft.note().value(),
        intent.transactionDate().toString());
    for (String field : fields) {
      framed.append(field.length()).append(':').append(field);
    }
    try {
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
          .digest(framed.toString().getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("Required SHA-256 implementation unavailable", exception);
    }
  }

  public static Instant micros(Instant instant) {
    return instant.truncatedTo(ChronoUnit.MICROS);
  }

  public TransactionView view(LedgerStore.Entry entry) {
    return representations.view(Objects.requireNonNull(entry));
  }

  public MutationReceipt receipt(LedgerStore.Inbox inbox, boolean replayed) {
    var saved = inbox.receipt().orElseThrow();
    var original = new LedgerStore.Entry(
        inbox.owner(), saved.entryId(), 1, inbox.intent(),
        saved.createdAt(), inbox.messageAt(), false);
    return representations.receipt(
        inbox.operationId(), replayed, view(original), saved.generation());
  }

  public FieldPrompt fieldPrompt(LedgerStore.Control control) {
    return representations.fieldPrompt(control.id(), control.field(), view(control.before()),
        view(control.after()), control.expiresAt(), control.displayMessage());
  }

  public ConfirmationRequired confirmation(LedgerStore.Control control) {
    return representations.confirmation(control.id(),
        ConfirmationRequired.Kind.valueOf(control.kind().name()), view(control.before()),
        view(control.after()), Long.toString(control.before().revision()),
        control.expiresAt(), control.displayMessage());
  }

  public ControlBinding controlBinding(LedgerStore.Control control) {
    return representations.controlBinding(control);
  }

  public MutationReceipt actionReceipt(LedgerStore.Control action, boolean replayed) {
    boolean changed = Boolean.TRUE.equals(action.changed());
    var after = action.after();
    var original = new LedgerStore.Entry(after.owner(), after.id(),
        action.before().revision() + (changed ? 1 : 0), after.intent(), after.createdAt(),
        after.messageAt(), after.deleted());
    return representations.actionReceipt(action.id(), action.kind().name().toLowerCase(),
        changed, replayed, view(original), Long.toString(action.generation()));
  }

  public TransactionPage page(
      List<LedgerStore.Entry> entries, long generation, Optional<String> next) {
    return representations.page(Objects.requireNonNull(entries), generation, next);
  }

  public RangeSummary summary(DateRange range, RangeFilter filter, LedgerStore.Totals totals,
      long generation) {
    return representations.summary(range, filter, totals.incomeTotal().toString(),
        totals.incomeCount().toString(), totals.expenseTotal().toString(),
        totals.expenseCount().toString(), totals.net().toString(), Long.toString(generation));
  }

  public TransactionPage rangePage(List<LedgerStore.Entry> entries, RangeSummary summary,
      Optional<String> next) {
    return representations.rangePage(entries, summary, next);
  }
}
