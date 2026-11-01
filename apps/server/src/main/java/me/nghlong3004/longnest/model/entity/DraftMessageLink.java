package me.nghlong3004.longnest.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import me.nghlong3004.longnest.model.enums.Channel;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "draft_message_links")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class DraftMessageLink {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "draft_id", nullable = false)
  private TransactionDraft draft;

  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  @Column(name = "channel", nullable = false, columnDefinition = "channel_type")
  private Channel channel;

  @Column(name = "chat_id", nullable = false, columnDefinition = "text")
  private String chatId;

  @Column(name = "message_id", nullable = false, columnDefinition = "text")
  private String messageId;

  @Builder
  private DraftMessageLink(
      TransactionDraft draft, Channel channel, String chatId, String messageId) {
    this.draft = Objects.requireNonNull(draft, "Draft is required");
    this.channel = Objects.requireNonNull(channel, "Channel is required");
    if (chatId == null || chatId.isBlank() || messageId == null || messageId.isBlank()) {
      throw new IllegalArgumentException("Source chat and message IDs are required");
    }
    this.chatId = chatId;
    this.messageId = messageId;
  }
}
