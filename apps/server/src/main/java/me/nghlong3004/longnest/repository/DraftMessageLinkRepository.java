package me.nghlong3004.longnest.repository;

import java.util.Optional;
import java.util.UUID;
import me.nghlong3004.longnest.model.entity.DraftMessageLink;
import me.nghlong3004.longnest.model.enums.Channel;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DraftMessageLinkRepository extends JpaRepository<DraftMessageLink, UUID> {

  Optional<DraftMessageLink> findByDraft_User_IdAndChannelAndChatIdAndMessageId(
      UUID userId, Channel channel, String chatId, String messageId);
}
