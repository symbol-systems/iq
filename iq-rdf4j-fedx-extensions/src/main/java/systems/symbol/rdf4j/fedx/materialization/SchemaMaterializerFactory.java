package systems.symbol.rdf4j.fedx.materialization;

import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory for creating schema materializers based on policy.
 *
 * <p>Routes policy to appropriate implementation:
 * - REALTIME => direct introspection, no caching (RealtimeSchemaMaterializer)
 * - CACHED_* => TTL-based cache with auto-expiry (CachedSchemaMaterializer)
 */
public class SchemaMaterializerFactory {

  private static final Logger LOG = LoggerFactory.getLogger(SchemaMaterializerFactory.class);

  private final List<I_SourceIntrospector> introspectors;

  /**
   * Construct with available introspectors.
   *
   * @param introspectors list of introspectors (specific first, then fallback)
   */
  public SchemaMaterializerFactory(List<I_SourceIntrospector> introspectors) {
    this.introspectors = Objects.requireNonNull(introspectors, "introspectors cannot be null");
    if (introspectors.isEmpty()) {
      throw new IllegalArgumentException("At least one introspector is required");
    }
  }

  /**
   * Create materializer for a policy.
   *
   * @param policy the schema materialization policy
   * @return materializer implementing the policy
   */
  public I_SchemaMaterializer create(SchemaMaterializationPolicy policy) {
    Objects.requireNonNull(policy, "policy cannot be null");

    return switch (policy.strategy()) {
      case REALTIME -> {
        LOG.info("Creating REALTIME schema materializer (no caching)");
        yield new RealtimeSchemaMaterializer(introspectors);
      }
      case CACHED_1H, CACHED_24H, CACHED_7D -> {
        LOG.info("Creating CACHED schema materializer: strategy={}, ttl={}", policy.strategy(),
            policy.ttl());
        yield new CachedSchemaMaterializer(introspectors, policy.ttl());
      }
    };
  }

  /**
   * Create materializer with default policy (CACHED_1H).
   *
   * @return cached schema materializer with 1-hour TTL
   */
  public I_SchemaMaterializer createDefault() {
    SchemaMaterializationPolicy defaultPolicy =
        new SchemaMaterializationPolicy(SchemaMaterializationPolicy.Strategy.CACHED_1H);
    return create(defaultPolicy);
  }
}
