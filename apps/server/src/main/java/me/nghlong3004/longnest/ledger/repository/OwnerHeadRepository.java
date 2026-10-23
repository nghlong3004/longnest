package me.nghlong3004.longnest.ledger.repository;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import me.nghlong3004.longnest.ledger.model.entity.OwnerHead;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
@Profile("ledger")
public interface OwnerHeadRepository extends JpaRepository<OwnerHead, String> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select h from OwnerHead h where h.owner = :owner")
  Optional<OwnerHead> lockOwner(@Param("owner") String owner);
}
