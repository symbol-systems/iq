package systems.symbol.rdf4j.fedx.materialization;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;

/**
 * Configuration for data materialization strategy per endpoint.
 *
 * <p>Determines whether and how query results are cached. Immutable value object.
 */
public final class DataMaterializationPolicy {

  /**
   * Named strategies.
   */
  public enum Strategy {
    /** Always fetch fresh data from source (no caching, default). */
    REALTIME("realtime"),

    /** Cache results for 5 minutes. */
    CACHED_5M("cached_5m"),

    /** Cache results for 1 hour. */
    CACHED_1H("cached_1h"),

    /** Cache results for 24 hours. */
    CACHED_24H("cached_24h"),

    /** Write-through: fetch live, cache result for next query. */
    WRITE_THROUGH("write_through");

    private final String id;

    Strategy(String id) {
      this.id = id;
    }

    public String id() {
      return id;
    }

    /** Parse from string (case-insensitive). */
    public static Strategy fromString(String str) {
      if (str == null) {
        return REALTIME; // default
      }
      for (Strategy s : values()) {
        if (s.id.equalsIgnoreCase(str)) {
          return s;
        }
      }
      return REALTIME; // default
    }
  }

  private final Strategy strategy;
  private final Duration ttl; // used for CACHED_* strategies

  /**
   * Construct with a strategy and TTL.
   *
   * @param strategy the caching strategy
   * @param ttl cache TTL for CACHED_* strategies
   */
  public DataMaterializationPolicy(Strategy strategy, Duration ttl) {
    this.strategy = Objects.requireNonNull(strategy, "strategy cannot be null");
    this.ttl = Objects.requireNonNull(ttl, "ttl cannot be null");
  }

  public Strategy strategy() {
    return strategy;
  }

  public Duration ttl() {
    return ttl;
  }

  /**
   * Create policy from endpoint metadata.
   *
   * <p>Looks for keys:
   * - "data.strategy" => strategy name (realtime, cached_5m, cached_1h, cached_24h, write_through)
   * - "data.ttl.minutes" => custom TTL in minutes (used for CACHED_* strategies)
   *
   * <p>Defaults to REALTIME if not specified.
   *
   * @param endpointMetadata endpoint properties map
   * @return policy derived from metadata, or default
   */
  public static DataMaterializationPolicy fromMetadata(Map<String, String> endpointMetadata) {
    if (endpointMetadata == null || endpointMetadata.isEmpty()) {
      return new DataMaterializationPolicy(Strategy.REALTIME, Duration.ZERO);
    }

    String strategyStr = endpointMetadata.get("data.strategy");
    Strategy strategy = Strategy.fromString(strategyStr);

    Duration ttl = Duration.ZERO;
    String ttlMinutesStr = endpointMetadata.get("data.ttl.minutes");

    if (ttlMinutesStr != null && !ttlMinutesStr.isEmpty()) {
      try {
        long minutes = Long.parseLong(ttlMinutesStr);
        ttl = Duration.ofMinutes(Math.max(0, minutes));
      } catch (NumberFormatException ignored) {
        // Use default (zero for realtime)
      }
    }

    // Assign default TTLs if not specified
    if (ttl.isZero() && strategy != Strategy.REALTIME) {
      ttl = switch (strategy) {
        case CACHED_5M -> Duration.ofMinutes(5);
        case CACHED_1H -> Duration.ofHours(1);
        case CACHED_24H -> Duration.ofHours(24);
        case WRITE_THROUGH -> Duration.ofMinutes(5);
        case REALTIME -> Duration.ZERO;
      };
    }

    return new DataMaterializationPolicy(strategy, ttl);
  }

  @Override
  public String toString() {
    return "DataMaterializationPolicy{" + "strategy=" + strategy + ", ttl=" + ttl + '}';
  }
}
