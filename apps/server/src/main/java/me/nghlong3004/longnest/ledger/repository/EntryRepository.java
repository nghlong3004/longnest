package me.nghlong3004.longnest.ledger.repository;

import java.time.Instant;
import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import me.nghlong3004.longnest.ledger.model.entity.LedgerEntry;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
@Profile("ledger")
public interface EntryRepository extends JpaRepository<LedgerEntry, Long> {
  interface TotalsProjection {
    BigDecimal getIncomeTotal();
    BigDecimal getIncomeCount();
    BigDecimal getExpenseTotal();
    BigDecimal getExpenseCount();
  }

  @Query(value = """
      select coalesce(sum(amount_minor) filter (where transaction_type = 'INCOME'), 0) as "incomeTotal",
      cast(count(*) filter (where transaction_type = 'INCOME') as numeric) as "incomeCount",
      coalesce(sum(amount_minor) filter (where transaction_type = 'EXPENSE'), 0) as "expenseTotal",
      cast(count(*) filter (where transaction_type = 'EXPENSE') as numeric) as "expenseCount"
      from ledger_entry where owner_id = :owner and deleted = false
      and transaction_date between :fromDate and :toDate
      and (:typeFilter = 'ALL' or transaction_type = :typeFilter)
      """, nativeQuery = true)
  TotalsProjection totals(@Param("owner") String owner, @Param("fromDate") LocalDate from,
      @Param("toDate") LocalDate to, @Param("typeFilter") String filter);

  @Query("""
      select e from LedgerEntry e where e.owner = :owner and e.deleted = false
      and e.values.transactionDate between :fromDate and :toDate
      and (:typeFilter = 'ALL' or cast(e.values.type as string) = :typeFilter)
      and (:beforeDate is null or e.values.transactionDate < :beforeDate
        or (e.values.transactionDate = :beforeDate and e.id < :beforeId))
      order by e.values.transactionDate desc, e.id desc
      """)
  List<LedgerEntry> range(@Param("owner") String owner, @Param("fromDate") LocalDate from,
      @Param("toDate") LocalDate to, @Param("typeFilter") String filter,
      @Param("beforeDate") LocalDate beforeDate, @Param("beforeId") long beforeId, Pageable limit);

  Optional<LedgerEntry> findByOwnerAndId(String owner, long id);

  List<LedgerEntry> findTop11ByOwnerAndDeletedFalseOrderByCreatedAtDescIdDesc(String owner);

  @Query("""
      select e from LedgerEntry e where e.owner = :owner and e.deleted = false
      and (e.createdAt < :createdAt or (e.createdAt = :createdAt and e.id < :id))
      order by e.createdAt desc, e.id desc
      """)
  List<LedgerEntry> after(@Param("owner") String owner, @Param("createdAt") Instant createdAt,
      @Param("id") long id, Pageable limit);
}
