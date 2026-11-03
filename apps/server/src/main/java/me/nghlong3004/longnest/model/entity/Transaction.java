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
import jakarta.persistence.Version;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
import me.nghlong3004.longnest.model.dto.TransactionData;
import me.nghlong3004.longnest.model.enums.Channel;
import me.nghlong3004.longnest.model.enums.TimeOfDay;
import me.nghlong3004.longnest.model.enums.TransactionCategory;
import me.nghlong3004.longnest.model.enums.TransactionType;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SourceType;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "transactions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Transaction {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Version
  @Column(nullable = false)
  private long version;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  @Column(name = "type", nullable = false, columnDefinition = "transaction_type")
  private TransactionType type;

  @NotNull
  @Positive
  @Digits(integer = 17, fraction = 2)
  @Column(name = "amount", nullable = false, precision = 19, scale = 2)
  private BigDecimal amount;

  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  @Column(name = "category", columnDefinition = "transaction_category")
  private TransactionCategory category;

  @Column(name = "description", columnDefinition = "text")
  private String description;

  @Column(name = "location", columnDefinition = "text")
  private String location;

  @Column(name = "date", nullable = false)
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

  @CreationTimestamp(source = SourceType.DB)
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @UpdateTimestamp(source = SourceType.DB)
  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Builder
  private Transaction(
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
      String messageId) {
    this.user = Objects.requireNonNull(user, "User is required");
    this.type = Objects.requireNonNull(type, "Transaction type is required");
    this.amount = validatedAmount(amount);
    validateDetails(type, category, date);
    this.date = date;
    this.channel = Objects.requireNonNull(channel, "Channel is required");
    if (chatId == null || chatId.isBlank() || messageId == null || messageId.isBlank()) {
      throw new IllegalArgumentException("Source chat and message IDs are required");
    }
    this.category = category;
    this.description = description;
    this.location = location;
    this.timeOfDay = timeOfDay;
    this.chatId = chatId;
    this.messageId = messageId;
  }

  public void revise(TransactionData data) {
    Objects.requireNonNull(data, "Transaction details are required");
    validateDetails(data.type(), data.category(), data.date());
    var amount = validatedAmount(data.amount());
    this.type = data.type();
    this.amount = amount;
    this.category = data.category();
    this.description = data.description();
    this.location = data.location();
    this.date = data.date();
    this.timeOfDay = data.timeOfDay();
  }

  private static BigDecimal validatedAmount(BigDecimal amount) {
    var normalized =
        Objects.requireNonNull(amount, "Amount is required").setScale(2, RoundingMode.UNNECESSARY);
    if (normalized.signum() <= 0 || normalized.precision() > 19) {
      throw new IllegalArgumentException("Amount must be positive and fit NUMERIC(19,2)");
    }
    return normalized;
  }

  private static void validateDetails(
      TransactionType type, TransactionCategory category, LocalDate date) {
    Objects.requireNonNull(type, "Transaction type is required");
    Objects.requireNonNull(date, "Transaction date is required");
    if (type == TransactionType.INCOME && category != null) {
      throw new IllegalArgumentException("Income cannot have an expense category");
    }
  }
}
