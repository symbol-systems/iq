package systems.symbol.kernel.policy;

/**
 * Audit sink for policy decisions.
 */
public interface I_PolicyAudit {

void record(PolicyDecisionEvent event);

default boolean isAsync() {
return true;
}
}
