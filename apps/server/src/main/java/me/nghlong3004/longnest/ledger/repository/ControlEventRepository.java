package me.nghlong3004.longnest.ledger.repository;

import me.nghlong3004.longnest.ledger.model.entity.ControlEvent;
import me.nghlong3004.longnest.ledger.model.entity.InboxId;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
@Profile("ledger")
public interface ControlEventRepository extends JpaRepository<ControlEvent, InboxId> {}
