package me.nghlong3004.longnest.ledger.repository;

import java.util.List;
import me.nghlong3004.longnest.ledger.model.entity.CreateInbox;
import me.nghlong3004.longnest.ledger.model.entity.InboxId;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
@Profile("ledger")
public interface InboxRepository extends JpaRepository<CreateInbox, InboxId> {
  @Query("select i.id.operationId from CreateInbox i where i.id.owner = :owner "
      + "and i.receiptEntryId is null and i.id.operationId like concat(:prefix, '%') "
      + "order by i.id.operationId")
  List<String> pending(@Param("owner") String owner, @Param("prefix") String prefix,
      Pageable limit);
}
