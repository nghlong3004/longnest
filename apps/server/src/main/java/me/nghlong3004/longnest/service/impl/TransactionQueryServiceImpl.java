package me.nghlong3004.longnest.service.impl;

import jakarta.validation.Validator;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import lombok.RequiredArgsConstructor;
import me.nghlong3004.longnest.exception.ResourceException;
import me.nghlong3004.longnest.mapper.TransactionMapper;
import me.nghlong3004.longnest.model.dto.ChatMessage;
import me.nghlong3004.longnest.model.dto.TransactionCursor;
import me.nghlong3004.longnest.model.dto.TransactionFilter;
import me.nghlong3004.longnest.model.dto.TransactionPage;
import me.nghlong3004.longnest.model.dto.TransactionQuery;
import me.nghlong3004.longnest.model.dto.TransactionTotals;
import me.nghlong3004.longnest.model.enums.ErrorCode;
import me.nghlong3004.longnest.model.enums.QueryPeriod;
import me.nghlong3004.longnest.repository.TransactionRepository;
import me.nghlong3004.longnest.repository.UserIdentityRepository;
import me.nghlong3004.longnest.service.TransactionQueryService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TransactionQueryServiceImpl implements TransactionQueryService {

  private static final int PAGE_SIZE = 5;
  private static final Sort ORDER = Sort.by(Sort.Direction.DESC, "date", "id");
  private final UserIdentityRepository identities;
  private final TransactionRepository transactions;
  private final TransactionMapper mapper;
  private final Validator validator;
  private final ZoneId ledgerTimeZone;

  @Override
  @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
  public TransactionPage view(
      ChatMessage message, TransactionQuery query, TransactionCursor cursor) {
    if (message == null
        || !validator.validate(message).isEmpty()
        || query == null
        || !validator.validate(query).isEmpty()
        || (cursor != null && !validator.validate(cursor).isEmpty())) {
      throw invalidQuery();
    }
    var filter = resolve(query, message.sentAt().atZone(ledgerTimeZone).toLocalDate());
    if (cursor != null
        && (cursor.date().isBefore(filter.from()) || cursor.date().isAfter(filter.to()))) {
      throw invalidQuery();
    }
    var identity =
        identities.findFirstByChannelAndExternalUserId(message.channel(), message.externalUserId());
    if (identity.isEmpty()) {
      return new TransactionPage(
          filter, List.of(), new TransactionTotals(BigDecimal.ZERO, 0), null);
    }
    var userId = identity.get().getUser().getId();
    var totals = transactions.totals(userId, filter.type(), filter.from(), filter.to());
    var rows =
        cursor == null
            ? transactions.findByUser_IdAndTypeAndDateBetween(
                userId,
                filter.type(),
                filter.from(),
                filter.to(),
                PageRequest.of(0, PAGE_SIZE + 1, ORDER))
            : transactions.findAfterCursor(
                userId,
                filter.type(),
                filter.from(),
                filter.to(),
                cursor.date(),
                cursor.id(),
                PageRequest.of(0, PAGE_SIZE + 1));
    var items = rows.stream().limit(PAGE_SIZE).map(mapper::toResponse).toList();
    TransactionCursor next = null;
    if (rows.size() > PAGE_SIZE) {
      var last = items.getLast();
      next = new TransactionCursor(last.date(), last.id());
    }
    return new TransactionPage(filter, items, totals, next);
  }

  private TransactionFilter resolve(TransactionQuery query, LocalDate reference) {
    if (query.period() != QueryPeriod.CUSTOM
        && query.period() != QueryPeriod.SINCE_DATE
        && (query.from() != null || query.to() != null)) {
      throw invalidQuery();
    }
    LocalDate from;
    LocalDate to;
    switch (query.period()) {
      case TODAY -> {
        from = reference;
        to = reference;
      }
      case YESTERDAY -> {
        from = reference.minusDays(1);
        to = from;
      }
      case SINCE_YESTERDAY -> {
        from = reference.minusDays(1);
        to = reference;
      }
      case THIS_MONTH -> {
        from = reference.withDayOfMonth(1);
        to = reference;
      }
      case LAST_MONTH -> {
        var month = YearMonth.from(reference).minusMonths(1);
        from = month.atDay(1);
        to = month.atEndOfMonth();
      }
      case SINCE_LAST_MONTH -> {
        from = reference.minusMonths(1).withDayOfMonth(1);
        to = reference;
      }
      case SINCE_DATE -> {
        if (query.to() != null) {
          throw invalidQuery();
        }
        from = query.from();
        to = reference;
      }
      case CUSTOM -> {
        from = query.from();
        to = query.to();
      }
      default -> throw invalidQuery();
    }
    if (from == null
        || to == null
        || from.isAfter(to)
        || from.getYear() < 1
        || to.getYear() > 9999) {
      throw invalidQuery();
    }
    return new TransactionFilter(query.type(), from, to);
  }

  private ResourceException invalidQuery() {
    return new ResourceException(ErrorCode.QUERY_INVALID_PARAMETERS);
  }
}
