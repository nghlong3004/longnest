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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import me.nghlong3004.longnest.model.enums.Channel;
import me.nghlong3004.longnest.model.enums.DraftStatus;
import me.nghlong3004.longnest.model.enums.TimeOfDay;
import me.nghlong3004.longnest.model.enums.TransactionCategory;
import me.nghlong3004.longnest.model.enums.TransactionType;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.OptimisticLock;
import org.hibernate.annotations.SourceType;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "transaction_drafts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class TransactionDraft {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  @Column(name = "type", columnDefinition = "transaction_type")
  private TransactionType type;

  @Positive
  @Digits(integer = 17, fraction = 2)
  @Column(name = "amount", precision = 19, scale = 2)
  private BigDecimal amount;

  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  @Column(name = "category", columnDefinition = "transaction_category")
  private TransactionCategory category;

  @Column(name = "description", columnDefinition = "text")
  private String description;

  @Column(name = "location", columnDefinition = "text")
  private String location;

  @Column(name = "date")
  private LocalDate date;

  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  @Column(name = "time_of_day", columnDefinition = "time_of_day")
  private TimeOfDay timeOfDay;

  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  @Column(name = "channel", nullable = false, columnDefinition = "channel_type")
  private Channel channel;

  @NotBlank
  @Column(name = "chat_id", nullable = false, columnDefinition = "text")
  private String chatId;

  @NotBlank
  @Column(name = "message_id", nullable = false, columnDefinition = "text")
  private String messageId;

  @NotBlank
  @Column(name = "original_text", nullable = false, columnDefinition = "text")
  private String originalText;

  @Column(name = "original_sent_at", nullable = false, updatable = false)
  private Instant originalSentAt;

  @Version
  @Column(name = "version", nullable = false)
  private long version;

  @Column(name = "question_text", columnDefinition = "text")
  private String questionText;

  @OptimisticLock(excluded = true)
  @Column(name = "question_message_id", columnDefinition = "text")
  private String questionMessageId;

  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  @Column(name = "status", nullable = false, columnDefinition = "draft_status")
  private DraftStatus status = DraftStatus.PENDING;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "transaction_id", unique = true)
  private Transaction transaction;

  @CreationTimestamp(source = SourceType.DB)
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @UpdateTimestamp(source = SourceType.DB)
  @OptimisticLock(excluded = true)
  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Builder
  private TransactionDraft(
      User user,
      TransactionType type,
      BigDecimal amount,
      TransactionCategory category,
      String description,
      String location,
      LocalDate date,
      TimeOfDay timeOfDay,
      Channel channel,
      String chatId,
      String messageId,
      String originalText,
      Instant originalSentAt) {
    this.user = Objects.requireNonNull(user, "User is required");
    this.channel = Objects.requireNonNull(channel, "Channel is required");
    if (chatId == null || chatId.isBlank() || messageId == null || messageId.isBlank()) {
      throw new IllegalArgumentException("Source chat and message IDs are required");
    }
    if (originalText == null || originalText.isBlank()) {
      throw new IllegalArgumentException("Original message text is required");
    }
    this.amount = normalizeAmount(amount);
    this.originalSentAt = Objects.requireNonNull(originalSentAt, "Original timestamp is required");
    this.type = type;
    this.category = category;
    this.description = description;
    this.location = location;
    this.date = date;
    this.timeOfDay = timeOfDay;
    this.chatId = chatId;
    this.messageId = messageId;
    this.originalText = originalText;
  }

  /** Applies supplied details while preserving known values and the draft's lifecycle. */
  public void revise(
      TransactionType type,
      BigDecimal amount,
      TransactionCategory category,
      String description,
      String location,
      LocalDate date,
      TimeOfDay timeOfDay) {
    requirePending();
    var normalizedAmount = normalizeAmount(amount);
    if (type != null) {
      this.type = type;
    }
    if (normalizedAmount != null) {
      this.amount = normalizedAmount;
    }
    if (category != null) {
      this.category = category;
    }
    if (this.type == TransactionType.INCOME) {
      this.category = null;
    }
    if (description != null) {
      this.description = description;
    }
    if (location != null) {
      this.location = location;
    }
    if (date != null) {
      this.date = date;
    }
    if (timeOfDay != null) {
      this.timeOfDay = timeOfDay;
    }
  }

  /** Records a new question before the transport has assigned its message ID. */
  public void askForClarification(String questionText) {
    requirePending();
    if (questionText == null || questionText.isBlank()) {
      throw new IllegalArgumentException("Question text is required");
    }
    this.questionText = questionText;
    this.questionMessageId = null;
  }

  public void askForClarification(String questionText, String questionMessageId) {
    requirePending();
    if (questionText == null
        || questionText.isBlank()
        || questionMessageId == null
        || questionMessageId.isBlank()) {
      throw new IllegalArgumentException("Question text and message ID are required");
    }
    this.questionText = questionText;
    this.questionMessageId = questionMessageId;
  }

  public void confirm(Transaction transaction) {
    requirePending();
    Objects.requireNonNull(transaction, "Transaction is required");
    if (transaction.getId() == null
        || user.getId() == null
        || !user.getId().equals(transaction.getUser().getId())) {
      throw new IllegalArgumentException(
          "Transaction must be persisted and belong to the same user");
    }
    this.transaction = transaction;
    this.status = DraftStatus.CONFIRMED;
  }

  public void cancel() {
    requirePending();
    this.status = DraftStatus.CANCELLED;
  }

  private void requirePending() {
    if (status != DraftStatus.PENDING) {
      throw new IllegalStateException("Only pending drafts can be changed");
    }
  }

  private static BigDecimal normalizeAmount(BigDecimal amount) {
    if (amount == null) {
      return null;
    }
    var normalized = amount.setScale(2, RoundingMode.UNNECESSARY);
    if (normalized.signum() <= 0 || normalized.precision() > 19) {
      throw new IllegalArgumentException("Amount must be positive and fit NUMERIC(19,2)");
    }
    return normalized;
  }
}
