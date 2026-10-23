package me.nghlong3004.longnest.ledger.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import me.nghlong3004.longnest.ledger.model.PersonalOwner;

@Entity
@Table(name = "ledger_owner_head")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OwnerHead {
  @Id
  @Column(name = "owner_id", length = 200)
  private String owner;

  @Column(nullable = false)
  private long generation;

  @Version
  @Column(name = "row_version", nullable = false)
  private Long rowVersion;

  public OwnerHead(String owner) {
    this.owner = new PersonalOwner(owner).principal();
    this.generation = 0;
  }

  public void advance(long expected, long next) {
    if (generation != expected || next != Math.addExact(expected, 1)) {
      throw new IllegalStateException("Invalid owner generation transition");
    }
    generation = next;
  }
}
