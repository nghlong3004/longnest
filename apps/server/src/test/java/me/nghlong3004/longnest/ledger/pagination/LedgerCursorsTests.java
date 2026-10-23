package me.nghlong3004.longnest.ledger.pagination;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Optional;
import me.nghlong3004.longnest.domain.DateRange;
import me.nghlong3004.longnest.domain.RangeFilter;
import me.nghlong3004.longnest.ledger.model.LedgerException;
import org.junit.jupiter.api.Test;

class LedgerCursorsTests {
  private static final Instant NOW = Instant.parse("2026-10-21T00:00:00Z");
  private static final LedgerCursors.Key KEY = new LedgerCursors.Key(NOW, 42);

  private static void fails(Runnable action, LedgerException.Code code) {
    assertThatThrownBy(action::run).isInstanceOfSatisfying(LedgerException.class,
        failure -> assertThat(failure.getCode()).isEqualTo(code));
  }

  @Test
  void rangeTokensShareRecentLifecycleButNeverCrossScopeRangeTypeOrOwner() {
    var cursors = new LedgerCursors(Clock.fixed(NOW, ZoneOffset.UTC), new SecureRandom());
    var range = Optional.of(DateRange.parse("2026-10-01", "2026-10-31"));
    var key = new LedgerCursors.Key(null, 42, range.orElseThrow().to());
    String token = cursors.issue("personal", 4, range, RangeFilter.EXPENSE, key);
    assertThat(cursors.resolve("personal", 4, range, RangeFilter.EXPENSE, Optional.of(token)))
        .contains(key);
    fails(() -> cursors.resolve("personal", 4, range, RangeFilter.FOOD, Optional.of(token)),
        LedgerException.Code.INVALID_CURSOR);
    String foodToken = cursors.issue("personal", 4, range, RangeFilter.FOOD, key);
    assertThat(cursors.resolve("personal", 4, range, RangeFilter.FOOD, Optional.of(foodToken)))
        .contains(key);
    fails(() -> cursors.resolve("personal", 4, range, RangeFilter.EXPENSE,
        Optional.of(foodToken)), LedgerException.Code.INVALID_CURSOR);
    fails(() -> cursors.resolve("personal", 4, Optional.of(token)), LedgerException.Code.INVALID_CURSOR);
    fails(() -> cursors.resolve("foreign", 4, range, RangeFilter.EXPENSE, Optional.of(token)),
        LedgerException.Code.INVALID_CURSOR);
    fails(() -> cursors.resolve("personal", 4,
        Optional.of(DateRange.parse("2026-10-02", "2026-10-31")), RangeFilter.EXPENSE,
        Optional.of(token)), LedgerException.Code.INVALID_CURSOR);
  }

  @Test
  void tokensAreOpaque128BitAndBoundToOwnerGenerationAndSavedKey() {
    var cursors = new LedgerCursors(Clock.fixed(NOW, ZoneOffset.UTC), new SecureRandom());
    String token = cursors.issue("personal", 4, KEY);
    assertThat(token).matches("[A-Za-z0-9_-]{22}");
    assertThat(Base64.getUrlDecoder().decode(token)).hasSize(16);
    assertThat(cursors.resolve("personal", 4, Optional.of(token))).contains(KEY);
    fails(() -> cursors.resolve("personal", 5, Optional.of(token)),
        LedgerException.Code.STALE_CURSOR);
    fails(() -> cursors.resolve("foreign", 5, Optional.of(token)),
        LedgerException.Code.INVALID_CURSOR);
    fails(() -> cursors.resolve("personal", 4, Optional.of(token + "x")),
        LedgerException.Code.INVALID_CURSOR);
    assertThat(cursors.resolve("personal", 4, Optional.empty())).isEmpty();
    fails(() -> cursors.resolve("personal", 4, null), LedgerException.Code.INVALID_CURSOR);
  }

  @Test
  void ttlExpiresAtTenMinutesNotAfterAndRestartLosesTokens() {
    var clock = new MutableClock();
    var cursors = new LedgerCursors(clock, new SecureRandom());
    String token = cursors.issue("personal", 4, KEY);
    clock.now = NOW.plusSeconds(599);
    assertThat(cursors.resolve("personal", 4, Optional.of(token))).contains(KEY);
    clock.now = NOW.plusSeconds(600);
    fails(() -> cursors.resolve("personal", 4, Optional.of(token)),
        LedgerException.Code.INVALID_CURSOR);
    var restarted = new LedgerCursors(clock, new SecureRandom());
    fails(() -> restarted.resolve("personal", 4, Optional.of(token)),
        LedgerException.Code.INVALID_CURSOR);
  }

  @Test
  void capacityEvictsOldestAtOneHundredAndPreservesLaterTokens() {
    var cursors = new LedgerCursors(Clock.fixed(NOW, ZoneOffset.UTC), new SecureRandom());
    String first = cursors.issue("personal", 4, KEY);
    String second = cursors.issue("personal", 4, KEY);
    for (int i = 2; i <= 100; i++) {
      cursors.issue("personal", 4, KEY);
    }
    fails(() -> cursors.resolve("personal", 4, Optional.of(first)),
        LedgerException.Code.INVALID_CURSOR);
    assertThat(cursors.resolve("personal", 4, Optional.of(second))).contains(KEY);
  }

  private static final class MutableClock extends Clock {
    private Instant now = NOW;

    @Override
    public ZoneId getZone() {
      return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
      return Clock.fixed(now, zone);
    }

    @Override
    public Instant instant() {
      return now;
    }
  }
}
