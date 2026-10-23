package me.nghlong3004.longnest.ledger.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import me.nghlong3004.longnest.ledger.model.entity.LedgerControl;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
@Profile("ledger")
public interface ControlRepository extends JpaRepository<LedgerControl, String> {
  Optional<LedgerControl> findByOwnerAndId(String owner, String id);

  Optional<LedgerControl> findByOwnerAndRequestId(String owner, String requestId);

  List<LedgerControl> findByOwnerAndScopeAndDisplayMessageAndKind(
      String owner, String scope, long displayMessage, LedgerStore.ControlKind kind);

  long countByOwnerAndStatusAndExpiresAtAfter(String owner, LedgerStore.ControlStatus status,
      Instant now);

  List<LedgerControl> findByOwnerAndScopeAndTargetIdAndStatusAndKindNot(
      String owner, String scope, long targetId, LedgerStore.ControlStatus status,
      LedgerStore.ControlKind kind);
}
