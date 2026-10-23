package me.nghlong3004.longnest.ledger.mapper;

import java.util.List;
import java.util.Optional;
import java.time.Instant;
import me.nghlong3004.longnest.domain.TransactionDraft;
import me.nghlong3004.longnest.domain.DateRange;
import me.nghlong3004.longnest.domain.RangeFilter;
import me.nghlong3004.longnest.ledger.dto.RangeSummary;
import me.nghlong3004.longnest.ledger.dto.ConfirmationRequired;
import me.nghlong3004.longnest.ledger.dto.ControlBinding;
import me.nghlong3004.longnest.ledger.dto.FieldPrompt;
import me.nghlong3004.longnest.ledger.dto.MutationReceipt;
import me.nghlong3004.longnest.ledger.dto.TransactionInput;
import me.nghlong3004.longnest.ledger.dto.TransactionPage;
import me.nghlong3004.longnest.ledger.dto.TransactionView;
import me.nghlong3004.longnest.ledger.model.EditField;
import me.nghlong3004.longnest.ledger.repository.LedgerStore;
import org.mapstruct.AnnotateWith;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;
import org.springframework.context.annotation.Profile;

/** Copies validated representations; never normalizes input or authorizes an operation. */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING,
    injectionStrategy = InjectionStrategy.CONSTRUCTOR, unmappedTargetPolicy = ReportingPolicy.ERROR,
    imports = {Optional.class, TransactionView.class})
@AnnotateWith(value = Profile.class,
    elements = @AnnotateWith.Element(name = "value", strings = "ledger"))
public interface TransactionRepresentationMapper {
  @Mapping(target = "amountMinor", expression = "java(draft.amount().canonicalAmount())")
  @Mapping(target = "currency", constant = "VND")
  @Mapping(target = "transactionDate", expression = "java(Optional.empty())")
  @Mapping(target = "category", expression = "java(Optional.of(draft.category()))")
  @Mapping(target = "note", expression = "java(Optional.of(draft.note().value()))")
  TransactionInput input(TransactionDraft draft);

  @Mapping(target = "type", source = "intent.draft.type")
  @Mapping(target = "amountMinor",
      expression = "java(entry.intent().draft().amount().canonicalAmount())")
  @Mapping(target = "currency", constant = "VND")
  @Mapping(target = "category", source = "intent.draft.category")
  @Mapping(target = "note", source = "intent.draft.note.value")
  @Mapping(target = "transactionDate", source = "intent.transactionDate")
  @Mapping(target = "state", expression =
      "java(entry.deleted() ? TransactionView.State.DELETED : TransactionView.State.ACTIVE)")
  TransactionView view(LedgerStore.Entry entry);

  @Mapping(target = "operation", constant = "create")
  @Mapping(target = "changed", constant = "true")
  MutationReceipt receipt(
      String operationId, boolean replayed, TransactionView transaction, long generation);

  FieldPrompt fieldPrompt(String promptId, EditField field, TransactionView before,
      TransactionView current,
      Instant expiresAt, Long displayMessage);

  ConfirmationRequired confirmation(String actionId, ConfirmationRequired.Kind kind,
      TransactionView before, TransactionView after, String expectedRevision,
      Instant expiresAt, Long displayMessage);

  @Mapping(target = "targetId", source = "before.id")
  @Mapping(target = "expectedRevision", source = "before.revision")
  ControlBinding controlBinding(LedgerStore.Control control);

  MutationReceipt actionReceipt(String operationId, String operation, boolean changed,
      boolean replayed, TransactionView transaction, String generation);

  @Mapping(target = "scope", constant = "recent")
  @Mapping(target = "pageSize", constant = "10")
  @Mapping(target = "nextCursor", source = "next")
  @Mapping(target = "summary", expression = "java(Optional.empty())")
  TransactionPage page(List<LedgerStore.Entry> entries, long generation, Optional<String> next);

  @Mapping(target = "currency", constant = "VND")
  RangeSummary summary(DateRange range, RangeFilter filter, String incomeTotal,
      String incomeCount, String expenseTotal, String expenseCount, String net, String generation);

  @Mapping(target = "scope", constant = "range")
  @Mapping(target = "pageSize", constant = "10")
  @Mapping(target = "nextCursor", source = "next")
  @Mapping(target = "summary", expression = "java(Optional.of(summary))")
  @Mapping(target = "generation", source = "summary.generation")
  TransactionPage rangePage(List<LedgerStore.Entry> entries, RangeSummary summary,
      Optional<String> next);
}
