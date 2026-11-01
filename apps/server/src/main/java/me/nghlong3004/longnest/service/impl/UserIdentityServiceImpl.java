package me.nghlong3004.longnest.service.impl;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import me.nghlong3004.longnest.model.dto.ChatMessage;
import me.nghlong3004.longnest.model.entity.User;
import me.nghlong3004.longnest.model.entity.UserIdentity;
import me.nghlong3004.longnest.repository.UserIdentityRepository;
import me.nghlong3004.longnest.repository.UserRepository;
import me.nghlong3004.longnest.service.UserIdentityService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(propagation = Propagation.MANDATORY)
public class UserIdentityServiceImpl implements UserIdentityService {

  private final UserRepository users;
  private final UserIdentityRepository identities;

  @Override
  public Optional<UserIdentity> find(ChatMessage message) {
    return identities.findByChannelAndExternalUserId(message.channel(), message.externalUserId());
  }

  @Override
  public UserIdentity resolve(ChatMessage message) {
    return find(message)
        .orElseGet(() ->
            identities.saveAndFlush(
                UserIdentity.builder()
                    .user(users.save(User.builder().build()))
                    .channel(message.channel())
                    .externalUserId(message.externalUserId())
                    .build()));
  }
}
