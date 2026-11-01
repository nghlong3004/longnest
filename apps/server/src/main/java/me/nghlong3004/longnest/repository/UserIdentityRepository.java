package me.nghlong3004.longnest.repository;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import me.nghlong3004.longnest.model.entity.UserIdentity;
import me.nghlong3004.longnest.model.enums.Channel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface UserIdentityRepository extends JpaRepository<UserIdentity, UUID> {

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  Optional<UserIdentity> findByChannelAndExternalUserId(Channel channel, String externalUserId);
}
