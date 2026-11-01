package me.nghlong3004.longnest.service;

import java.util.Optional;
import me.nghlong3004.longnest.model.dto.ChatMessage;
import me.nghlong3004.longnest.model.entity.UserIdentity;
import org.springframework.dao.DataAccessException;
import org.springframework.transaction.IllegalTransactionStateException;

/** Resolves trusted channel identities within the caller's database transaction. */
public interface UserIdentityService {

  /**
   * Locks an existing sender identity without creating records.
   *
   * @param message authorized sender and channel metadata
   * @return the existing, locked identity, if any
   * @throws IllegalTransactionStateException without an active transaction
   */
  Optional<UserIdentity> find(ChatMessage message);

  /**
   * Locks the sender's identity, creating its user and identity atomically when absent.
   *
   * @param message authorized sender and channel metadata
   * @return the resolved identity owned by the sender
   * @throws DataAccessException if resolution or creation fails
   */
  UserIdentity resolve(ChatMessage message);
}
