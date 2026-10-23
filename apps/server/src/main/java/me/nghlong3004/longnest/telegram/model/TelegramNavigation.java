package me.nghlong3004.longnest.telegram.model;

import static java.util.stream.Collectors.toUnmodifiableSet;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import me.nghlong3004.longnest.domain.DateRange;
import me.nghlong3004.longnest.domain.RangeFilter;
import me.nghlong3004.longnest.ledger.dto.TransactionPage;
import me.nghlong3004.longnest.ledger.dto.TransactionView;

/** Read-only page identities and cursor history, never cached financial records or action proof. */
@RequiredArgsConstructor
public class TelegramNavigation {
  private static final Duration TTL = Duration.ofMinutes(10);
  private static final int CAPACITY = 100;
  private static final int HISTORY_LIMIT = 100;
  private final Clock clock;
  private final SecureRandom random;
  private final LinkedHashMap<String, Visit> visits = new LinkedHashMap<>();

  public record Route(Optional<String> cursor, List<Optional<String>> previous,
      Optional<String> entryId, Optional<RangeSelection> selection) {
    public Route {
      previous = List.copyOf(previous);
      if (cursor == null || entryId == null || selection == null) {
        throw new IllegalArgumentException("Invalid navigation route");
      }
    }

    public Route(Optional<String> cursor, List<Optional<String>> previous,
        Optional<String> entryId) { this(cursor, previous, entryId, Optional.empty()); }

    public static Route first() {
      return new Route(Optional.empty(), List.of(), Optional.empty());
    }

    public static Route first(RangeSelection selection) {
      return new Route(Optional.empty(), List.of(), Optional.empty(), Optional.of(selection));
    }

    public Route refresh() {
      return new Route(Optional.empty(), List.of(), Optional.empty(), selection);
    }
  }

  /** Read-only query metadata, not an action token or cached financial result. */
  public record RangeSelection(DateRange range, RangeFilter filter) {
    public RangeSelection {
      if (range == null || filter == null) throw new IllegalArgumentException("Invalid query");
    }

    private String code() {
      String type = switch (filter) { case ALL -> "a"; case INCOME -> "i"; case EXPENSE -> "e"; };
      return type + range.from().format(DateTimeFormatter.BASIC_ISO_DATE)
          + range.to().format(DateTimeFormatter.BASIC_ISO_DATE);
    }

    private static RangeSelection parse(String code) {
      if (!code.matches("[aie][0-9]{16}")) throw new IllegalArgumentException("Invalid query");
      RangeFilter filter = switch (code.charAt(0)) {
        case 'a' -> RangeFilter.ALL;
        case 'i' -> RangeFilter.INCOME;
        default -> RangeFilter.EXPENSE;
      };
      return new RangeSelection(new DateRange(
          LocalDate.parse(code.substring(1, 9), DateTimeFormatter.BASIC_ISO_DATE),
          LocalDate.parse(code.substring(9), DateTimeFormatter.BASIC_ISO_DATE)),
          filter);
    }
  }

  public static String pageCallback(TransactionPage page, String token, String action) {
    if (page.summary().isEmpty()) return "b:" + token + ":" + action;
    var summary = page.summary().orElseThrow();
    String compactAction = action.startsWith("s:")
        ? "s" + Long.toString(Long.parseLong(action.substring(2)), 36) : action;
    return "r:" + token + ":" + new RangeSelection(summary.range(), summary.filter()).code()
        + ":" + compactAction;
  }

  private record Visit(long chat, int message, Route route, Optional<String> next,
      Set<String> entries, Map<String, String> revisions, Instant expiresAt) {}

  public record EntryControl(String id, String revision, String command) {}

  public synchronized String open(long chat, TransactionPage page, Route route) {
    Instant now = clock.instant();
    visits.values().removeIf(visit -> !now.isBefore(visit.expiresAt()));
    for (int attempt = 0; attempt < 16; attempt++) {
      byte[] bytes = new byte[16];
      random.nextBytes(bytes);
      String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
      if (!visits.containsKey(token)) {
        if (visits.size() == CAPACITY) visits.pollFirstEntry();
        var history = route.previous();
        var bounded = new Route(route.cursor(),
            history.subList(Math.max(0, history.size() - HISTORY_LIMIT), history.size()),
            Optional.empty(), route.selection());
        visits.put(token, new Visit(chat, 0, bounded, page.nextCursor(),
            page.entries().stream().map(TransactionView::id).collect(toUnmodifiableSet()),
            page.entries().stream().collect(Collectors.toUnmodifiableMap(
                TransactionView::id, TransactionView::revision)), now.plus(TTL)));
        return token;
      }
    }
    throw new IllegalStateException("Navigation unavailable");
  }

  public synchronized String openEntry(long chat, TransactionView entry) {
    Instant now = clock.instant();
    visits.values().removeIf(visit -> !now.isBefore(visit.expiresAt()));
    for (int attempt = 0; attempt < 16; attempt++) {
      byte[] bytes = new byte[16];
      random.nextBytes(bytes);
      String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
      if (!visits.containsKey(token)) {
        if (visits.size() == CAPACITY) visits.pollFirstEntry();
        visits.put(token, new Visit(chat, 0, Route.first(), Optional.empty(),
            Set.of(entry.id()), Map.of(entry.id(), entry.revision()), now.plus(TTL)));
        return token;
      }
    }
    throw new IllegalStateException("Entry navigation unavailable");
  }

  /** Bind only after a successful SDK send; uncertain sends must not authorize controls. */
  public synchronized void bind(String token, int message) {
    Visit visit = visits.get(token);
    if (visit != null && message > 0) {
      visits.put(token, new Visit(visit.chat(), message, visit.route(), visit.next(),
          visit.entries(), visit.revisions(), visit.expiresAt()));
    }
  }

  /** Display-bound read control. An old menu never silently upgrades its expected revision. */
  public synchronized Optional<EntryControl> resolveEntry(String data, long chat, int message) {
    if (data == null || data.length() > 64) return Optional.empty();
    String[] parts = data.split(":", -1);
    if (parts.length != 4 || !parts[0].equals("v")) return Optional.empty();
    Visit visit = visits.get(parts[1]);
    if (visit == null) return Optional.empty();
    if (visit.chat() != chat || visit.message() <= 0 || visit.message() != message) {
      throw new SecurityException("Entry control message mismatch");
    }
    if (!clock.instant().isBefore(visit.expiresAt())) return Optional.empty();
    String revision = visit.revisions().get(parts[3]);
    return revision == null ? Optional.empty()
        : Optional.of(new EntryControl(parts[3], revision, parts[2]));
  }

  /** Missing/expired controls refresh page one; copied controls cannot select another message. */
  public synchronized Optional<Route> resolve(String data, long chat, int message) {
    if (data == null || data.length() > 64) return Optional.empty();
    if (data.startsWith("r:")) return resolveRange(data, chat, message);
    String[] parts = data.split(":", -1);
    if (parts.length < 3 || !parts[0].equals("b")) return Optional.empty();
    Visit visit = visits.get(parts[1]);
    if (visit == null) return Optional.empty();
    if (visit.chat() != chat || visit.message() <= 0 || visit.message() != message) {
      throw new SecurityException("Navigation message mismatch");
    }
    if (!clock.instant().isBefore(visit.expiresAt())) {
      visits.remove(parts[1]);
      return Optional.empty();
    }
    Route route = visit.route();
    if (parts.length == 4 && parts[2].equals("s") && visit.entries().contains(parts[3])) {
      return Optional.of(new Route(route.cursor(), route.previous(), Optional.of(parts[3])));
    }
    if (parts.length != 3) return Optional.empty();
    if (parts[2].equals("n") && visit.next().isPresent()) {
      var previous = new ArrayList<>(route.previous());
      previous.add(route.cursor());
      if (previous.size() > HISTORY_LIMIT) previous.removeFirst();
      return Optional.of(new Route(visit.next(), previous, Optional.empty()));
    }
    if (parts[2].equals("p")) {
      if (route.previous().isEmpty()) return Optional.of(Route.first());
      return Optional.of(new Route(route.previous().getLast(),
          route.previous().subList(0, route.previous().size() - 1), Optional.empty()));
    }
    return Optional.empty();
  }

  private Optional<Route> resolveRange(String data, long chat, int message) {
    String[] parts = data.split(":", -1);
    if (parts.length != 4) return Optional.empty();
    RangeSelection selection;
    try {
      selection = RangeSelection.parse(parts[2]);
    } catch (RuntimeException invalid) {
      return Optional.empty();
    }
    Route first = Route.first(selection);
    Visit visit = visits.get(parts[1]);
    if (visit == null) return Optional.of(first);
    if (visit.chat() != chat || visit.message() <= 0 || visit.message() != message
        || !visit.route().selection().equals(Optional.of(selection))) {
      throw new SecurityException("Range control message/query mismatch");
    }
    if (!clock.instant().isBefore(visit.expiresAt())) {
      visits.remove(parts[1]);
      return Optional.of(first);
    }
    Route route = visit.route();
    String action = parts[3];
    if (action.startsWith("s") && action.length() > 1) {
      try {
        long id = Long.parseLong(action.substring(1), 36);
        String key = Long.toString(id);
        if (id > 0 && visit.entries().contains(key)) {
          return Optional.of(new Route(route.cursor(), route.previous(), Optional.of(key),
              route.selection()));
        }
      } catch (NumberFormatException invalid) {
        return Optional.of(first);
      }
    }
    if (action.equals("n") && visit.next().isPresent()) {
      var previous = new ArrayList<>(route.previous());
      previous.add(route.cursor());
      if (previous.size() > HISTORY_LIMIT) previous.removeFirst();
      return Optional.of(new Route(visit.next(), previous, Optional.empty(), route.selection()));
    }
    if (action.equals("p") && !route.previous().isEmpty()) {
      return Optional.of(new Route(route.previous().getLast(),
          route.previous().subList(0, route.previous().size() - 1), Optional.empty(),
          route.selection()));
    }
    return Optional.of(first);
  }
}
