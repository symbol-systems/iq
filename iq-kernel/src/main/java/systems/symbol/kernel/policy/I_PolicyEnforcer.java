package systems.symbol.kernel.policy;

/**
 * Core policy decision point.
 * <p>
 * Protocol: Implementations are stateless, thread-safe and must not throw.
 * Errors are represented as deny results with reason.
 */
@FunctionalInterface
public interface I_PolicyEnforcer {

PolicyResult evaluate(PolicyInput input);

default String name() {
return getClass().getSimpleName();
}

default boolean isHealthy() {
return true;
}

default int order() {
return 0;
}
}
