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
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import me.nghlong3004.longnest.model.enums.Channel;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SourceType;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "user_identities")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class UserIdentity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  @Column(name = "channel", nullable = false, columnDefinition = "channel_type")
  private Channel channel;

  @NotBlank
  @Column(name = "external_user_id", nullable = false, columnDefinition = "text")
  private String externalUserId;

  @CreationTimestamp(source = SourceType.DB)
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @UpdateTimestamp(source = SourceType.DB)
  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Builder
  private UserIdentity(User user, Channel channel, String externalUserId) {
    this.user = Objects.requireNonNull(user, "User is required");
    this.channel = Objects.requireNonNull(channel, "Channel is required");
    if (externalUserId == null || externalUserId.isBlank()) {
      throw new IllegalArgumentException("External user ID is required");
    }
    this.externalUserId = externalUserId;
  }
}
