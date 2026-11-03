package me.nghlong3004.longnest.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
@Table(name = "transaction_action_inputs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class TransactionActionInput {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  @Column(nullable = false, columnDefinition = "channel_type")
  private Channel channel;

  @Column(name = "external_user_id", nullable = false)
  private String externalUserId;

  @Column(name = "chat_id", nullable = false)
  private String chatId;

  @Column(name = "message_id", nullable = false)
  private String messageId;

  @Builder
  private TransactionActionInput(
      Channel channel, String externalUserId, String chatId, String messageId) {
    this.channel = Objects.requireNonNull(channel, "Channel is required");
    if (externalUserId == null
        || externalUserId.isBlank()
        || chatId == null
        || chatId.isBlank()
        || messageId == null
        || messageId.isBlank()) {
      throw new IllegalArgumentException("Source user, chat and message IDs are required");
    }
    this.externalUserId = externalUserId;
    this.chatId = chatId;
    this.messageId = messageId;
  }
}
