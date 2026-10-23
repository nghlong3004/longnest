package me.nghlong3004.longnest.ledger.pagination;

import static me.nghlong3004.longnest.ledger.model.LedgerException.Code.INVALID_CURSOR;
import static me.nghlong3004.longnest.ledger.model.LedgerException.Code.LIMIT_REACHED;
import static me.nghlong3004.longnest.ledger.model.LedgerException.Code.STALE_CURSOR;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import me.nghlong3004.longnest.domain.DateRange;
import me.nghlong3004.longnest.domain.RangeFilter;
import me.nghlong3004.longnest.ledger.model.LedgerException;

/** One bounded policy for recent and range queries; tokens have no durable state. */
@RequiredArgsConstructor
public class LedgerCursors {
  private static final Duration TTL = Duration.ofMinutes(10);
  private static final int CAPACITY = 100;
  private final Clock clock;
  private final SecureRandom random;
  private final LinkedHashMap<String, Cursor> tokens = new LinkedHashMap<>();

  public record Key(Instant createdAt, long id, LocalDate date) {
    public Key {
      if (id <= 0 || (createdAt == null) == (date == null)) {
        throw new IllegalArgumentException("Invalid keyset key");
      }
    }

    public Key(Instant createdAt, long id) { this(createdAt, id, null); }
  }

  private record Cursor(String owner, long generation, Optional<DateRange> range,
      RangeFilter filter, Key key, Instant expiresAt) {}

  public synchronized Optional<Key> resolve(String owner, long generation, Optional<String> token) {
    return resolve(owner, generation, Optional.empty(), RangeFilter.ALL, token);
  }

  public synchronized Optional<Key> resolve(String owner, long generation,
      Optional<DateRange> range, RangeFilter filter, Optional<String> token) {
    if (token == null) {
      throw new LedgerException(INVALID_CURSOR);
    }
    if (token.isEmpty()) {
      return Optional.empty();
    }
    var cursor = tokens.get(token.orElseThrow());
    if (cursor == null || !owner.equals(cursor.owner()) || !cursor.range().equals(range)
        || cursor.filter() != filter
        || !clock.instant().isBefore(cursor.expiresAt())) {
      throw new LedgerException(INVALID_CURSOR);
    }
    if (generation != cursor.generation()) {
      throw new LedgerException(STALE_CURSOR);
    }
    return Optional.of(cursor.key());
  }

  public synchronized String issue(String owner, long generation, Key key) {
    return issue(owner, generation, Optional.empty(), RangeFilter.ALL, key);
  }

  public synchronized String issue(String owner, long generation, Optional<DateRange> range,
      RangeFilter filter, Key key) {
    if (range == null || filter == null || range.isPresent() != (key.date() != null)) {
      throw new LedgerException(INVALID_CURSOR);
    }
    Instant now = clock.instant();
    tokens.values().removeIf(cursor -> !now.isBefore(cursor.expiresAt()));
    for (int attempt = 0; attempt < 16; attempt++) {
      byte[] bytes = new byte[16];
      random.nextBytes(bytes);
      String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
      if (!tokens.containsKey(token)) {
        if (tokens.size() == CAPACITY) {
          tokens.pollFirstEntry();
        }
        tokens.put(token, new Cursor(owner, generation, range, filter, key, now.plus(TTL)));
        return token;
      }
    }
    throw new LedgerException(LIMIT_REACHED);
  }
}
