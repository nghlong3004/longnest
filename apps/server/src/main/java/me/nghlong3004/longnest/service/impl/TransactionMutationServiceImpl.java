package me.nghlong3004.longnest.service.impl;

import jakarta.validation.Validator;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import me.nghlong3004.longnest.mapper.TransactionMapper;
import me.nghlong3004.longnest.model.dto.ChatMessage;
import me.nghlong3004.longnest.model.dto.TransactionData;
import me.nghlong3004.longnest.model.dto.TransactionRevision;
import me.nghlong3004.longnest.model.entity.Transaction;
import me.nghlong3004.longnest.model.enums.TimeOfDay;
import me.nghlong3004.longnest.model.enums.TransactionCategory;
import me.nghlong3004.longnest.model.enums.TransactionField;
import me.nghlong3004.longnest.model.enums.TransactionType;
import me.nghlong3004.longnest.repository.DeletedTransactionSourceRepository;
import me.nghlong3004.longnest.repository.TransactionActionInputRepository;
import me.nghlong3004.longnest.repository.TransactionDraftRepository;
import me.nghlong3004.longnest.repository.TransactionRepository;
import me.nghlong3004.longnest.repository.UserIdentityRepository;
import me.nghlong3004.longnest.service.TransactionMutationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TransactionMutationServiceImpl implements TransactionMutationService {
  private final UserIdentityRepository identities;
  private final TransactionRepository transactions;
  private final TransactionDraftRepository drafts;
  private final DeletedTransactionSourceRepository deletedSources;
  private final TransactionMapper mapper;
  private final Validator validator;
  private final TransactionActionInputRepository actionInputs;

  @Override
  @Transactional
  public boolean consumeInput(ChatMessage source) {
    if (source == null || !validator.validate(source).isEmpty()) {
      throw invalid();
    }
    identities
        .findByChannelAndExternalUserId(source.channel(), source.externalUserId())
        .orElseThrow(TransactionMutationServiceImpl::invalid);
    if (actionInputs.existsByChannelAndExternalUserIdAndChatIdAndMessageId(
        source.channel(), source.externalUserId(), source.chatId(), source.messageId())) {
      return false;
    }
    actionInputs.saveAndFlush(mapper.toActionInput(source));
    return true;
  }

  @Override
  @Transactional
  public TransactionRevision load(ChatMessage source, UUID id) {
    var row = owned(source, id);
    return new TransactionRevision(mapper.toResponse(row), row.getVersion());
  }

  @Override
  public TransactionData preview(TransactionRevision before, TransactionField field, String input) {
    if (field == null || input == null || input.isBlank() || input.length() > 500) {
      throw invalid();
    }
    var value = input.strip();
    var old = before.transaction();
    try {
      var data =
          new TransactionData(
              field == TransactionField.TYPE ? TransactionType.valueOf(value) : old.type(),
              field == TransactionField.AMOUNT ? parseAmount(value) : old.amount(),
              field == TransactionField.CATEGORY
                  ? (value.equals("-") ? null : TransactionCategory.valueOf(value))
                  : old.category(),
              field == TransactionField.DESCRIPTION ? nullable(value) : old.description(),
              field == TransactionField.LOCATION ? nullable(value) : old.location(),
              field == TransactionField.DATE ? LocalDate.parse(value) : old.date(),
              field == TransactionField.TIME_OF_DAY
                  ? (value.equals("-") ? null : TimeOfDay.valueOf(value))
                  : old.timeOfDay());
      // Income has no expense category; this derived change is included in the full preview.
      if (field == TransactionField.TYPE && data.type() == TransactionType.INCOME) {
        data =
            new TransactionData(
                data.type(),
                data.amount(),
                null,
                data.description(),
                data.location(),
                data.date(),
                data.timeOfDay());
      }
      validate(data);
      return data;
    } catch (RuntimeException exception) {
      throw invalid();
    }
  }

  @Override
  @Transactional
  public void save(ChatMessage source, UUID id, long version, TransactionData data) {
    var row = owned(source, id);
    requireVersion(row, version);
    validate(data);
    row.revise(data);
    transactions.flush();
  }

  @Override
  @Transactional
  public void delete(ChatMessage source, UUID id, long version) {
    var row = owned(source, id);
    requireVersion(row, version);
    if (!deletedSources.existsByUserIdAndChannelAndChatIdAndMessageId(
        row.getUser().getId(), row.getChannel(), row.getChatId(), row.getMessageId())) {
      deletedSources.saveAndFlush(mapper.toDeletedSource(row));
    }
    drafts.findByTransaction_Id(id).ifPresent(draft -> draft.markTransactionDeleted());
    drafts.flush();
    transactions.delete(row);
    transactions.flush();
  }

  private Transaction owned(ChatMessage source, UUID id) {
    if (source == null || !validator.validate(source).isEmpty() || id == null) {
      throw invalid();
    }
    var identity =
        identities
            .findByChannelAndExternalUserId(source.channel(), source.externalUserId())
            .orElseThrow(TransactionMutationServiceImpl::invalid);
    return transactions
        .findByIdAndUser_IdAndChannelAndChatId(
            id, identity.getUser().getId(), source.channel(), source.chatId())
        .orElseThrow(TransactionMutationServiceImpl::invalid);
  }

  private void validate(TransactionData data) {
    if (data == null
        || !validator.validate(data).isEmpty()
        || data.type() == null
        || data.amount() == null
        || data.date() == null
        || data.date().getYear() < 1
        || data.date().getYear() > 9999
        || (data.type() == TransactionType.INCOME && data.category() != null)) {
      throw invalid();
    }
  }

  private static BigDecimal parseAmount(String value) {
    if (!value.matches("[0-9]{1,17}(\\.[0-9]{1,2})?[kK]?")) {
      throw invalid();
    }
    boolean thousands = value.endsWith("k") || value.endsWith("K");
    var number = new BigDecimal(thousands ? value.substring(0, value.length() - 1) : value);
    return (thousands ? number.multiply(BigDecimal.valueOf(1000)) : number).setScale(2);
  }

  private static String nullable(String value) {
    return value.equals("-") ? null : value;
  }

  private static void requireVersion(Transaction row, long version) {
    if (row.getVersion() != version) {
      throw invalid();
    }
  }

  private static IllegalArgumentException invalid() {
    return new IllegalArgumentException(
        "Khoản không còn khả dụng, đã thay đổi hoặc giá trị không hợp lệ. Hãy xem lại danh sách.");
  }
}
