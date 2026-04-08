package systems.symbol.rdf4j.fedx.materialization;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Configuration for schema materialization strategy per endpoint.
 *
 * <p>Determines whether and how schema is cached. Immutable value object.
 */
public final class SchemaMaterializationPolicy {

  /**
   * Named strategies.
   */
  public enum Strategy {
    /** Always re-introspect from source (no caching). */
    REALTIME("realtime", Duration.ZERO),

    /** Cache schema for 1 hour (default). */
    CACHED_1H("cached_1h", Duration.ofHours(1)),

    /** Cache schema for 24 hours. */
    CACHED_24H("cached_24h", Duration.ofHours(24)),

    /** Cache schema for 7 days. */
    CACHED_7D("cached_7d", Duration.ofDays(7));

    private final String id;
    private final Duration defaultTtl;

    Strategy(String id, Duration defaultTtl) {
      this.id = id;
      this.defaultTtl = defaultTtl;
    }

    public String id() {
      return id;
    }

    public Duration defaultTtl() {
      return defaultTtl;
    }

    /** Parse from string (case-insensitive). */
    public static Strategy fromString(String str) {
      if (str == null) {
        return CACHED_1H; // default
      }
      for (Strategy s : values()) {
        if (s.id.equalsIgnoreCase(str)) {
          return s;
        }
      }
      return CACHED_1H; // default
    }
  }

  private final Strategy strategy;
  private final Duration ttl; // used only if strategy is CACHED_*

  /**
   * Construct with a strategy.
   *
   * @param strategy the caching strategy
   */
  public SchemaMaterializationPolicy(Strategy strategy) {
    this(strategy, strategy.defaultTtl());
  }

  /**
   * Construct with a strategy and custom TTL.
   *
   * @param strategy the caching strategy
   * @param ttl custom TTL (used only for CACHED_* strategies)
   */
  public SchemaMaterializationPolicy(Strategy strategy, Duration ttl) {
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
   * - "schema.strategy" => strategy name (realtime, cached_1h, cached_24h, cached_7d)
   * - "schema.ttl.minutes" => custom TTL in minutes (overrides default)
   *
   * <p>Defaults to CACHED_1H if not specified.
   *
   * @param endpointMetadata endpoint properties map
   * @return policy derived from metadata, or default
   */
  public static SchemaMaterializationPolicy fromMetadata(Map<String, String> endpointMetadata) {
    if (endpointMetadata == null || endpointMetadata.isEmpty()) {
      return new SchemaMaterializationPolicy(Strategy.CACHED_1H);
    }

    String strategyStr = endpointMetadata.get("schema.strategy");
    Strategy strategy = Strategy.fromString(strategyStr);

    String ttlMinutesStr = endpointMetadata.get("schema.ttl.minutes");
    Duration ttl = strategy.defaultTtl();

    if (ttlMinutesStr != null && !ttlMinutesStr.isEmpty()) {
      try {
        long minutes = Long.parseLong(ttlMinutesStr);
        ttl = Duration.ofMinutes(Math.max(0, minutes));
      } catch (NumberFormatException ignored) {
        // Use default
      }
    }

    return new SchemaMaterializationPolicy(strategy, ttl);
  }

  @Override
  public String toString() {
    return "SchemaMaterializationPolicy{" + "strategy=" + strategy + ", ttl=" + ttl + '}';
  }
}
